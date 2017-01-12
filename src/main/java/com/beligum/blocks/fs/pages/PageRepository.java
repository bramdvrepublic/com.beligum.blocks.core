package com.beligum.blocks.fs.pages;

import com.beligum.base.models.Person;
import com.beligum.base.resources.DefaultResourceRequest;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ResourceRepositoryPrefix;
import com.beligum.base.resources.ifaces.*;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.LockFile;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.logger.PageLogEntry;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.sources.NewPageSource;
import com.beligum.blocks.rdf.sources.PageSource;
import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.fs.FileContext;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

import static com.beligum.blocks.fs.pages.PageRepository.PageDeleteOption.DELETE_ALL_TRANSLATIONS;

/**
 * Created by bram on 12/27/16.
 */
public class PageRepository implements ResourceRepository
{
    //-----CONSTANTS-----
    public enum PageDeleteOption implements ResourceRepository.DeleteOption
    {
        /**
         * Delete the page passed to the delete() method, plus all the attached translations
         */
        DELETE_ALL_TRANSLATIONS
    }

    public static final String PUBLIC_PATH_PREFIX = "/";

    //-----VARIABLES-----
    private Settings settings;
    private FileContext writeContext;

    //-----CONSTRUCTORS-----
    public PageRepository() throws IOException
    {
        this.settings = Settings.instance();
        this.writeContext = StorageFactory.getPageStoreFileSystem();
    }

    //-----PUBLIC METHODS-----
    @Override
    public ResourceRepositoryPrefix[] getPrefixes()
    {
        return new ResourceRepositoryPrefix[] { new ResourceRepositoryPrefix(this, URI.create(PUBLIC_PATH_PREFIX), MimeTypes.HTML) };
    }
    @Override
    public boolean isImmutable()
    {
        //Our HTML files are never static, it's the whole point of the blocks system
        return false;
    }
    @Override
    public boolean isReadOnly()
    {
        //both save() and delete() are implemented, so we fully support writing away HTML files
        return false;
    }
    @Override
    public ResourceRequest request(URI uri, MimeType forcedMimeType)
    {
        return new DefaultResourceRequest(this, uri, forcedMimeType);
    }
    @Override
    public Resource get(ResourceRequest resourceRequest)
    {
        Resource retVal = null;

        try {
            Page page = new ReadOnlyPage(resourceRequest);

            //Here, we decide on the normalized html path to see if the page exists or not
            // (note that we actually could check on the original and re-generate the normalized if necessary, but we decided to take the safe road)
            //Also note that out interface demands us to return null if the resourceRequest can't be resolved, so this check is necessary!
            if (page.getFileContext().util().exists(page.getNormalizedPageProxyPath())) {
                retVal = page;
            }
        }
        catch (IOException e) {
            Logger.error("Error while resolving html resource for " + resourceRequest.getUri(), e);
        }

        //DISABLED because this is a security risk!
        //        //if we found nothing, we just pass it on to the super classpath resolver
        //        //note: this is necessary because this resolver is used with a very short "/" prefix and will match a lot
        //        if (retVal == null) {
        //            retVal = super.resolve(resourceRequest);
        //        }

        return retVal;
    }
    //    @Override
    //    public PageIterator getAll(boolean readOnly, String relativeStartFolder, FullPathGlobFilter filter, int depth) throws IOException
    //    {
    //        URI rootPath = readOnly ? Settings.instance().getPagesViewPath() : Settings.instance().getPagesStorePath();
    //        URI startFolder = rootPath;
    //        if (!StringUtils.isEmpty(relativeStartFolder)) {
    //            //make sure it doesn't remove leading paths
    //            while (relativeStartFolder.startsWith("/")) {
    //                relativeStartFolder = relativeStartFolder.substring(1);
    //            }
    //
    //            startFolder = startFolder.resolve(relativeStartFolder);
    //        }
    //
    //        FileContext fileContext = readOnly ? StorageFactory.getPageViewFileSystem() : StorageFactory.getPageStoreFileSystem();
    //        return new PageIterator(fileContext, new Path(rootPath), new Path(startFolder), readOnly, filter, depth);
    //    }
    @Override
    public Resource save(Source source, Person editor, SaveOption... options) throws IOException, UnsupportedOperationException, IllegalArgumentException
    {
        Page retVal = null;

        //we need some specific functions, so make sure it's a PageSource, otherwise, convert it
        PageSource pageSource = null;
        if (source instanceof PageSource) {
            pageSource = (PageSource) source;
        }
        else {
            pageSource = new NewPageSource(source);
        }

        //this will look up the old page in read-only mode to compare with the new data
        //Note: when saving a new page, this will be null
        Page oldPage = R.resourceManager().get(pageSource.getUri(), Page.class);

        if (options.length > 0) {
            throw new IllegalArgumentException("Unsupported option passed; " + ArrayUtils.toString(options));
        }

        //create a new read-write page instance from the data (mainly the URI and MimeType) in the supplied resource
        ReadWritePage newPage = new ReadWritePage(this, pageSource);

        //will synchronize the metadata directory by creating/releasing a lock file
        try (LockFile lock = newPage.acquireLock()) {

            //pre-calculate the hash based on the incoming stream and compare it with the stored version to abort early if nothing changed
            String newHash = newPage.getHashChecksum();
            boolean nothingChanged = oldPage != null && newHash.equals(oldPage.getHashChecksum());

            if (nothingChanged) {
                retVal = newPage;
            }
            else {

                newPage.createParent();

                //we're overwriting; make an entry in the history folder
                if (oldPage != null) {
                    newPage.writeHistoryEntry(oldPage);
                }

                //save the original page html
                newPage.write(pageSource);

                //write out a log entry that the page was altered
                newPage.writeLogEntry(editor, oldPage != null ? PageLogEntry.Action.UPDATE : PageLogEntry.Action.CREATE);

                //save the page metadata (read it in if it exists)
                //Note: disabled and more or less replaced by the writeLogEntry() above because it was too error prone on crashes
                //newPage.writeMetadata(editor);

                //reindex the page
                this.index(newPage);

                //expire the cache
                this.expire(newPage);

                retVal = newPage;
            }
        }

        return retVal;
    }
    @Override
    public Resource delete(Resource resource, Person editor, DeleteOption... options) throws IOException, UnsupportedOperationException, IllegalArgumentException
    {
        Resource retVal = null;

        boolean deleteAllTranslations = false;
        if (options.length > 0) {
            for (DeleteOption option : options) {
                if (option == DELETE_ALL_TRANSLATIONS) {
                    deleteAllTranslations = true;
                }
                else {
                    throw new IllegalArgumentException("Unsupported option passed; " + option);
                }
            }
        }

        Page page = resource.unwrap(Page.class);
        if (page == null) {
            throw new IOException("Unable to delete this resource, it's not a valid Page; " + resource);
        }

        //we need to reuse the connection or we'll run into trouble when deleting multiple translations
        PageIndexConnection mainPageIndexer = StorageFactory.getMainPageIndexer().connect();
        PageIndexConnection triplestoreIndexer = StorageFactory.getTriplestoreIndexer().connect();

        //first, delete the translations, then delete the first one
        if (deleteAllTranslations) {
            Map<Locale, Page> translations = page.getTranslations();
            for (Map.Entry<Locale, Page> e : translations.entrySet()) {
                this.deleteSinglePage(e.getValue(), editor, mainPageIndexer, triplestoreIndexer);
            }
        }

        //delete this page
        retVal = this.deleteSinglePage(page, editor, mainPageIndexer, triplestoreIndexer);

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private Resource deleteSinglePage(Page roPage, Person editor, PageIndexConnection pageIndexer, PageIndexConnection triplestoreIndexer) throws IOException
    {
        Resource retVal = null;

        ReadWritePage rwPage = new ReadWritePage(this, roPage);

        //nothing to do if we don't exist
        if (rwPage.exists()) {

            try (LockFile lock = rwPage.acquireLock()) {

                //save the page metadata BEFORE we create the history entry (to make sure we save who deleted it)
                //Note: commented out to be in sync with save(), hope that's ok
                //page.writeMetadata(editor);

                //we're overwriting; make an entry in the history folder (because it won't be deleted; allows us to implement undo later on)
                rwPage.writeHistoryEntry(rwPage);

                //delete the necessary file structures
                rwPage.delete();

                //write out a log entry that the page was deleted
                rwPage.writeLogEntry(editor, PageLogEntry.Action.DELETE);

                //reindex the page
                this.unindex(rwPage, pageIndexer, triplestoreIndexer);

                //expire the cache
                this.expire(rwPage);

                retVal = rwPage;
            }
        }

        return retVal;
    }
    private void index(Page page) throws IOException
    {
        this.index(page, StorageFactory.getMainPageIndexer().connect(), StorageFactory.getTriplestoreIndexer().connect());
    }
    private void index(Page page, PageIndexConnection pageIndexer, PageIndexConnection triplestoreIndexer) throws IOException
    {
        //Note: transaction handling is done through the global XA transaction
        pageIndexer.update(page);
        triplestoreIndexer.update(page);
    }
    private void unindex(Page page) throws IOException
    {
        this.unindex(page, StorageFactory.getMainPageIndexer().connect(), StorageFactory.getTriplestoreIndexer().connect());
    }
    private void unindex(Page page, PageIndexConnection pageIndexer, PageIndexConnection triplestoreIndexer) throws IOException
    {
        //Note: transaction handling is done through the global XA transaction
        pageIndexer.delete(page);
        triplestoreIndexer.delete(page);
    }
    private void expire(Page page)
    {
        //make sure we evict all possible cached values (mainly in production)
        R.resourceManager().expire(page.getPublicAbsoluteAddress());
        R.resourceManager().expire(page.getPublicRelativeAddress());
    }
}

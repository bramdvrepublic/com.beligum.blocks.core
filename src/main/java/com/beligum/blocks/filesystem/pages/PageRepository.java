/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.filesystem.pages;

import com.beligum.base.models.Person;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ResourceRepositoryPrefix;
import com.beligum.base.resources.ifaces.*;
import com.beligum.base.resources.repositories.AbstractResourceRepository;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.LockFile;
import com.beligum.blocks.filesystem.ifaces.ResourceMetadata;
import com.beligum.blocks.index.ifaces.IndexConnection;
import com.beligum.blocks.filesystem.log.PageLogEntry;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.utils.SecurityTools;
import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

import static com.beligum.blocks.filesystem.pages.PageRepository.PageDeleteOption.DELETE_ALL_TRANSLATIONS;

/**
 * Created by bram on 12/27/16.
 */
public class PageRepository extends AbstractResourceRepository
{
    //-----CONSTANTS-----
    public enum PageDeleteOption implements ResourceRepository.DeleteOption
    {
        /**
         * Delete the page passed to the delete() method, plus all the attached translations
         */
        DELETE_ALL_TRANSLATIONS
    }

    public static class IndexConnectionOption implements ResourceRepository.IndexOption
    {
        private IndexConnection mainPageConnection;
        private IndexConnection triplestoreConnection;

        public IndexConnectionOption(IndexConnection mainPageConnection, IndexConnection triplestoreConnection)
        {
            this.mainPageConnection = mainPageConnection;
            this.triplestoreConnection = triplestoreConnection;
        }

        public IndexConnection getMainPageConnection()
        {
            return mainPageConnection;
        }
        public IndexConnection getTriplestoreConnection()
        {
            return triplestoreConnection;
        }
    }

    public static final String PUBLIC_PATH_PREFIX = "/";

    //-----VARIABLES------

    //-----CONSTRUCTORS-----
    public PageRepository() throws IOException
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public ResourceRepositoryPrefix[] getPrefixes()
    {
        return new ResourceRepositoryPrefix[] { new ResourceRepositoryPrefix(this, URI.create(PUBLIC_PATH_PREFIX), MimeTypes.HTML) };
    }
    @Override
    public ResourceRequest request(URI uri, MimeType forcedMimeType)
    {
        //all pages are forced to HTML
        //note that this is necessary and tricky:
        // if a page is requested with another page in a <a> tag,
        // it will get looked up during fingerprinting, but at that
        // time, we can't force the mime type of that link,
        // so it will return a null request object in the ResourceManager.
        // In production mode, this null value gets cached and the page
        // is blocked from future (direct) requests, returning a 404
        return super.request(uri, MimeTypes.HTML);
    }
    @Override
    public Resource get(ResourceRequest resourceRequest)
    {
        Resource retVal = null;

        try {
            Page page = new ReadOnlyPage(resourceRequest);

            //Here, we decide on the normalized html path to see if the page exists or not
            // (note that we actually could check on the original and re-generate the normalized if necessary, but we decided to take the safe road)
            //Also note that our interface demands us to return null if the resourceRequest can't be resolved, so this check is necessary!
            if (page.getFileContext().util().exists(page.getNormalizedHtmlFile())) {
                retVal = page;
            }
        }
        catch (Exception e) {
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
    @Override
    public ResourceIterator getAll(boolean readOnly, URI startFolder, ResourceFilter filter, Integer maxDepth) throws IOException, UnsupportedOperationException
    {
        //this should be synced with the constructors of ReadOnlyPage and ReadWritePage
        URI rootPath = readOnly ? Settings.instance().getPagesViewUri() : Settings.instance().getPagesStoreUri();

        URI startPath = rootPath;
        if (startFolder != null) {
            startPath = startPath.resolve(startFolder);
        }

        FileContext fileContext = readOnly ? StorageFactory.getPageViewFileSystem() : StorageFactory.getPageStoreFileSystem();

        return new PageIterator(this, fileContext, new Path(rootPath), new Path(startPath), readOnly, filter == null ? null : new FullPathGlobFilter(filter.getPathPattern()), maxDepth);
    }
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
        Page oldPage = R.resourceManager().get(pageSource.getUri(), MimeTypes.HTML, Page.class);

        if (options != null && options.length > 0) {
            throw new IllegalArgumentException("Unsupported option passed; " + ArrayUtils.toString(options));
        }

        //instance a new read-write page instance from the data (mainly the URI and MimeType) in the supplied resource
        ReadWritePage newPage = new ReadWritePage(this, pageSource);

        //will synchronize the metadata directory by creating/releasing a lock file
        //Note that we need to do this before the hash, because a newly calculated hash will write to the dotfolder
        try (LockFile lock = newPage.acquireLock()) {

            //pre-calculate the hash based on the incoming stream and compare it with the stored version to abort early if nothing changed
            boolean nothingChanged = oldPage != null && pageSource.getHash().equals(oldPage.getHash());

            if (nothingChanged) {
                retVal = newPage;
            }
            else {
                newPage.createParent();

                //we're overwriting; make an entry in the history folder
                if (oldPage != null) {
                    newPage.writeHistoryEntry(oldPage);
                }

                //update and/or initialize the metadata of the incoming source before we write it to disk
                //Note: by doing it here, the nothingChanged flag still works as expected
                //Note that this needs to be called before the security check below because it initializes the ACls if needed
                pageSource.updateMetadata(editor);

                //note that this must be called on the source (not the newPage)
                //because the html hasn't been injected in the newPage yet (this happens below in newPage.write(pageSource))
                ResourceMetadata newMetadata = pageSource.getMetadata();

                //if we have an old page, make sure to check the permissions on that version, not the new version
                if (oldPage != null) {
                    ResourceMetadata oldMetadata = oldPage.getMetadata();

                    //first, check if we had save rights on the old page, because we're about to overwrite it
                    if (newMetadata.getUpdateAcl() != null) {
                        SecurityTools.checkPermission(R.securityManager().getCurrentRole(), newMetadata.getUpdateAcl());
                    }

                    //check if the ACLs have changed; if they did, we need to check that we have the right permission
                    if (!newMetadata.hasSameAcls(oldMetadata)) {
                        if (oldMetadata.getManageAcl() != null) {
                            SecurityTools.checkPermission(R.securityManager().getCurrentRole(), oldMetadata.getManageAcl());
                        }
                        else {
                            // What do we do here? The ACL changed, but there was no specific manager ACL set on the old page.
                            // I guess we can leave it alone and assume the more general security rules should do their work, right?
                        }
                    }
                }
                //If this is the first time this page is saved, we check that we have save and manage permission,
                //otherwise, we're about to create a page that can't be changed or managed by the current user anymore
                //Note that this is not strictly necessary (the acls should be initialized correctly),
                //but if this doesn't pass, the ACL system is probably misconfigured.
                else {
                    if (newMetadata.getUpdateAcl() != null) {
                        SecurityTools.checkPermission(R.securityManager().getCurrentRole(), newMetadata.getUpdateAcl());
                    }
                    if (newMetadata.getManageAcl() != null) {
                        SecurityTools.checkPermission(R.securityManager().getCurrentRole(), newMetadata.getManageAcl());
                    }
                    else {
                        // What do we do here? The ACL changed, but there was no specific manager ACL set on the new page.
                        // I guess we can leave it alone and assume the more general security rules should do their work, right?
                    }
                }

                // save the original page html
                newPage.write(pageSource);

                // generated and write the normalized proxy html
                newPage.updateNormalizedProxy(pageSource);

                // extract the RDF and save it
                // Note that the indexer below will re-read this, so make sure this happens before indexing
                newPage.updateRdfProxy(pageSource);

                // if all went well, we can update the hash file
                newPage.writeHash(source.getHash());

                // write out a log entry that the page was altered
                newPage.writeLogEntry(editor, oldPage != null ? PageLogEntry.Action.UPDATE : PageLogEntry.Action.CREATE);

                // reindex the page
                this.index(newPage);

                // expire the cache
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
        IndexConnection mainPageIndexer = StorageFactory.getJsonIndexer().connect(StorageFactory.getCurrentScopeTx());
        IndexConnection triplestoreIndexer = StorageFactory.getSparqlIndexer().connect(StorageFactory.getCurrentScopeTx());

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
    @Override
    public Resource reindex(Resource resource, IndexOption... options) throws IOException, UnsupportedOperationException, IllegalArgumentException
    {
        Resource retVal = null;

        //this allows us to pass a long-running (asynchronous, shared) transaction to boost performance
        IndexConnection mainPageConnection = null;
        IndexConnection triplestoreConnection = null;
        if (options.length > 0) {
            for (IndexOption option : options) {
                //for now, this is the only option we support here
                if (option instanceof IndexConnectionOption) {
                    IndexConnectionOption o = (IndexConnectionOption) option;
                    mainPageConnection = o.mainPageConnection;
                    triplestoreConnection = o.triplestoreConnection;
                }
                else {
                    throw new IllegalArgumentException("Unsupported option passed; " + option);
                }
            }
        }

        //fallback to regular request-scoped transactions if nothing special was passed
        if (mainPageConnection == null) {
            mainPageConnection = StorageFactory.getJsonIndexer().connect(StorageFactory.getCurrentScopeTx());
        }
        if (triplestoreConnection == null) {
            triplestoreConnection = StorageFactory.getSparqlIndexer().connect(StorageFactory.getCurrentScopeTx());
        }

        Page page = resource.unwrap(Page.class);
        if (page == null) {
            throw new IOException("Unable to reindex this resource, it's not a valid Page; " + resource);
        }

        //before we reindex the page, we'll do a few little tests to fix possible errors:
        // - check if the original is there (can't continue without)
        // - check if the normalized version is present and re-generate it if it's missing
        // - check if the RDF model is present and re-generate it if it's missing
        boolean originalMissing = !page.getFileContext().util().exists(page.getLocalStoragePath());
        if (originalMissing) {
            throw new IOException("Original HTML file for this page is missing, can't reindex; " + page.getPublicAbsoluteAddress());
        }

        boolean normalizedMissing = !page.getFileContext().util().exists(page.getNormalizedHtmlFile());
        boolean rdfMissing = !page.getFileContext().util().exists(page.getRdfExportFile());
        boolean rdfDepsMissing = !page.getFileContext().util().exists(page.getRdfDependenciesExportFile());

        //For debugging only!!
        //        normalizedMissing = true;
        //        rdfMissing = true;
        //        rdfDepsMissing = false;

        //Note that we only need to have a write context if one of the above files is missing
        if (normalizedMissing || rdfMissing || rdfDepsMissing) {
            //create a write context
            ReadWritePage rwPage = new ReadWritePage(this, page);
            try (LockFile lock = rwPage.acquireLock()) {

                //Note: we can't use the NewPageSource(page) constructor because it reads the normalized html, not the raw original
                NewPageSource pageSource = null;
                try (InputStream originalHtml = page.getFileContext().open(page.getLocalStoragePath())) {
                    pageSource = new NewPageSource(page.getUri(), originalHtml);
                }

                if (normalizedMissing) {
                    Logger.info("Regenerating missing normalized html proxy file for " + page);
                    rwPage.updateNormalizedProxy(pageSource);
                }

                if (rdfMissing || rdfDepsMissing) {
                    //                    if (rdfMissing) {
                    //                        Logger.info("Regenerating missing RDF proxy file for " + page);
                    //                    }
                    //                    if (rdfDepsMissing) {
                    //                        Logger.info("Regenerating missing RDF dependencies proxy file for " + page);
                    //                    }
                    rwPage.updateRdfProxy(pageSource);
                }

                //it makes sense to expire the cache because eg. the Page.exists() is based on the availability
                // of the normalized file and this might have changed.
                this.expire(rwPage);

                //signal the caller we messed around in the FS
                retVal = rwPage;
            }
        }

        //By returning a RO-page or a RW-page, we can more or less signal the caller what changed (only the index or the index + fixes to the FS)
        //Note: this is also necessary to let all the following reads be part of the transaction,
        //so they pick up the possible changes (by using the same file context as the rwPage)
        if (retVal == null) {
            retVal = page;
        }

        //This is actually what he're here for: update both indexes (Note: order is important!)
        mainPageConnection.update(retVal);
        triplestoreConnection.update(retVal);

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private Resource deleteSinglePage(Page roPage, Person editor, IndexConnection pageIndexer, IndexConnection triplestoreIndexer) throws IOException
    {
        Resource retVal = null;

        ReadWritePage rwPage = new ReadWritePage(this, roPage);

        //nothing to do if we don't exist
        if (rwPage.exists()) {

            try (LockFile lock = rwPage.acquireLock()) {

                //save the page metadata BEFORE we instance the history entry (to make sure we save who deleted it)
                //Note: commented out to be in sync with save(), hope that's ok
                //page.writeMetadata(editor);

                //we're overwriting; make an entry in the history folder (because it won't be deleted; allows us to implement undo later on)
                rwPage.writeHistoryEntry(rwPage);

                //reindex the page (not that we need to call this before the filesystem delete() below because it might want to read in some file structures during unindex)
                this.unindex(rwPage, pageIndexer, triplestoreIndexer);

                //delete the necessary file structures
                rwPage.delete();

                //write out a log entry that the page was deleted
                rwPage.writeLogEntry(editor, PageLogEntry.Action.DELETE);

                //expire the cache
                this.expire(rwPage);

                retVal = rwPage;
            }
        }

        return retVal;
    }
    private void index(Page page) throws IOException
    {
        this.index(page, StorageFactory.getJsonIndexer().connect(StorageFactory.getCurrentScopeTx()), StorageFactory.getSparqlIndexer().connect(StorageFactory.getCurrentScopeTx()));
    }
    private void index(Page page, IndexConnection pageIndexer, IndexConnection triplestoreIndexer) throws IOException
    {
        //Note: transaction handling is done through the global XA transaction
        //Note: order is important!
        pageIndexer.update(page);
        triplestoreIndexer.update(page);
    }
    private void unindex(Page page) throws IOException
    {
        this.unindex(page, StorageFactory.getJsonIndexer().connect(StorageFactory.getCurrentScopeTx()), StorageFactory.getSparqlIndexer().connect(StorageFactory.getCurrentScopeTx()));
    }
    private void unindex(Page page, IndexConnection pageIndexer, IndexConnection triplestoreIndexer) throws IOException
    {
        //Note: transaction handling is done through the global XA transaction
        //Note: we must delete the triplestore before the page index because the page index is used to calculate translations and subresources!
        triplestoreIndexer.delete(page);
        pageIndexer.delete(page);
    }
    private void expire(Page page)
    {
        //make sure we evict all possible cached values (mainly in production)
        R.resourceManager().expire(page.getPublicAbsoluteAddress());
        R.resourceManager().expire(page.getPublicRelativeAddress());
    }
}

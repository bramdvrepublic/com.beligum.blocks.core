package com.beligum.blocks.filesystem.pages;

import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.ResourceIterator;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.filesystem.hdfs.HdfsUtils;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import org.apache.hadoop.fs.*;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 8/28/16.
 */
public class PageIterator implements ResourceIterator
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private ResourceRepository pageRepository;
    private FileContext fileContext;
    private Path rootFolder;
    private Path startFolder;
    private boolean readOnly;
    private RemoteIterator<LocatedFileStatus> fileIter;
    private Page precomputedNext;
    private PathFilter filter;
    private boolean keepRunning;

    //-----CONSTRUCTORS-----
    public PageIterator(ResourceRepository pageRepository, FileContext fileContext, Path rootFolder, Path startFolder, boolean readOnly, PathFilter filter, Integer maxDepth) throws IOException
    {
        this.pageRepository = pageRepository;
        this.fileContext = fileContext;
        this.rootFolder = rootFolder;
        this.startFolder = startFolder;
        this.readOnly = readOnly;
        this.filter = filter;

        this.fileIter = new HdfsFileIterator(this.fileContext, this.rootFolder, this.startFolder, true, maxDepth);
        this.precomputedNext = null;
        this.keepRunning = true;
    }

    //-----PUBLIC METHODS-----
    @Override
    public void cancel()
    {
        //TODO we should sync this with the loop below
        this.keepRunning = false;
    }
    @Override
    public boolean hasNext()
    {
        boolean retVal = false;

        try {
            Page next = this.doNext();
            if (next != null) {
                this.precomputedNext = next;
                retVal = true;
            }
        }
        catch (IOException e) {
            com.beligum.base.utils.Logger.error("Exception caught while checking for next child, returning false; " + this.startFolder, e);
        }

        return retVal;
    }
    @Override
    public Page next()
    {
        Page retVal = null;

        try {
            if (this.precomputedNext != null) {
                retVal = this.precomputedNext;
                this.precomputedNext = null;
            }
            else {
                retVal = this.doNext();
            }
        }
        catch (IOException e) {
            Logger.error("Error while building the page from the URI returned by the HDFS iterator (returning null);", e);
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private Page doNext() throws IOException
    {
        Page retVal = null;

        while (retVal == null && this.fileIter.hasNext() && this.keepRunning) {
            LocatedFileStatus next = this.fileIter.next();
            Path absolutePath = next.getPath();

            //will throw an exception otherwise..
            if (absolutePath.getName().endsWith(Settings.instance().getPagesFileExtension())) {

                //we build a relative path from the root storage path
                //Note that this is strictly not necessary because all of our filesystems are chrooted ones,
                //but let's leave it here for possible future changes.
                Path relativePath = new Path(HdfsUtils.ROOT.resolve(this.rootFolder.toUri().relativize(absolutePath.toUri())));

                //strip the storage file extension and check if we're dealing with a folder
                String filename = relativePath.getName();
                String urlFilename = filename.substring(0, filename.length() - Settings.instance().getPagesFileExtension().length());
                if (urlFilename.equals(AbstractPage.DIR_PAGE_NAME)) {
                    urlFilename = null;
                }

                //we strip off the name and re-build it if necessary
                URI canonicalTemp = relativePath.toUri().resolve(".");
                //means we're dealing with a file-url, not a folder-url
                if (urlFilename != null) {
                    canonicalTemp = canonicalTemp.resolve(urlFilename);
                }

                // here, we have a fairly uniform path to filter on:
                //  - prefixed with the language (like on disk)
                //  - removed directory filename
                // but note that it still has a schema prefix (the HDFS impl), so we only use the path
                if (this.filter == null || this.filter.accept(new Path(canonicalTemp.getPath()))) {

                    //Note: a null-valued language means: detect it, which works because the structure on disk (with leading language folder)
                    // is also supported by the public urls
                    if (this.readOnly) {
                        retVal = new ReadOnlyPage(this.pageRepository, canonicalTemp, null, MimeTypes.HTML, false, this.fileContext);
                    }
                    else {
                        retVal = new ReadWritePage(this.pageRepository, canonicalTemp, null, MimeTypes.HTML, false, this.fileContext);
                    }
                }
                else {
                    //signal the next if to skip this cause the filter is set, but doesn't match
                    //Logger.warn("Page iterator: skipping " + relativePath + " because it doesn't match the path filter");
                }
            }
        }

        return retVal;
    }
}

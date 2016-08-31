package com.beligum.blocks.fs.pages;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.fs.HdfsUtils;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.hadoop.fs.*;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by bram on 8/28/16.
 */
public class WalkPagesIterator implements Iterator<Page>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private FileContext fileContext;
    private Path rootFolder;
    private Path startFolder;
    private boolean readOnly;
    private RemoteIterator<LocatedFileStatus> fileIter;
    private Page precomputedNext;
    private PathFilter filter;

    //-----CONSTRUCTORS-----
    public WalkPagesIterator(FileContext fileContext, Path rootFolderAbs, Path startFolderAbs, boolean readOnly, PathFilter filter) throws IOException
    {
        this.fileContext = fileContext;
        this.rootFolder = rootFolderAbs;
        this.startFolder = startFolderAbs;
        this.readOnly = readOnly;
        this.filter = filter;

        this.fileIter = this.fileContext.util().listFiles(this.startFolder, true);
        this.precomputedNext = null;
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean hasNext()
    {
        boolean retVal = false;

        try {
            Page next = this.doNext();
            if (next!=null) {
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
            if (this.precomputedNext!=null) {
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

        while (retVal==null && this.fileIter.hasNext()) {
            LocatedFileStatus next = this.fileIter.next();
            Path nextPath = next.getPath();

            //note: meta files can never resolve to pages
            if (!HdfsUtils.isMetaPath(nextPath)) {
                //we build a relative path from the absolute path to be able to mimic a page request
                Path nextPathRelative = new Path(HdfsUtils.ROOT.resolve(this.rootFolder.toUri().relativize(nextPath.toUri())));

                if (this.filter!=null && !this.filter.accept(nextPathRelative)) {
                    //signal the next if to skip this cause the filter is set, but doesn't match
                    nextPathRelative = null;
                }

                if (nextPathRelative!=null) {
                    retVal = this.readOnly ? new ReadOnlyPage(nextPathRelative) : new ReadWritePage(nextPathRelative);
                }
            }
        }

        return retVal;
    }
}

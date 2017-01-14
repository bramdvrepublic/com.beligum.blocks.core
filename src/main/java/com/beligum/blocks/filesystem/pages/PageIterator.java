package com.beligum.blocks.filesystem.pages;

import com.beligum.blocks.filesystem.pages.ifaces.Page;

import java.util.Iterator;

/**
 * Created by bram on 8/28/16.
 */
public class PageIterator implements Iterator<Page>
{
    @Override
    public boolean hasNext()
    {
        return false;
    }
    @Override
    public Page next()
    {
        return null;
    }
    public void cancel()
    {
    }

    //    //-----CONSTANTS-----
//
//    //-----VARIABLES-----
//    private FileContext fileContext;
//    private Path rootFolder;
//    private Path startFolder;
//    private boolean readOnly;
//    private RemoteIterator<LocatedFileStatus> fileIter;
//    private Page precomputedNext;
//    private FullPathGlobFilter filter;
//    private boolean keepRunning;
//
//    //-----CONSTRUCTORS-----
//    public PageIterator(FileContext fileContext, Path rootFolderAbs, Path startFolderAbs, boolean readOnly, FullPathGlobFilter filter, int maxDepth) throws IOException
//    {
//        this.fileContext = fileContext;
//        this.rootFolder = rootFolderAbs;
//        this.startFolder = startFolderAbs;
//        this.readOnly = readOnly;
//        this.filter = filter;
//
//        //this.fileContext.util().listFiles(this.startFolder, true);
//        this.fileIter = new HdfsFileIterator(this.fileContext, this.rootFolder, this.startFolder, true, maxDepth);
//        this.precomputedNext = null;
//        this.keepRunning = true;
//    }
//
//    //-----PUBLIC METHODS-----
//    public void cancel()
//    {
//        this.keepRunning = false;
//    }
//    @Override
//    public boolean hasNext()
//    {
//        boolean retVal = false;
//
//        try {
//            Page next = this.doNext();
//            if (next != null) {
//                this.precomputedNext = next;
//                retVal = true;
//            }
//        }
//        catch (IOException e) {
//            com.beligum.base.utils.Logger.error("Exception caught while checking for next child, returning false; " + this.startFolder, e);
//        }
//
//        return retVal;
//    }
//    @Override
//    public Page next()
//    {
//        Page retVal = null;
//
//        try {
//            if (this.precomputedNext != null) {
//                retVal = this.precomputedNext;
//                this.precomputedNext = null;
//            }
//            else {
//                retVal = this.doNext();
//            }
//        }
//        catch (IOException e) {
//            Logger.error("Error while building the page from the URI returned by the HDFS iterator (returning null);", e);
//        }
//
//        return retVal;
//    }
//
//    //-----PROTECTED METHODS-----
//
//    //-----PRIVATE METHODS-----
//    private Page doNext() throws IOException
//    {
//        Page retVal = null;
//
//        while (retVal == null && this.fileIter.hasNext() && this.keepRunning) {
//            LocatedFileStatus next = this.fileIter.next();
//            Path nextPath = next.getPath();
//
//            //we build a relative path from the absolute path to be able to mimic a page request
//            Path nextPathRelative = new Path(HdfsUtils.ROOT.resolve(this.rootFolder.toUri().relativize(nextPath.toUri())));
//
//            if (this.filter != null && !this.filter.accept(nextPathRelative)) {
//                //signal the next if to skip this cause the filter is set, but doesn't match
//                Logger.warn("Page iterator: skipping " + nextPathRelative + " because it doesn't match the filter (" + this.filter.getPattern() + ")");
//                nextPathRelative = null;
//            }
//
//            if (nextPathRelative != null) {
//                //will throw an exception otherwise..
//                if (nextPathRelative.getName().endsWith(Settings.instance().getPagesFileExtension())) {
//                    retVal = this.readOnly ? new ReadOnlyPage(nextPathRelative) : new ReadWritePage(nextPathRelative);
//                }
//            }
//        }
//
//        return retVal;
//    }
}

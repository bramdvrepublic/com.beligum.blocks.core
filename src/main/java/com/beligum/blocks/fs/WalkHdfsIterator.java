package com.beligum.blocks.fs;

import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

/**
 * Created by bram on 8/28/16.
 */
public class WalkHdfsIterator implements Iterator<Path>
{
    //-----CONSTANTS-----
    private static final URI ROOT = URI.create("/");

    //-----VARIABLES-----
    private FileContext fs;
    private Path root;
    private final URI fsRoot;
    private RemoteIterator<FileStatus> currentStatuses;
    private WalkHdfsIterator currentIter;
    private WalkHdfsIterator previousIter;

    //-----CONSTRUCTORS-----
    public WalkHdfsIterator(FileContext fs, Path root) throws IOException
    {
        this.fs = fs;
        this.root = root;
        this.fsRoot = fs.resolvePath(new Path("/")).toUri();
        this.currentStatuses = fs.listStatus(this.root);
        this.currentIter = this;
        this.previousIter = this;
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean hasNext()
    {
        boolean retVal = false;

        try {
            retVal = this.currentIter.currentStatuses.hasNext();

            //if there's nothing left, but we're in the 'parent' iterator, revert a level down
            if (!retVal && this.currentIter!=this) {
                retVal = this.currentStatuses.hasNext();
            }
        }
        catch (IOException e) {
            com.beligum.base.utils.Logger.error("Exception caught while checking for next child, returning false; "+this.root, e);
        }

        return retVal;
    }
    @Override
    public Path next()
    {
        Path retVal = null;

        try {
            //same code as in hasNext() but with a change of the current iterator
            boolean hasNext = this.currentIter.currentStatuses.hasNext();
            if (!hasNext && this.currentIter!=this) {
                hasNext = this.currentStatuses.hasNext();
                //'drop' the iterator
                this.currentIter = this.currentIter.previousIter;
            }

            if (hasNext) {
                FileStatus childStatus = this.currentIter.currentStatuses.next();
                //this relativation is needed to be able to work with chrooted filesystems, because fs.listStatus() returns absolute file paths
                retVal = new Path(ROOT.resolve(this.currentIter.fsRoot.relativize(childStatus.getPath().toUri())));
                if (childStatus.isDirectory()) {
                    //this will 'enter' the directory
                    this.previousIter = this.currentIter;
                    this.currentIter = new WalkHdfsIterator(this.currentIter.fs, retVal);
                }
            }
        }
        catch (Exception e) {
            com.beligum.base.utils.Logger.error("Exception caught while building next child, returning null; "+this.currentIter.root, e);
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

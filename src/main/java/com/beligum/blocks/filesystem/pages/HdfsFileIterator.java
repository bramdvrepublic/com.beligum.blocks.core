package com.beligum.blocks.filesystem.pages;

import com.beligum.blocks.filesystem.hdfs.HdfsUtils;
import org.apache.hadoop.fs.*;

import java.io.IOException;
import java.util.Stack;

/**
 * Started out with copying parts from org.apache.hadoop.fs.FileContext.Util.listFiles()
 * and adapted to our needs.
 *
 * Note: this iterator only returns files, not folders or symlinks (symlinks are followed as an extra depth layer though)
 *
 * Created by bram on 9/1/16.
 */
public class HdfsFileIterator implements RemoteIterator<LocatedFileStatus>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private FileContext fileContext;
    private Path rootFolder;
    private Path startFolder;
    private boolean recursive;
    private int maxDepth;
    private Stack<RemoteIterator<LocatedFileStatus>> iterators;
    private RemoteIterator<LocatedFileStatus> currentIter;
    private LocatedFileStatus currentFile;

    //-----CONSTRUCTORS-----
    public HdfsFileIterator(FileContext fileContext, final Path absRootFolder, final Path absStartFolder, final boolean recursive, int maxDepth) throws IOException
    {
        this.fileContext = fileContext;
        this.rootFolder = absRootFolder;
        this.startFolder = absStartFolder;
        this.recursive = recursive;
        this.maxDepth = maxDepth;

        this.iterators = new Stack<>();
        this.currentIter = this.fileContext.listLocatedStatus(absStartFolder);
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean hasNext() throws IOException
    {
        while (currentFile == null) {
            //means we have a next file in the current directory
            if (currentIter.hasNext()) {
                handleFileStat(currentIter.next());
            }
            //pop back to where we left off at the parent folder
            else if (!iterators.empty()) {
                currentIter = iterators.pop();
            }
            else {
                return false;
            }
        }

        return true;
    }
    @Override
    public LocatedFileStatus next() throws IOException
    {
        if (hasNext()) {
            LocatedFileStatus result = currentFile;
            currentFile = null;
            return result;
        }

        throw new java.util.NoSuchElementException("No more entry in " + startFolder);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Process the input stat.
     * If it is a file, return the file stat.
     * If it is a directory, traverse the directory if recursive is true;
     * ignore it if recursive is false.
     * If it is a symlink, resolve the symlink first and then process it
     * depending on if it is a file or directory.
     */
    private void handleFileStat(LocatedFileStatus stat) throws IOException
    {
        //file
        if (stat.isFile()) {
            if (this.test(stat)) {
                currentFile = stat;
            }
        }
        //symlink
        else if (stat.isSymlink()) {
            // resolve symbolic link
            FileStatus symstat = this.fileContext.getFileStatus(stat.getSymlink());
            //we go 'up' on a (good) symlink as well
            if (symstat.isFile() || (recursive && symstat.isDirectory())) {
                if (this.test(stat)) {
                    iterators.push(currentIter);
                    currentIter = this.fileContext.listLocatedStatus(stat.getPath());
                }
            }
        }
        //folder
        else if (recursive) {
            if (this.test(stat)) {
                iterators.push(currentIter);
                currentIter = this.fileContext.listLocatedStatus(stat.getPath());
            }
        }
        else {
            //NOOP, just leave the currentFile null
        }
    }
    private boolean test(LocatedFileStatus stat)
    {
        boolean retVal = false;

        if (stat.isDirectory() && this.maxDepth>=0 && this.iterators.size()>=this.maxDepth) {
            retVal = false;
        }
        else {
            //don't enter meta (and hidden in general) folders
            if (!HdfsUtils.isMetaPath(stat.getPath())) {
                retVal = true;
            }
        }

        return retVal;
    }
}

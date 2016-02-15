package com.beligum.blocks.fs.hdfs;

import org.apache.hadoop.fs.FileStatus;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of BasicFileAttributes.
 * <p/>
 * Created by bram on 2/14/16.
 */
public class HadoopBasicFileAttributes implements BasicFileAttributes
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final FileStatus fileStatus;

    //-----CONSTRUCTORS-----
    public HadoopBasicFileAttributes(FileStatus fileStatus)
    {
        this.fileStatus = fileStatus;
    }

    //-----PUBLIC METHODS-----
    @Override
    public FileTime creationTime()
    {
        return FileTime.from(this.fileStatus.getModificationTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Object fileKey()
    {
        //??? don't know what to return here
        return null;
    }

    @Override
    public boolean isDirectory()
    {
        return this.fileStatus.isDirectory();
    }

    @Override
    public boolean isOther()
    {
        return false;
    }

    @Override
    public boolean isRegularFile()
    {
        return this.fileStatus.isFile();
    }

    @Override
    public boolean isSymbolicLink()
    {
        return this.fileStatus.isSymlink();
    }

    @Override
    public FileTime lastAccessTime()
    {
        return FileTime.from(this.fileStatus.getAccessTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public FileTime lastModifiedTime()
    {
        return FileTime.from(this.fileStatus.getModificationTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public long size()
    {
        return this.fileStatus.getLen();
    }

    @Override
    public String toString()
    {
        return "[IS DIR : " + this.fileStatus.isDirectory() + "]";
    }

    //-----PROTECTED METHODS-----
    protected FileStatus getFileStatus()
    {
        return fileStatus;
    }

    //-----PRIVATE METHODS-----

}

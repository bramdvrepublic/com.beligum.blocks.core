package com.beligum.blocks.fs;

import com.beligum.base.resources.ResourceRequest;
import com.beligum.base.resources.mappers.AbstractResource;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;

/**
 * A simple resource that wraps a string content
 *
 * Created by bram on 2/7/16.
 */
public class HdfsResource extends AbstractResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private FileContext fileContext;
    private Path hdfsPath;

    //-----CONSTRUCTORS-----
    public HdfsResource(ResourceRequest request, FileContext fileContext, Path hdfsPath) throws IOException
    {
        super(request);

        this.fileContext = fileContext;
        this.hdfsPath = hdfsPath;
    }

    //-----PUBLIC METHODS-----
    @Override
    public InputStream newInputStream() throws IOException
    {
        return this.fileContext.open(this.hdfsPath);
    }
    @Override
    public long getLastModifiedTime() throws IOException
    {
        return Math.max((this.fileContext == null ? 0 : this.fileContext.getFileStatus(this.hdfsPath).getModificationTime()), this.getChildrenLastModificationTime());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

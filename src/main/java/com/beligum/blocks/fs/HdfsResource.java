package com.beligum.blocks.fs;

import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.base.resources.mappers.AbstractResource;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;

/**
 * A simple resource that wraps a HDFS path
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
    public boolean exists() throws IOException
    {
        return this.fileContext.util().exists(this.hdfsPath);
    }
    @Override
    public long getLastModifiedTime() throws IOException
    {
        return Math.max((this.fileContext == null ? this.getZeroLastModificationTime() : this.fileContext.getFileStatus(this.hdfsPath).getModificationTime()), this.calcChildrenLastModificationTime());
    }
    @Override
    public long getSize() throws IOException
    {
        return this.fileContext.getFileStatus(this.hdfsPath).getLen();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

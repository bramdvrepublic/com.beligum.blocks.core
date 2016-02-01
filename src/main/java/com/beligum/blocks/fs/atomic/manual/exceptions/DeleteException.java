package com.beligum.blocks.fs.atomic.manual.exceptions;

import org.apache.hadoop.fs.Path;

import java.io.IOException;

public class DeleteException extends IOException
{
    public DeleteException()
    {
    }
    public DeleteException(Path f)
    {
        super(f.toString());
    }
    public DeleteException(Path f, Exception e)
    {
        super(f.toString(), e);
    }
}

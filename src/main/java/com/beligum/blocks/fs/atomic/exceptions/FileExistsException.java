package com.beligum.blocks.fs.atomic.exceptions;

import org.apache.hadoop.fs.Path;

public class FileExistsException extends java.io.IOException
{
    public FileExistsException()
    {
    }
    public FileExistsException(String message)
    {
        super(message);
    }
    public FileExistsException(Path f)
    {
        super(f.toString());
    }
    public FileExistsException(Path f, Exception e)
    {
        super(f.toString(), e);
    }
}

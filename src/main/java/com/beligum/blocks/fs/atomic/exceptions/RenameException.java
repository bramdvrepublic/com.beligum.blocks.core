package com.beligum.blocks.fs.atomic.exceptions;

import org.apache.hadoop.fs.Path;

import java.io.IOException;

public class RenameException extends IOException
{
    public RenameException()
    {
    }
    public RenameException(Path f1, Path f2)
    {
        super(f1.toString() + " -> " + f2.toString());
    }
    public RenameException(Path f1, Path f2, Exception e)
    {
        super(f1.toString() + " -> " + f2.toString(), e);
    }
}

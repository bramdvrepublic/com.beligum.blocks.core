package com.beligum.blocks.fs.atomic.orig;

class FileExistsException extends java.io.IOException
{
    FileExistsException()
    {
    }
    FileExistsException(String message)
    {
        super(message);
    }
    FileExistsException(java.io.File f)
    {
        super(f.toString());
    }
}

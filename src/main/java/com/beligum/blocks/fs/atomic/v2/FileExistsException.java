package com.beligum.blocks.fs.atomic.v2;

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

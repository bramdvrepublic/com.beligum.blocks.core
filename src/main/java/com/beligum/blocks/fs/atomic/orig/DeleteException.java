package com.beligum.blocks.fs.atomic.orig;

import java.io.File;
import java.io.IOException;

public class DeleteException extends IOException
{
    DeleteException()
    {
    }
    DeleteException(File f)
    {
        super(f.toString());
    }
}

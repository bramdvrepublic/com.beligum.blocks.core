package com.beligum.blocks.fs.atomic.v2;

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

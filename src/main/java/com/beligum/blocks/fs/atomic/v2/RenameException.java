package com.beligum.blocks.fs.atomic.v2;

import java.io.File;
import java.io.IOException;

public class RenameException extends IOException
{
    RenameException()
    {
    }

    RenameException(File f1, File f2)
    {
        super(f1.toString() + " -> " + f2.toString());
    }
}

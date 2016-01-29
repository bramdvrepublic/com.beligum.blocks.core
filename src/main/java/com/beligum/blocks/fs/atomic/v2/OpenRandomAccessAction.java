package com.beligum.blocks.fs.atomic.v2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

class OpenRandomAccessAction extends OpenFileAction
{
    private transient RandomAccessFile out;

    OpenRandomAccessAction(File original)
    {
        super(original, false);
    }

    protected Object execute() throws IOException
    {
        out = new RandomAccessFile(original, "rw");
        return out;
    }

    protected void close() throws IOException
    {
        out.close();
    }
}

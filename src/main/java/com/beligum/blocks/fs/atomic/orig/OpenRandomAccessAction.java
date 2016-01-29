package com.beligum.blocks.fs.atomic.orig;

import java.io.*;

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

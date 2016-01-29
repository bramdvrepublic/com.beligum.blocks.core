package com.beligum.blocks.fs.atomic.v2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class OpenFileOutputStreamAction extends OpenFileAction
{
    private FileOutputStream out;
    private boolean append;

    OpenFileOutputStreamAction(File orig, boolean append)
    {
        super(orig, !append);
        this.append = append;
    }

    protected Object execute() throws IOException
    {
        out = new FileOutputStream(original.toString(), append);
        return out;
    }

    protected void close() throws IOException
    {
        out.close();
    }
}

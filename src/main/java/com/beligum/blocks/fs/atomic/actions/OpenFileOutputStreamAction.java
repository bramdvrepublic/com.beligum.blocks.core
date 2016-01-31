package com.beligum.blocks.fs.atomic.actions;

import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;

public class OpenFileOutputStreamAction extends OpenFileAction
{
    private OutputStream out;
    private boolean append;

    public OpenFileOutputStreamAction(FileContext fileContext, Path orig, boolean append)
    {
        super(fileContext, orig, !append);
        this.append = append;
    }

    protected Object execute() throws IOException
    {
        EnumSet<CreateFlag> createFlag = append ? EnumSet.of(CreateFlag.CREATE, CreateFlag.APPEND) : EnumSet.of(CreateFlag.CREATE);
        out = this.fileContext.create(original, createFlag);

        return out;
    }

    protected void close() throws IOException
    {
        out.close();
    }
}

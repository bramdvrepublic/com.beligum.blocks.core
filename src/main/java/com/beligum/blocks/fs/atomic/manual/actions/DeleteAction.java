package com.beligum.blocks.fs.atomic.manual.actions;

import com.beligum.blocks.fs.atomic.manual.Action;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.FileNotFoundException;
import java.io.IOException;

public class DeleteAction extends Action
{
    private Path original;
    private Path backup;

    public DeleteAction(FileContext fileContext, Path f) throws IOException
    {
        super(fileContext);

        original = f;
        if (!this.fileContext.util().exists(original))
            throw new FileNotFoundException(original.toString());
    }

    protected void prepare() throws IOException
    {
        backup = generateBackupFilename(original);
    }

    protected void createBackup() throws IOException
    {
        renameNotDeleting(original, backup);
    }

    protected Object execute() throws IOException
    {
        return null;
    }

    protected void undo() throws IOException
    {
        if (!this.fileContext.util().exists(original))
            restoreBackup(backup, original);
    }

    protected void cleanup() throws IOException
    {
        deleteIfExists(backup);
    }

    public String toString()
    {
        return "[DELETE original=" + original + ", backup=" + backup + "]";
    }
}



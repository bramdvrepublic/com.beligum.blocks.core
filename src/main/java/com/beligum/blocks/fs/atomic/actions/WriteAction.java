package com.beligum.blocks.fs.atomic.actions;

import com.beligum.blocks.fs.atomic.Action;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.EnumSet;

public class WriteAction extends Action
{
    private Path original;
    private Path backup;
    private boolean append;

    public WriteAction(FileContext fileContext, Path f, boolean append)
    {
        super(fileContext);

        original = f;
        this.append = append;
    }

    protected void prepare() throws IOException
    {
        // If writing to a file that already exists, prepare to back it up.
        if (this.fileContext.util().exists(original))
            backup = generateBackupFilename(original);
    }

    protected void createBackup() throws IOException
    {
        // If writing to a file that already exists, back it up.
        if (this.fileContext.util().exists(original))
            copyNotDeleting(original, backup);
    }

    protected Object execute() throws IOException
    {
        // Get an output stream to the file.

        EnumSet<CreateFlag> createFlag = append ? EnumSet.of(CreateFlag.CREATE, CreateFlag.APPEND) : EnumSet.of(CreateFlag.CREATE);
        return this.fileContext.create(original, createFlag);
    }

    protected void undo() throws IOException
    {
        // If there is a backup file, restore it.
        if (backup != null && this.fileContext.util().exists(backup))
            restoreBackup(backup, original);

            // Otherwise, the file was newly created, so delete it.
        else
            delete(original);
    }

    protected void cleanup() throws IOException
    {
        if (backup != null)
            deleteIfExists(backup);
    }

    public String toString()
    {
        return "[WRITE original=" + original + ", backup=" + backup + "]";
    }
}

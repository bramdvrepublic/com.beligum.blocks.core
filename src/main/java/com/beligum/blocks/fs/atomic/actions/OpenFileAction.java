package com.beligum.blocks.fs.atomic.actions;

import com.beligum.blocks.fs.atomic.Action;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

public abstract class OpenFileAction extends Action
{
    protected Path original;
    private Path backup;
    private boolean exist;                // did the file exist prior to being opened?
    private transient boolean truncate;

    public OpenFileAction(FileContext fileContext, Path original, boolean truncate)
    {
        super(fileContext);

        this.original = original;
        this.truncate = truncate;
    }

    protected void prepare() throws IOException
    {
        backup = generateBackupFilename(original);
        exist = this.fileContext.util().exists(original);
    }

    protected void createBackup() throws IOException
    {
        if (exist) {
            if (truncate)
                renameNotDeleting(original, backup);
            else
                copyNotDeleting(original, backup);
        }
    }

    protected void undo() throws IOException
    {
        if (exist)
            restoreBackup(backup, original);
        else
            deleteIfExists(original);
    }

    protected void cleanup() throws IOException
    {
        deleteIfExists(backup);
    }

    public String toString()
    {
        return "[" + getClass().getName() +
               ": original=" + original + ", backup=" + backup + "]";
    }
}

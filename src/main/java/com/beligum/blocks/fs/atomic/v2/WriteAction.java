package com.beligum.blocks.fs.atomic.v2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class WriteAction extends Action
{
    private File original;
    private File backup;
    private boolean append;

    WriteAction(File f, boolean append)
    {
        original = f;
        this.append = append;
    }

    protected void prepare()
    {
        // If writing to a file that already exists, prepare to back it up.
        if (original.exists())
            backup = generateBackupFilename(original);
    }

    protected void createBackup() throws IOException
    {
        // If writing to a file that already exists, back it up.
        if (original.exists())
            copyNotDeleting(original, backup);
    }

    protected Object execute() throws IOException
    {
        // Get an output stream to the file.
        return new FileOutputStream(original, append);
    }

    protected void undo() throws IOException
    {
        // If there is a backup file, restore it.
        if (backup != null && backup.exists())
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

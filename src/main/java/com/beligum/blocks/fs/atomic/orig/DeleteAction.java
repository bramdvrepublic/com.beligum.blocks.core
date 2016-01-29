package com.beligum.blocks.fs.atomic.orig;

import java.io.*;

class DeleteAction extends Action
{
    private File original;
    private File backup;

    DeleteAction(File f) throws IOException
    {
        original = f;
        if (!original.exists())
            throw new FileNotFoundException(original.toString());
    }

    protected void prepare()
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
        if (!original.exists())
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



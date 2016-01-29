package com.beligum.blocks.fs.atomic.v2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

class DeleteAction extends Action
{
    private File original;
    private File backup;
    private boolean isFile;

    DeleteAction(File f) throws IOException
    {
        original = f;
        if (!original.exists())
            throw new FileNotFoundException(original.toString());
        isFile = f.isFile();
    }

    protected void prepare()
    {
        backup = generateBackupFilename(original);
    }

    protected void createBackup() throws IOException
    {
        if (isFile)
            renameNotDeleting(original, backup);
        else
            delete(original);
    }

    protected Object execute() throws IOException
    {
        return null;
    }

    protected void undo() throws IOException
    {
        if (!original.exists()) {
            if (isFile) {
                restoreBackup(backup, original);
            }
            else {
                if (!original.mkdir())
                    throw new IOException(original.toString());
            }
        }
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



package com.beligum.blocks.fs.atomic.v2;

import java.io.File;
import java.io.IOException;

class MakeDirectoryAction extends Action
{
    private File original;
    private boolean alreadyExists;

    MakeDirectoryAction(File f) throws IOException
    {
        original = f;

        // Throw an exception if trying to overwrite a file with a directory.
        if (original.isFile())
            throw new IOException("A file already exists at path '" +
                                  original + "'");

        // If the directory already exists, mark it so it won't be deleted
        // on rollback.
        alreadyExists = original.isDirectory();
    }

    protected void prepare()
    {
    }

    protected void createBackup()
    {
    }

    protected Object execute() throws IOException
    {
        // Note that this uses java.io.File.mkdir instead of mkdirs,
        // because mkdirs might successfully make some parent
        // directories even if the operation fails.
        return new Boolean(original.mkdir());
    }

    protected void undo() throws IOException
    {
        // Delete the directory, but only if it didn't already exist
        // before this action.
        if (!alreadyExists)
            deleteIfExists(original);
    }

    protected void cleanup()
    {
    }

    public String toString()
    {
        return "[MKDIR original=" + original + "]";
    }
}

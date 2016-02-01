package com.beligum.blocks.fs.atomic.manual;

import com.beligum.blocks.fs.HdfsUtils;
import com.beligum.blocks.fs.atomic.manual.exceptions.DeleteException;
import com.beligum.blocks.fs.atomic.manual.exceptions.FileExistsException;
import com.beligum.blocks.fs.atomic.manual.exceptions.RenameException;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.Serializable;

public abstract class Action implements Serializable
{
    private transient Transaction txn;
    protected transient FileContext fileContext;

    public void setTransaction(Transaction t)
    {
        txn = t;
    }

    protected Action(FileContext fileContext)
    {
        this.fileContext = fileContext;
    }

    ////////////////////////////////////////////////////////////////
    /// Life-cycle methods.

    /**
     * Prepare for the action by storing all information required
     * for undo in non-transient instance variables.
     * <i>This method should not change the state of the filesystem.</i>
     * After this call, the Action object will be serialized into the journal.
     * Exceptions are considered transaction errors, and will result in a
     * rollback.
     */
    protected abstract void prepare() throws IOException;

    /**
     * Create a backup file, if necessary.
     * Exceptions cause rollback.
     */
    protected void createBackup() throws IOException
    {
    }

    /**
     * Do the action.  Exceptions are reported to the user as is.
     */
    protected abstract Object execute() throws IOException;

    /**
     * Called at commit or rollback, only if action was successful.
     */
    protected void close() throws IOException
    {
    }

    /**
     * Undo the effects of the action, if those effects are present.
     * This must be <i>locally idempotent</i>; it must be able to be
     * invoked multiple times with the same results, assuming no other
     * changes to the relevant files are made between invocations.
     * <p/>
     * Exceptions thrown from this method are considered
     * to leave the system in an inconsistent state.
     */
    protected abstract void undo() throws IOException;

    /**
     * Delete backup and perform other needed cleanup.
     * Called after commit and rollback.
     * Exceptions thrown during cleanup do not prevent commit or rollback
     * from succeeding.  They are not thrown out of Transaction methods;
     * instead, you can get them by calling Transaction.getCleanupExceptions.
     */
    protected void cleanup() throws IOException
    {
    }

    ////////////////////////////////////////////////////////////////
    /// Utilities for subclasses.

    private static int uniqueFileNum = 0;

    protected Path generateBackupFilename(Path f) throws IOException
    {
        int n;
        Path result;
        do {
            synchronized (Action.class) {
                n = uniqueFileNum++;
            }
            result = new Path(f.toUri().toString() + n + txn.getTransactionManager().getFileExtension());
        } while (this.fileContext.util().exists(result));

        return result;
    }

    ////////////////////////////////////////////////////////////////

    protected void restoreBackup(Path backup, Path original)
                    throws IOException
    {
        if (this.fileContext.util().exists(backup)) {
            renameDeleting(backup, original);
        }
    }

    ////////////////////////////////////////////////////////////////
    /// Path utilities.

    /**
     * Delete the file.  Uses Java's semantics for delete.
     *
     * @throws DeleteException where File.delete would return false
     */
    protected void delete(Path f) throws DeleteException
    {
        try {
            this.fileContext.delete(f, false);
        }
        catch (IOException e) {
            throw new DeleteException(f, e);
        }
    }

    /**
     * Delete the file only if it exists.
     */
    protected synchronized void deleteIfExists(Path f) throws IOException
    {
        if (this.fileContext.util().exists(f))
            delete(f);
    }

    /**
     * Copy source to dest, overwriting dest.
     */
    protected void copyDeleting(Path source, Path dest) throws IOException
    {
        this.fileContext.util().copy(source, dest, false, true);
    }

    protected void copyNotDeleting(Path source, Path dest) throws IOException, FileExistsException
    {
        if (HdfsUtils.createNewFile(this.fileContext, dest))
            copyDeleting(source, dest);
        else
            throw new FileExistsException(dest);
    }

    /**
     * Rename the source file to the destination file, deleting the destination
     * file if it exists.  For filesystems where a file cannot be renamed
     * to an existing file, this method is not atomic.
     *
     * @throws DeleteException if the existing file could not be deleted
     * @throws RenameException if the renaming failed
     */
    protected synchronized void renameDeleting(Path source, Path dest) throws IOException
    {
        if (!txn.getTransactionManager().renameCanDelete())
            deleteIfExists(dest);
        rename(source, dest);
    }

    /**
     * Rename the source file to the destination file, throwing an exception
     * if the destination file exists.
     *
     * @throws RenameException     if there is an error in File.renameTo
     * @throws FileExistsException if dest exists
     */
    protected synchronized void renameNotDeleting(Path source, Path dest) throws IOException
    {
        if (!txn.getTransactionManager().renameCanDelete())
            rename(source, dest);
        else if (HdfsUtils.createNewFile(this.fileContext, dest))
            rename(source, dest);
        else
            throw new FileExistsException(dest.toString());
    }

    protected void rename(Path source, Path dest) throws RenameException
    {
        try {
            this.fileContext.rename(source, dest);
        }
        catch (IOException e) {
            throw new RenameException(source, dest, e);
        }
    }
}

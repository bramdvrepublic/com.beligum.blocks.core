package com.beligum.blocks.filesystem.hdfs;

import com.beligum.base.utils.Logger;
import org.xadisk.bridge.proxies.interfaces.XASession;

import java.io.File;

/**
 * Created by bram on 2/1/16.
 */
public class XADiskUtil
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Mostly copied from
     *
     * @see org.apache.hadoop.fs.FileUtil#fullyDelete(File, boolean)
     */
    public static boolean fullyDelete(XASession tx, final File dir)
    {
        if (deleteImpl(tx, dir, false)) {
            // dir is (a) normal file, (b) symlink to a file, (c) empty directory or
            // (d) symlink to a directory
            return true;
        }
        // handle nonempty directory deletion
        if (!fullyDeleteContents(tx, dir)) {
            return false;
        }
        return deleteImpl(tx, dir, true);
    }


    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private static boolean deleteImpl(XASession tx, final File f, final boolean doLog)
    {
        if (f == null) {
            Logger.warn("null file argument.");
            return false;
        }
        boolean wasDeleted = false;
        try {
            //XADisk can't delete a non existing file (throws setRollbackOnly)
            if (tx.fileExists(f)) {
                tx.deleteFile(f);
                wasDeleted = true;
            }
        }
        catch (Exception e) {
            if (doLog) {
                Logger.error("Exception caught while deleting file or dir; " + f.getAbsolutePath(), e);
            }
        }

        if (wasDeleted) {
            return true;
        }
        else {
            //it wasn't deleted so we assume it still exists
            boolean ex = true;
            try {
                ex = tx.fileExists(f);
            }
            catch (Exception e) {
                if (doLog) {
                    Logger.error("Exception caught while checking for a deleted file or dir; " + f.getAbsolutePath(), e);
                }
            }
            if (doLog && ex) {
                Logger.warn("Failed to delete file or dir [" + f.getAbsolutePath() + "]: it still exists.");
            }

            return !ex;
        }
    }
    /**
     * Delete the contents of a directory, not the directory itself.  If
     * we return false, the directory may be partially-deleted.
     * If dir is a symlink to a directory, all the contents of the actual
     * directory pointed to by dir will be deleted.
     */
    public static boolean fullyDeleteContents(XASession tx, final File dir)
    {
        boolean deletionSucceeded = true;

        final String[] contents;
        try {
            contents = tx.listFiles(dir);
            if (contents != null) {
                for (int i = 0; i < contents.length; i++) {
                    File file = new File(dir, contents[i]);
                    if (!tx.fileExistsAndIsDirectory(file)) {
                        if (!deleteImpl(tx, file, true)) {// normal file or symlink to another file
                            deletionSucceeded = false;
                            continue; // continue deletion of other files/dirs under dir
                        }
                    }
                    else {
                        // Either directory or symlink to another directory.
                        // Try deleting the directory as this might be a symlink
                        boolean b = false;
                        b = deleteImpl(tx, file, false);
                        if (b) {
                            //this was indeed a symlink or an empty directory
                            continue;
                        }
                        // if not an empty directory or symlink let
                        // fullydelete handle it.
                        if (!fullyDelete(tx, file)) {
                            deletionSucceeded = false;
                            // continue deletion of other files/dirs under dir
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            deletionSucceeded = false;
            Logger.error("Exception caught while recursively deleting a file or dir; " + dir.getAbsolutePath(), e);
        }

        return deletionSucceeded;
    }
}

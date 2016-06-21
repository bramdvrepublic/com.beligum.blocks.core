package com.beligum.blocks.fs.ifaces;

import com.beligum.blocks.fs.LockFile;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;
import org.apache.tika.mime.MediaType;

import java.io.IOException;

/**
 * <p>
 *     This is a wrapper for the Paths in our file system and calculates all kinds of meta data
 * </p>
 * Created by bram on 9/17/15.
 */
public interface ResourcePath
{
    //-----INTERFACES-----

    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    Path getLocalPath();
    FileContext getFileContext();
    Path getMetaFolder();
    Path getMetaHashFile();
    Path getMetaLogFile();
    Path getMetaHistoryFolder();
    Path getMetaMonitorFolder();
    Path getMetaProxyFolder();
    Path getMetaProxyFolder(MediaType mimeType);
    Path getMetaMetadataFolder();
    String getMetaHashChecksum();
    String calcHashChecksum() throws IOException;
    LockFile acquireLock() throws IOException;
    boolean isLocked() throws IOException;
    void releaseLockFile(LockFile lock) throws IOException;
    /**
     * Returns if this file is a metadata file or folder (somewhere in/down the metafolder tree of a media file)
     */
    boolean isMetaFile();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

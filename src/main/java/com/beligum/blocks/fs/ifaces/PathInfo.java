package com.beligum.blocks.fs.ifaces;

import com.beligum.blocks.fs.LockFile;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;
import org.apache.tika.mime.MediaType;

import java.io.IOException;
import java.net.URI;

/**
 * <p>
 *     This is a wrapper for the Paths in our file system and calculates all kinds of meta data
 * </p>
 * Created by bram on 9/17/15.
 */
public interface PathInfo
{
    //-----INTERFACES-----

    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    URI getUri();

    Path getPath();

    FileContext getFileContext();

    Path getMetaFolder();

    Path getMetaHashFile();

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

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

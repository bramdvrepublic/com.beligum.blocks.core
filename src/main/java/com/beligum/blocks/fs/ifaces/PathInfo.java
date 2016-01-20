package com.beligum.blocks.fs.ifaces;

import com.beligum.blocks.fs.LockFile;

import java.io.IOException;

/**
 * <p>
 *     This is a wrapper for the Paths in our file system and calculates all kinds of meta data
 * </p>
 * Created by bram on 9/17/15.
 */
public interface PathInfo<T>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    T getPath();

    T getMetaFolder();

    T getMetaHashFile();

    T getMetaHistoryFolder();

    T getMetaMonitorFolder();

    T getMetaProxyFolder();

    T getMetaMetadataFolder();

    String getMetaHashChecksum();

    String calcHashChecksum() throws IOException;

    LockFile<T> acquireLock() throws IOException;

    void releaseLockFile(LockFile<T> lock) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

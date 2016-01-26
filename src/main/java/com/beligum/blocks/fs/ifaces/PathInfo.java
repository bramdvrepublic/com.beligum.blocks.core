package com.beligum.blocks.fs.ifaces;

import com.beligum.blocks.fs.LockFile;
import org.apache.tika.mime.MediaType;

import java.io.IOException;
import java.net.URI;

/**
 * <p>
 *     This is a wrapper for the Paths in our file system and calculates all kinds of meta data
 * </p>
 * Created by bram on 9/17/15.
 */
public interface PathInfo<T>
{
    //-----INTERFACES-----
    interface PathFactory<T>
    {
        T create(URI uri);
        T create(T parent, T child);
        T create(T parent, String childName);
    }

    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    PathFactory<T> getPathFactory();

    T getPath();

    Object getPathFileSystem();

    T getMetaFolder();

    T getMetaHashFile();

    T getMetaHistoryFolder();

    T getMetaMonitorFolder();

    T getMetaProxyFolder();

    T getMetaProxyFolder(MediaType mimeType);

    T getMetaMetadataFolder();

    String getMetaHashChecksum();

    String calcHashChecksum() throws IOException;

    LockFile<T> acquireLock() throws IOException;

    boolean isLocked() throws IOException;

    void releaseLockFile(LockFile<T> lock) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

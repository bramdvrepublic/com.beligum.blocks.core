package com.beligum.blocks.filesystem.ifaces;

import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * Created by bram on 6/20/16.
 */
public interface FsMonitor
{
    //-----CONSTANTS-----
    interface Listener
    {
        void onFileCreated(final FsMonitor fsMonitor, final Path file);
        void onFileModified(final FsMonitor fsMonitor, final Path file);
        void onFileDeleted(final FsMonitor fsMonitor, final Path file);
        void onDirectoryCreated(final FsMonitor fsMonitor, final Path dir);
        void onDirectoryModified(final FsMonitor fsMonitor, final Path dir);
        void onDirectoryDeleted(final FsMonitor fsMonitor, final Path dir);
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * @return the root folder (inside the configured FileContext, see above method) that's being monitored
     */
    Path getRootFolder();

    /**
     * Starts watching for filesystem changes (changes will be broadcast to all registered listeners)
     */
    void start() throws IOException;

    /**
     * Stop watching for filesystem changes and return immediately (asynchronous)
     */
    void stop();

    /**
     * Stop watching for filesystem changes and wait for all threads to complete
     */
    void stopAndWait();

    /**
     * Register a new listener
     */
    boolean registerListener(Listener listener);

    /**
     * Unregister a listener (does nothing if not registered previously)
     */
    boolean unregisterListener(Listener listener);

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

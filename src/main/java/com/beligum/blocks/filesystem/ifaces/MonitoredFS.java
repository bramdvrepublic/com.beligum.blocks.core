package com.beligum.blocks.filesystem.ifaces;

import java.io.IOException;

/**
 * Created by bram on 6/20/16.
 */
public interface MonitoredFS
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * This method will be called by outside code to build a watcher instance for this filesystem.
     */
    FsMonitor createNewMonitor() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.filesystem.hdfs.monitor;

import com.beligum.blocks.filesystem.ifaces.FsMonitor;
import org.apache.hadoop.fs.Path;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by bram on 6/20/16.
 */
public abstract class AbstractFsMonitor implements FsMonitor
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected Path rootFolder;
    protected Set<Listener> listeners;

    //-----CONSTRUCTORS-----
    protected AbstractFsMonitor(Path rootFolder)
    {
        this.rootFolder = rootFolder;
        this.listeners = new LinkedHashSet<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public org.apache.hadoop.fs.Path getRootFolder()
    {
        return this.rootFolder;
    }
    @Override
    public boolean registerListener(Listener listener)
    {
        return this.listeners.add(listener);
    }
    @Override
    public boolean unregisterListener(Listener listener)
    {
        return this.listeners.remove(listener);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

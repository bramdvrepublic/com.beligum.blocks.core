package com.beligum.blocks.filesystem.hdfs.monitor.local;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.ifaces.FsMonitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class LocalWatchGrowTimer extends TimerTask
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private long lastSize;
    private Path path;
    private boolean creatingFile;
    private Map<Path, Timer> timers;
    private LocalFSMonitor fsMonitor;

    //-----CONSTRUCTORS-----
    public LocalWatchGrowTimer(Path path, boolean creatingFile, Map<Path, Timer> timers, LocalFSMonitor fsMonitor)
    {
        this.path = path;
        //always let it run once (avoid slow starts and 0==0)
        this.lastSize = -1;
        this.creatingFile = creatingFile;
        this.timers = timers;
        this.fsMonitor = fsMonitor;
    }

    //-----PUBLIC METHODS-----
    /**
     * The action to be performed by this timer task.
     */
    @Override
    public void run()
    {
        try {
            long size = Files.size(this.path);
            //the -1 makes sure we run at least one time
            if (this.lastSize != -1 && size == this.lastSize) {
                //done
                this.timers.get(this.path).cancel();
                this.timers.remove(this.path);

                if (creatingFile) {
                    Logger.debug("File created; " + this.path);
                    for (FsMonitor.Listener listener : this.fsMonitor.getListeners()) {
                        listener.onFileCreated(this.fsMonitor, this.fsMonitor.toHdfsPath(this.path));
                    }
                }
                else {
                    Logger.debug("File modified; " + this.path);
                    for (FsMonitor.Listener listener : this.fsMonitor.getListeners()) {
                        listener.onFileModified(this.fsMonitor, this.fsMonitor.toHdfsPath(this.path));
                    }
                }
            }
            else {
                //not done yet
                //wait two seconds
                Thread.sleep(2000);
                this.lastSize = size;
            }
        }
        catch (Exception e) {
            Logger.error("Caught setRollbackOnly while monitoring file size of file, aborting " + this.path, e);

            //don't forget to clean up
            this.timers.get(this.path).cancel();
            this.timers.remove(this.path);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

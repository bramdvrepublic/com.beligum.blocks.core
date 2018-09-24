/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.filesystem.hdfs.monitor.local;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.hdfs.monitor.AbstractFsMonitor;
import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.nio.file.*;

/**
 * Created by bram on 12/17/14.
 */
public class LocalFSMonitor extends AbstractFsMonitor
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Path filesystemRoot;
    private boolean relativizeToRoot;
    private boolean includeHidden;
    private LocalProcessingThread watchThread;

    /**
     * Creates a WatchService and registers the given directory
     *
     * @param filesystemRoot the absolute root folder to monitor
     * @param relativizeToRoot whether the returned paths should be absolute or relativized to the filesystemRoot (with leading slashes). Note: this will also set the root path to "/"
     * @param includeHidden whether to emit events for hidden files
     * @throws IOException
     */
    public LocalFSMonitor(Path filesystemRoot, boolean relativizeToRoot, boolean includeHidden) throws IOException
    {
        super(relativizeToRoot ? new org.apache.hadoop.fs.Path("/") : new org.apache.hadoop.fs.Path(filesystemRoot.toUri()));

        this.filesystemRoot = filesystemRoot;
        this.relativizeToRoot = relativizeToRoot;
        this.includeHidden = includeHidden;

        this.watchThread = new LocalProcessingThread(this);
        this.watchThread.setDaemon(true);
    }

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public synchronized void start()
    {
        this.watchThread.start();
    }
    @Override
    public synchronized void stop()
    {
        if (this.watchThread != null) {
            this.watchThread.keepRunning(false);
        }
    }
    @Override
    public synchronized void stopAndWait()
    {
        this.stop();

        if (this.watchThread != null) {
            try {
                this.watchThread.join();
            }
            catch (InterruptedException e) {
                // Don't care
            }
        }
    }
    public Path getFilesystemRoot()
    {
        return filesystemRoot;
    }
    public boolean isIncludeHidden()
    {
        return includeHidden;
    }
    public org.apache.hadoop.fs.Path toHdfsPath(java.nio.file.Path absJavaPath)
    {
        org.apache.hadoop.fs.Path retVal = null;

        if (absJavaPath != null) {
            if (this.relativizeToRoot) {
                //subtle: HDFS (especially chroot'ed FS) expect the resulting path to be absolute (with leading slash)
                absJavaPath = Paths.get("/").resolve(this.filesystemRoot.relativize(absJavaPath));
            }

            retVal = new org.apache.hadoop.fs.Path(absJavaPath.toUri());
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

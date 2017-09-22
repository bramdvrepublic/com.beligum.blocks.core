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

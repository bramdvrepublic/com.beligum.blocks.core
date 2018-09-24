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

package com.beligum.blocks.filesystem.hdfs.monitor;

import com.beligum.blocks.filesystem.ifaces.FsMonitor;
import org.apache.hadoop.fs.Path;

import java.util.Collection;
import java.util.Collections;
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
        this.listeners = Collections.synchronizedSet(new LinkedHashSet<>());
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
    @Override
    public Collection<Listener> getListeners()
    {
        return this.listeners;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

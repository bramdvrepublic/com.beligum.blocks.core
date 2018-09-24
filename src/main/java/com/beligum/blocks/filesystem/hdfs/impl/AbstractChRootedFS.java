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

package com.beligum.blocks.filesystem.hdfs.impl;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.hdfs.impl.orig.v2_7_1.ChRootedFs;
import com.beligum.blocks.filesystem.hdfs.monitor.local.LocalFSMonitor;
import com.beligum.blocks.filesystem.hdfs.xattr.XAttrResolver;
import com.beligum.blocks.filesystem.ifaces.FsMonitor;
import com.beligum.blocks.filesystem.ifaces.LocalFS;
import com.beligum.blocks.filesystem.ifaces.MonitoredFS;
import com.beligum.blocks.filesystem.ifaces.XAttrFS;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.AbstractFileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Created by bram on 2/5/17.
 */
public abstract class AbstractChRootedFS extends ChRootedFs implements LocalFS, MonitoredFS, XAttrFS
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected XAttrResolver xAttrResolver;

    //-----CONSTRUCTORS-----
    protected AbstractChRootedFS(URI uri, Configuration conf, AbstractFileSystem fs) throws URISyntaxException
    {
        super(uri, conf, fs);
    }

    //-----PUBLIC METHODS-----
    @Override
    public byte[] getXAttr(Path path, String name) throws IOException
    {
        if (this.xAttrResolver != null) {
            return this.xAttrResolver.getXAttr(path, name);
        }
        else {
            return super.getXAttr(path, name);
        }
    }
    @Override
    public Map<String, byte[]> getXAttrs(Path path) throws IOException
    {
        if (this.xAttrResolver != null) {
            return this.xAttrResolver.getXAttrs(path);
        }
        else {
            return super.getXAttrs(path);
        }
    }
    @Override
    public Map<String, byte[]> getXAttrs(Path path, List<String> names) throws IOException
    {
        if (this.xAttrResolver != null) {
            return this.xAttrResolver.getXAttrs(path, names);
        }
        else {
            return super.getXAttrs(path, names);
        }
    }
    @Override
    public void register(XAttrResolver xAttrResolver)
    {
        if (this.xAttrResolver != null) {
            Logger.warn("Overwriting an existing XAttrResolver, this is probably a mistake; " + this.xAttrResolver);
        }

        this.xAttrResolver = xAttrResolver;
    }
    @Override
    public FsMonitor createNewMonitor() throws IOException
    {
        return new LocalFSMonitor(Paths.get(this.resolvePath(new Path("/")).toUri()), true, false);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

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
import com.beligum.blocks.filesystem.hdfs.monitor.local.LocalFSMonitor;
import com.beligum.blocks.filesystem.hdfs.xattr.XAttrResolver;
import com.beligum.blocks.filesystem.ifaces.FsMonitor;
import com.beligum.blocks.filesystem.ifaces.LocalFS;
import com.beligum.blocks.filesystem.ifaces.MonitoredFS;
import com.beligum.blocks.filesystem.ifaces.XAttrFS;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.local.LocalConfigKeys;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * This is an extension of DelegateToFileSystem and adds default monitoring and xattr support
 * <p>
 * Created by bram on 2/2/16.
 */
public abstract class AbstractLocalFS extends DelegateToFileSystem implements LocalFS, MonitoredFS, XAttrFS
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected XAttrResolver xAttrResolver;

    //-----CONSTRUCTORS-----
    /**
     * This constructor has the signature needed by
     * {@link AbstractFileSystem#createFileSystem(URI, Configuration)}.
     *
     * @param uri  URI of the file system
     * @param conf Configuration for the file system
     */
    protected AbstractLocalFS(final URI uri, final Configuration conf, FileSystem fileSystem) throws IOException, URISyntaxException
    {
        super(uri, fileSystem, conf, fileSystem.getScheme(), false);
    }

    //-----PUBLIC METHODS-----
    @Override
    public int getUriDefaultPort()
    {
        return -1; // No default port for file:///
    }

    @Override
    public FsServerDefaults getServerDefaults() throws IOException
    {
        return LocalConfigKeys.getServerDefaults();
    }

    @Override
    public boolean isValidName(String src)
    {
        // Different local file systems have different validation rules. Skip
        // validation here and just let the OS handle it. This is consistent with
        // RawLocalFileSystem.
        return true;
    }
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

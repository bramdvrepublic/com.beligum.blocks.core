package com.beligum.blocks.filesystem.hdfs.impl;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.hdfs.impl.orig.v2_7_1.ChRootedFs;
import com.beligum.blocks.filesystem.hdfs.monitor.LocalFSMonitor;
import com.beligum.blocks.filesystem.hdfs.xattr.XAttrResolver;
import com.beligum.blocks.filesystem.ifaces.FsMonitor;
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
public abstract class AbstractChRootedFS extends ChRootedFs implements MonitoredFS, XAttrFS
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

package com.beligum.blocks.filesystem.ifaces;

import com.beligum.blocks.filesystem.hdfs.xattr.XAttrResolver;

/**
 * Created by bram on 1/14/17.
 */
public interface XAttrFS
{
    /**
     * Registers the XAttr resolver to this AbstractFileSystem
     */
    void register(XAttrResolver xAttrResolver);
}

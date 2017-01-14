package com.beligum.blocks.filesystem.ifaces;

import com.beligum.base.resources.ifaces.Resource;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

/**
 * General superclass for all resources that are built in a HDFS file system
 *
 * Created by bram on 12/30/16.
 */
public interface HdfsResource extends Resource
{
    /**
     * The path to this resource's local data in the current file context
     */
    Path getLocalStoragePath();

    /**
     * The HDFS file context that belongs to this resource.
     * Note that this means the same file can have different resource instances because of a different fileContext
     * (eg. read-only filesystem vs. read-write filesystem)
     */
    FileContext getFileContext();
}

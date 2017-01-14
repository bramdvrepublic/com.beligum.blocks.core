package com.beligum.blocks.filesystem.hdfs.xattr;

import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * All classes implementing this method will be registered into the XAttrResolver during startup.
 *
 * Created by bram on 10/23/15.
 */
public interface XAttrMapper
{
    /**
     * The name of the xattr to register.
     * For more information, see https://hadoop.apache.org/docs/r2.7.2/hadoop-project-dist/hadoop-hdfs/ExtendedAttributes.html
     *
     * Eg. user.blocks.media.PROGRESS
     */
    String getXAttribute();

    /**
     * Fetch this xattr data of the supplied path
     */
    Object resolveXAttribute(FileContext fileContext, Path path) throws IOException;
}

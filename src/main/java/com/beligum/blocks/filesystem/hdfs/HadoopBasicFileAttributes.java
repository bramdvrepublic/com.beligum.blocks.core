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

package com.beligum.blocks.filesystem.hdfs;

import org.apache.hadoop.fs.FileStatus;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of BasicFileAttributes.
 * <p/>
 * Created by bram on 2/14/16.
 */
public class HadoopBasicFileAttributes implements BasicFileAttributes
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final FileStatus fileStatus;

    //-----CONSTRUCTORS-----
    public HadoopBasicFileAttributes(FileStatus fileStatus)
    {
        this.fileStatus = fileStatus;
    }

    //-----PUBLIC METHODS-----
    @Override
    public FileTime creationTime()
    {
        return FileTime.from(this.fileStatus.getModificationTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Object fileKey()
    {
        //??? don't know what to return here
        return null;
    }

    @Override
    public boolean isDirectory()
    {
        return this.fileStatus.isDirectory();
    }

    @Override
    public boolean isOther()
    {
        return false;
    }

    @Override
    public boolean isRegularFile()
    {
        return this.fileStatus.isFile();
    }

    @Override
    public boolean isSymbolicLink()
    {
        return this.fileStatus.isSymlink();
    }

    @Override
    public FileTime lastAccessTime()
    {
        return FileTime.from(this.fileStatus.getAccessTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public FileTime lastModifiedTime()
    {
        return FileTime.from(this.fileStatus.getModificationTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public long size()
    {
        return this.fileStatus.getLen();
    }

    @Override
    public String toString()
    {
        return "[IS DIR : " + this.fileStatus.isDirectory() + "]";
    }

    //-----PROTECTED METHODS-----
    protected FileStatus getFileStatus()
    {
        return fileStatus;
    }

    //-----PRIVATE METHODS-----

}

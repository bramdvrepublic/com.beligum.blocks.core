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

package com.beligum.blocks.filesystem.hdfs.impl.local;

import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

import java.io.IOException;
import java.util.EnumSet;

/**
 * Read-only version of the Hadoop RawLocalFileSystem that overloads all write functions to throw exceptions
 * just to make sure we don't use it to edit files.
 * <p>
 * Created by bram on 2/1/16.
 */
public class ReadOnlyRawLocalFileSystem extends RawLocalFileSystem
{
    //-----CONSTANTS-----
    public static final String SCHEME = "fileRo";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ReadOnlyRawLocalFileSystem() throws IOException
    {
        super();
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getScheme()
    {
        return SCHEME;
    }
    @Override
    public FSDataOutputStream append(Path f, int bufferSize, Progressable progress) throws IOException
    {
        return this.throwReadOnly("append");
    }
    @Override
    public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException
    {
        return this.throwReadOnly("instance");
    }
    @Override
    @Deprecated
    public FSDataOutputStream createNonRecursive(Path f, FsPermission permission, EnumSet<CreateFlag> flags, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException
    {
        return this.throwReadOnly("createNonRecursive");
    }
    @Override
    public FSDataOutputStream create(Path f, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException
    {
        return this.throwReadOnly("instance");
    }
    @Override
    public FSDataOutputStream createNonRecursive(Path f, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException
    {
        return this.throwReadOnly("createNonRecursive");
    }
    @Override
    public boolean rename(Path src, Path dst) throws IOException
    {
        return this.throwReadOnly("rename");
    }
    @Override
    public boolean truncate(Path f, final long newLength) throws IOException
    {
        return this.throwReadOnly("truncate");
    }
    @Override
    public boolean delete(Path p, boolean recursive) throws IOException
    {
        return this.throwReadOnly("delete");
    }
    @Override
    public boolean mkdirs(Path f) throws IOException
    {
        return this.throwReadOnly("mkdirs");
    }
    @Override
    public boolean mkdirs(Path f, FsPermission permission) throws IOException
    {
        return this.throwReadOnly("mkdirs");
    }
    @Override
    public void moveFromLocalFile(Path src, Path dst) throws IOException
    {
        this.throwReadOnly("moveFromLocalFile");
    }
    @Override
    public Path startLocalOutput(Path fsOutputFile, Path tmpLocalFile) throws IOException
    {
        return this.throwReadOnly("startLocalOutput");
    }
    @Override
    public void completeLocalOutput(Path fsWorkingFile, Path tmpLocalFile) throws IOException
    {
        this.throwReadOnly("completeLocalOutput");
    }
    @Override
    public void setOwner(Path p, String username, String groupname) throws IOException
    {
        this.throwReadOnly("setOwner");
    }
    @Override
    public void setPermission(Path p, FsPermission permission) throws IOException
    {
        this.throwReadOnly("setPermission");
    }
    @Override
    public void setTimes(Path p, long mtime, long atime) throws IOException
    {
        this.throwReadOnly("setTimes");
    }
    @Override
    public void createSymlink(Path target, Path link, boolean createParent) throws IOException
    {
        this.throwReadOnly("createSymlink");
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private <T> T throwReadOnly(String methodName) throws IOException
    {
        throw new IOException("Unsupported method " + methodName + "() because this is a read-only file system");
    }
}

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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.util.Progressable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;

public class ReadOnlySqlFS extends SqlFS
{
    //-----CONSTANTS-----
    public static final String SCHEME = SqlFS.SCHEME+"Ro";

    //-----CONSTRUCTORS-----
    public ReadOnlySqlFS(final URI uri, final Configuration conf) throws IOException, URISyntaxException
    {
        super(uri, conf, true);
    }

    //-----PUBLIC METHODS-----
    @Override
    public synchronized FSDataOutputStream createInternal(Path f, EnumSet<CreateFlag> flag, FsPermission absolutePermission, int bufferSize, short replication, long blockSize, Progressable progress,
                                                          Options.ChecksumOpt checksumOpt, boolean createParent)
                    throws AccessControlException, FileAlreadyExistsException, FileNotFoundException, ParentNotDirectoryException, UnsupportedFileSystemException, UnresolvedLinkException, IOException
    {
        return this.throwReadOnly("createInternal");
    }
    @Override
    public synchronized void mkdir(Path dir, FsPermission permission, boolean createParent) throws AccessControlException, FileAlreadyExistsException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.throwReadOnly("mkdir");
    }
    @Override
    public synchronized boolean delete(Path f, boolean recursive) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        return this.throwReadOnly("delete");
    }
    @Override
    public synchronized boolean setReplication(Path f, short replication) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        return this.throwReadOnly("setReplication");
    }
    @Override
    public synchronized void renameInternal(Path src, Path dst) throws AccessControlException, FileAlreadyExistsException, FileNotFoundException, ParentNotDirectoryException, UnresolvedLinkException, IOException
    {
        this.throwReadOnly("renameInternal");
    }
    @Override
    public synchronized void setPermission(Path f, FsPermission permission) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.throwReadOnly("setPermission");
    }
    @Override
    public synchronized void setOwner(Path f, String username, String groupname) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.throwReadOnly("setOwner");
    }
    @Override
    public synchronized void setTimes(Path f, long mtime, long atime) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.throwReadOnly("setTimes");
    }
    @Override
    public synchronized void setVerifyChecksum(boolean verifyChecksum) throws AccessControlException, IOException
    {
        this.throwReadOnly("setVerifyChecksum");
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private <T> T throwReadOnly(String methodName) throws IOException
    {
        throw new IOException("Unsupported method " + methodName + "() because this is a read-only file system");
    }
}

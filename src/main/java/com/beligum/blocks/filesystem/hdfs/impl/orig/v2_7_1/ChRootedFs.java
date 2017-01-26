/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.beligum.blocks.filesystem.hdfs.impl.orig.v2_7_1;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.Options.ChecksumOpt;
import org.apache.hadoop.fs.permission.AclEntry;
import org.apache.hadoop.fs.permission.AclStatus;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.util.Progressable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This is the original view.ChRootedFs with mods:
 * - made the class public
 * - made it inherit from FilterFs
 * - used the constructor() and fullPath() method from the (old, original) ChRootedFileSystem implementation
 * - removed all overridden methods that don't involve paths (now bubbling up to FilterFs)
 * - checked all methods in FilterFS with a Path as argument that they're overloaded here
 * - used ViewFs as inspiration (search for 'stripOutRoot' in ViewFs) to make changes to strip out the chroot again everywhere a Path is returned
 * See https://github.com/apache/hadoop/blob/trunk/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/viewfs/ViewFs.java
 * ------------------------------------------------------------------------------
 * <code>ChrootedFs</code> is a file system with its root some path
 * below the root of its base file system.
 * Example: For a base file system hdfs://nn1/ with chRoot at /usr/foo, the
 * members will be setup as shown below.
 * <ul>
 * <li>myFs is the base file system and points to hdfs at nn1</li>
 * <li>myURI is hdfs://nn1/user/foo</li>
 * <li>chRootPathPart is /user/foo</li>
 * <li>workingDir is a directory related to chRoot</li>
 * </ul>
 * <p>
 * The paths are resolved as follows by ChRootedFileSystem:
 * <ul>
 * <li> Absolute path /a/b/c is resolved to /user/foo/a/b/c at myFs</li>
 * <li> Relative path x/y is resolved to /user/foo/<workingDir>/x/y</li>
 * </ul>
 */
@InterfaceAudience.Private
@InterfaceStability.Evolving /*Evolving for a release,to be changed to Stable */
public class ChRootedFs extends FilterFs
{
    private final URI myUri; // the base URI + the chroot
    private final Path chRootPathPart; // the root below the root of the base
    private final String chRootPathPartString;
    private final URI chRootPathPartUri;
    protected Path workingDir;

    public ChRootedFs(final URI uri, Configuration conf, final AbstractFileSystem fs) throws URISyntaxException
    {
        //note that the super constructors eat up the URI path, so make sure you don't rely on fs.getUri() to get the path
        super(fs);

        String pathString = uri.getPath();
        if (pathString.isEmpty()) {
            pathString = "/";
        }
        chRootPathPart = new Path(pathString);
        chRootPathPartString = chRootPathPart.toUri().getPath();
        chRootPathPartUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), pathString, uri.getQuery(), uri.getFragment());
        myUri = uri;
        workingDir = getHomeDirectory();
    }

    protected Path fullPath(final Path path)
    {
        super.checkPath(path);

        if (path.isAbsolute()) {
            return new Path((chRootPathPart.isRoot() ? "" : chRootPathPartString) + path.toUri().getPath());
        }
        else {
            return new Path(chRootPathPartString + workingDir.toUri().getPath(), path);
        }
    }

    /**
     * Strip out the root from the path.
     *
     * @param p - fully qualified path p
     * @return -  the remaining path  without the begining /
     */
    public String stripOutRoot(final Path p)
    {
        try {
            checkPath(p);
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException("Internal Error - path " + p + " should have been with URI " + myUri);
        }
        String pathPart = p.toUri().getPath();
        return (pathPart.length() == chRootPathPartString.length()) ? "" : pathPart.substring(chRootPathPartString.length() + (chRootPathPart.isRoot() ? 0 : 1));
    }

    @Override
    public URI getUri()
    {
        return myUri;
    }

    //Not implemented in ChrootedFileSystem neither...
    //    @Override
    //    public Path makeQualified(Path path)
    //    {
    //        return super.makeQualified(fullPath(path));
    //    }

    @Override
    public Path getInitialWorkingDirectory()
    {
    /*
     * 3 choices here: return null or / or strip out the root out of myFs's
     *  inital wd.
     * Only reasonable choice for initialWd for chrooted fds is null
     */
        return null;
    }

    public Path getResolvedQualifiedPath(final Path f)
                    throws FileNotFoundException
    {
        return makeQualified(new Path(chRootPathPartString + f.toUri().toString()));
    }

    @Override
    public boolean isValidName(String src)
    {
        return super.isValidName(fullPath(new Path(src)).toUri().toString());
    }

    @Override
    public FSDataOutputStream createInternal(final Path f,
                                             final EnumSet<CreateFlag> flag, final FsPermission absolutePermission,
                                             final int bufferSize, final short replication, final long blockSize,
                                             final Progressable progress, final ChecksumOpt checksumOpt,
                                             final boolean createParent) throws IOException, UnresolvedLinkException
    {
        return super.createInternal(fullPath(f), flag,
                                    absolutePermission, bufferSize,
                                    replication, blockSize, progress, checksumOpt, createParent);
    }

    @Override
    public boolean delete(final Path f, final boolean recursive)
                    throws IOException, UnresolvedLinkException
    {
        return super.delete(fullPath(f), recursive);
    }

    @Override
    public BlockLocation[] getFileBlockLocations(final Path f, final long start,
                                                 final long len) throws IOException, UnresolvedLinkException
    {
        return super.getFileBlockLocations(fullPath(f), start, len);
    }

    @Override
    public FileChecksum getFileChecksum(final Path f) throws IOException, UnresolvedLinkException
    {
        return super.getFileChecksum(fullPath(f));
    }

    @Override
    public FileStatus getFileStatus(final Path f) throws IOException, UnresolvedLinkException
    {
        return new ChRootedFileStatus(super.getFileStatus(fullPath(f)), this.chRootPathPartUri);
    }

    @Override
    public void access(Path path, FsAction mode) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        super.access(fullPath(path), mode);
    }

    @Override
    public FileStatus getFileLinkStatus(final Path f) throws IOException, UnresolvedLinkException
    {
        return new ChRootedFileStatus(super.getFileLinkStatus(fullPath(f)), this.chRootPathPartUri);
    }

    @Override
    public FsStatus getFsStatus(final Path f) throws AccessControlException,
                                                     FileNotFoundException, UnresolvedLinkException, IOException
    {
        return super.getFsStatus(fullPath(f));
    }

    @Override
    public Path resolvePath(final Path p) throws FileNotFoundException, UnresolvedLinkException, AccessControlException, IOException
    {
        return super.resolvePath(fullPath(p));
    }

    //Not implemented in ChrootedFileSystem neither...
    //    @Override
    //    public void checkPath(Path path)
    //    {
    //        myFs.checkPath(path);
    //    }

    //Don't really know what to do with this one
    //    @Override
    //    public String getUriPath(final Path p)
    //    {
    //        return myFs.getUriPath(p);
    //    }

    @Override
    public FileStatus[] listStatus(final Path f) throws IOException, UnresolvedLinkException
    {
        return ChRootedFileStatus.wrap(super.listStatus(fullPath(f)), this.chRootPathPartUri);
    }

    @Override
    public RemoteIterator<FileStatus> listStatusIterator(final Path f) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        RemoteIterator<FileStatus> superIter = super.listStatusIterator(f);
        return new RemoteIterator<FileStatus>()
        {
            @Override
            public boolean hasNext() throws IOException
            {
                return superIter.hasNext();
            }

            @Override
            public FileStatus next() throws IOException
            {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return ChRootedFileStatus.wrap(superIter.next(), chRootPathPartUri);
            }
        };
    }

    @Override
    public RemoteIterator<Path> listCorruptFileBlocks(Path path) throws IOException
    {
        return super.listCorruptFileBlocks(fullPath(path));
    }

    @Override
    public void mkdir(final Path dir, final FsPermission permission, final boolean createParent) throws IOException, UnresolvedLinkException
    {
        super.mkdir(fullPath(dir), permission, createParent);

    }

    @Override
    public FSDataInputStream open(final Path f) throws AccessControlException,
                                                       FileNotFoundException, UnresolvedLinkException, IOException
    {
        return super.open(fullPath(f));
    }

    @Override
    public FSDataInputStream open(final Path f, final int bufferSize) throws IOException, UnresolvedLinkException
    {
        return super.open(fullPath(f), bufferSize);
    }

    @Override
    public boolean truncate(final Path f, final long newLength)
                    throws IOException, UnresolvedLinkException
    {
        return super.truncate(fullPath(f), newLength);
    }

    @Override
    public void renameInternal(final Path src, final Path dst) throws IOException, UnresolvedLinkException
    {
        // note fullPath will check that paths are relative to this FileSystem.
        // Hence both are in same file system and a rename is valid
        super.renameInternal(fullPath(src), fullPath(dst));
    }

    @Override
    public void renameInternal(final Path src, final Path dst, final boolean overwrite) throws IOException, UnresolvedLinkException
    {
        // note fullPath will check that paths are relative to this FileSystem.
        // Hence both are in same file system and a rename is valid
        super.renameInternal(fullPath(src), fullPath(dst), overwrite);
    }

    @Override
    public void setOwner(final Path f, final String username, final String groupname) throws IOException, UnresolvedLinkException
    {
        super.setOwner(fullPath(f), username, groupname);

    }

    @Override
    public void setPermission(final Path f, final FsPermission permission) throws IOException, UnresolvedLinkException
    {
        super.setPermission(fullPath(f), permission);
    }

    @Override
    public boolean setReplication(final Path f, final short replication) throws IOException, UnresolvedLinkException
    {
        return super.setReplication(fullPath(f), replication);
    }

    @Override
    public void setTimes(final Path f, final long mtime, final long atime) throws IOException, UnresolvedLinkException
    {
        super.setTimes(fullPath(f), mtime, atime);
    }

    @Override
    public void modifyAclEntries(Path path, List<AclEntry> aclSpec) throws IOException
    {
        super.modifyAclEntries(fullPath(path), aclSpec);
    }

    @Override
    public void removeAclEntries(Path path, List<AclEntry> aclSpec) throws IOException
    {
        super.removeAclEntries(fullPath(path), aclSpec);
    }

    @Override
    public void removeDefaultAcl(Path path) throws IOException
    {
        super.removeDefaultAcl(fullPath(path));
    }

    @Override
    public void removeAcl(Path path) throws IOException
    {
        super.removeAcl(fullPath(path));
    }

    @Override
    public void setAcl(Path path, List<AclEntry> aclSpec) throws IOException
    {
        super.setAcl(fullPath(path), aclSpec);
    }

    @Override
    public AclStatus getAclStatus(Path path) throws IOException
    {
        return super.getAclStatus(fullPath(path));
    }

    @Override
    public void setXAttr(Path path, String name, byte[] value)
                    throws IOException
    {
        super.setXAttr(fullPath(path), name, value);
    }

    @Override
    public void setXAttr(Path path, String name, byte[] value, EnumSet<XAttrSetFlag> flag) throws IOException
    {
        super.setXAttr(fullPath(path), name, value, flag);
    }

    @Override
    public byte[] getXAttr(Path path, String name) throws IOException
    {
        return super.getXAttr(fullPath(path), name);
    }

    @Override
    public Map<String, byte[]> getXAttrs(Path path) throws IOException
    {
        return super.getXAttrs(fullPath(path));
    }

    @Override
    public Map<String, byte[]> getXAttrs(Path path, List<String> names)
                    throws IOException
    {
        return super.getXAttrs(fullPath(path), names);
    }

    @Override
    public List<String> listXAttrs(Path path) throws IOException
    {
        return super.listXAttrs(fullPath(path));
    }

    @Override
    public void removeXAttr(Path path, String name) throws IOException
    {
        super.removeXAttr(fullPath(path), name);
    }

    @Override
    public void createSymlink(final Path target, final Path link, final boolean createParent) throws IOException, UnresolvedLinkException
    {
    /*
     * We leave the link alone:
     * If qualified or link relative then of course it is okay.
     * If absolute (ie / relative) then the link has to be resolved
     * relative to the changed root.
     */
        super.createSymlink(fullPath(target), link, createParent);
    }

    @Override
    public Path getLinkTarget(final Path f) throws IOException
    {
        return super.getLinkTarget(fullPath(f));
    }
}

// jTDS JDBC Driver for Microsoft SQL Server and Sybase
// Copyright (C) 2004 The jTDS Project
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package com.beligum.blocks.filesystem.hdfs.impl.sql;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * An in-memory or disk based representation of binary data.
 *
 * (b) this class was adapted by Bram Biesbrouck:
 * - added constructor and made public
 * - removed the dependency on ConnectionJDBC2
 * - replaced messages by their real text strings
 * - added default maxMemSize
 *
 * @author Brian Heineman
 * @author Mike Hutchinson
 * @version $Id: BlobImpl.java,v 1.31.2.3 2009-12-30 08:45:34 ickzon Exp $
 */
public class BlobImpl implements Blob
{
    /**
     * 0 length <code>byte[]</code> as initial value for empty
     * <code>Blob</code>s.
     */
    private static final byte[] EMPTY_BLOB = new byte[0];

    //(b) 1MB max mem size: I hope it makes sense?
    private static final long DEFAULT_MAX_MEM_SIZE = 1024 * 1024;

    /**
     * The underlying <code>BlobBuffer</code>.
     */
    private final BlobBuffer blobBuffer;


    /**
     * Constructs a new empty <code>Blob</code> instance.
     *
     */
    public BlobImpl()
    {
        this(EMPTY_BLOB);
    }

    public BlobImpl(byte[] bytes)
    {
        //(b) 1MB max mem size: I hope it makes sense?
        this(null, DEFAULT_MAX_MEM_SIZE, bytes);
    }

    /**
     * Constructs a new <code>Blob</code> instance initialized with data.
     *
     * @param bytes      the blob object to encapsulate
     */
    public BlobImpl(File bufferDir, long maxMemSize, byte[] bytes)
    {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes cannot be null");
        }

        blobBuffer = new BlobBuffer(bufferDir, maxMemSize);
        blobBuffer.setBuffer(bytes, false);
    }

    //
    // ------ java.sql.Blob interface methods from here -------
    //

    public InputStream getBinaryStream() throws SQLException
    {
        return blobBuffer.getBinaryStream(false);
    }

    public byte[] getBytes(long pos, int length) throws SQLException
    {
        return blobBuffer.getBytes(pos, length);
    }

    public long length() throws SQLException
    {
        return blobBuffer.getLength();
    }

    public long position(byte[] pattern, long start) throws SQLException
    {
        return blobBuffer.position(pattern, start);
    }

    public long position(Blob pattern, long start) throws SQLException
    {
        if (pattern == null) {
            throw new SQLException("The search pattern cannot be null.", "HY009");
        }
        return blobBuffer.position(pattern.getBytes(1, (int) pattern.length()), start);
    }

    public OutputStream setBinaryStream(final long pos) throws SQLException
    {
        return blobBuffer.setBinaryStream(pos, false);
    }

    public int setBytes(long pos, byte[] bytes) throws SQLException
    {
        if (bytes == null) {
            throw new SQLException("The bytes value must not be null.", "HY009");
        }
        return setBytes(pos, bytes, 0, bytes.length);
    }

    public int setBytes(long pos, byte[] bytes, int offset, int len)
                    throws SQLException
    {
        if (bytes == null) {
            throw new SQLException("The bytes value must not be null.", "HY009");
        }
        // Force BlobBuffer to take a copy of the byte array
        // In many cases this is wasteful but the user may
        // reuse the byte buffer corrupting the original set
        return blobBuffer.setBytes(pos, bytes, offset, len, true);
    }

    public void truncate(long len) throws SQLException
    {
        blobBuffer.truncate(len);
    }

    /////// JDBC4 demarcation, do NOT put any JDBC3 code below this line ///////

    public void free() throws SQLException
    {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public InputStream getBinaryStream(long pos, long length)
                    throws SQLException
    {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }
}
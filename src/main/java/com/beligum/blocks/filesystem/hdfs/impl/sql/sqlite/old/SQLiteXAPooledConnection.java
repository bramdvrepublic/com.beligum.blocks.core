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

package com.beligum.blocks.filesystem.hdfs.impl.sql.sqlite.old;

import org.sqlite.JDBC;
import org.sqlite.SQLiteConnection;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

//******************************************************************************
//**  SQLitePooledConnection
//******************************************************************************

/**
 * This class is used by the SQLiteConnectionPoolDataSource to open/close
 * database connections.
 ******************************************************************************/

public class SQLiteXAPooledConnection implements XAConnection, XAResource
{

    // this connection is kept open as long as the XAConnection is alive
    private SQLiteConnection physicalConn;

    // this connection is replaced whenever getConnection is called
    private volatile Connection handleConn;

    private ArrayList<ConnectionEventListener> listeners = arrayList();
    private java.util.Properties config;

    //Port from New.java
    private static <T> ArrayList<T> arrayList()
    {
        return new ArrayList<T>(4);
    }

    public SQLiteXAPooledConnection(SQLiteConnection physicalConn, java.util.Properties config)
    {
        this.physicalConn = physicalConn;
        this.config = config;
    }

    @Override
    public XAResource getXAResource()
    {
        debugCode("getXAResource()");
        return this;
    }

    /**
     * Close the physical connection.
     * This method is usually called by the connection pool.
     *
     * @throws SQLException
     */
    @Override
    public void close() throws SQLException
    {
        Connection lastHandle = handleConn;
        if (lastHandle != null) {
            listeners.clear();
            lastHandle.close();
        }
        if (physicalConn != null) {
            try {
                physicalConn.close();
            }
            finally {
                physicalConn = null;
            }
        }
    }

    /**
     * Get a connection that is a handle to the physical connection. This method
     * is usually called by the connection pool. This method closes the last
     * connection handle if one exists.
     *
     * @return the connection
     */
    @Override
    public Connection getConnection() throws SQLException
    {
        debugCode("getConnection()");
        Connection lastHandle = handleConn;
        if (lastHandle != null) {
            lastHandle.close();
        }
        // this will ensure the rollback command is cached
        //physicalConn.rollback();
        handleConn = new PooledJdbcConnection(physicalConn, config);
        return handleConn;
    }

    /**
     * Get the list of prepared transaction branches.
     * This method is called by the transaction manager during recovery.
     *
     * @param flag TMSTARTRSCAN, TMENDRSCAN, or TMNOFLAGS. If no other flags are set,
     *             TMNOFLAGS must be used.
     * @return zero or more Xid objects
     * @throws XAException
     */
    @Override
    public Xid[] recover(int flag) throws XAException
    {
        debugCode("recover(" + flag + ")");
        throw new XAException("Not Implemented");
    }

    /**
     * Prepare a transaction.
     *
     * @param xid the transaction id
     * @return XA_OK
     * @throws XAException
     */
    @Override
    public int prepare(Xid xid) throws XAException
    {
        debugCode("prepare(" + xid + ");");
        throw new XAException("Not Implemented");
    }

    /**
     * Forget a transaction.
     * This method does not have an effect for this database.
     *
     * @param xid the transaction id
     */
    @Override
    public void forget(Xid xid)
    {
        debugCode("forget(" + xid + ");");
    }

    /**
     * Roll back a transaction.
     *
     * @param xid the transaction id
     * @throws XAException
     */
    @Override
    public void rollback(Xid xid) throws XAException
    {
        debugCode("rollback(" + xid + ");");
        throw new XAException("Not Implemented");
    }

    /**
     * End a transaction.
     *
     * @param xid   the transaction id
     * @param flags TMSUCCESS, TMFAIL, or TMSUSPEND
     * @throws XAException
     */
    @Override
    public void end(Xid xid, int flags) throws XAException
    {
        debugCode("end(" + xid + ", " + flags + ");");
        throw new XAException("Not Implemented");
    }

    /**
     * Start or continue to work on a transaction.
     *
     * @param xid   the transaction id
     * @param flags TMNOFLAGS, TMJOIN, or TMRESUME
     * @throws XAException
     */
    @Override
    public void start(Xid xid, int flags) throws XAException
    {
        debugCode("start(" + xid + ", " + flags + ");");
        throw new XAException("Not Implemented");
    }

    /**
     * Commit a transaction.
     *
     * @param xid      the transaction id
     * @param onePhase use a one-phase protocol if true
     * @throws XAException
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException
    {
        debugCode("commit(" + xid + ", " + onePhase + ");");
        throw new XAException("Not Implemented");
    }

    /**
     * Register a new listener for the connection.
     *
     * @param listener the event listener
     */
    @Override
    public void addConnectionEventListener(ConnectionEventListener listener)
    {
        debugCode("addConnectionEventListener(listener);");
        listeners.add(listener);
    }

    /**
     * Remove the event listener.
     *
     * @param listener the event listener
     */
    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener)
    {
        debugCode("removeConnectionEventListener(listener);");
        listeners.remove(listener);
    }

    /**
     * INTERNAL
     */
    void closedHandle()
    {
        debugCode("closedHandle();");
        ConnectionEvent event = new ConnectionEvent(this);
        // go backward so that a listener can remove itself
        // (otherwise we need to clone the list)
        for (int i = listeners.size() - 1; i >= 0; i--) {
            ConnectionEventListener listener = listeners.get(i);
            listener.connectionClosed(event);
        }
        handleConn = null;
    }

    /**
     * Get the transaction timeout.
     *
     * @return 0
     */
    @Override
    public int getTransactionTimeout()
    {
        debugCode("getTransactionTimeout");
        return 0;
    }

    /**
     * Set the transaction timeout.
     *
     * @param seconds ignored
     * @return false
     */
    @Override
    public boolean setTransactionTimeout(int seconds)
    {
        debugCode("setTransactionTimeout(" + seconds + ")");
        return false;
    }

    /**
     * Checks if this is the same XAResource.
     *
     * @param xares the other object
     * @return true if this is the same object
     */
    @Override
    public boolean isSameRM(XAResource xares)
    {
        debugCode("isSameRM(xares);");
        return xares == this;
    }

    @Override
    public void addStatementEventListener(StatementEventListener listener)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeStatementEventListener(StatementEventListener listener)
    {
        throw new UnsupportedOperationException();
    }

    private static String getFileName(String url)
    {
        String fileName = url.substring(JDBC.PREFIX.length());
        if (fileName.startsWith("//")) fileName = fileName.substring(2);
        return fileName;
    }

    /**
     * A pooled connection.
     */
    class PooledJdbcConnection extends SQLiteConnection
    {

        private boolean isClosed;

        public PooledJdbcConnection(SQLiteConnection conn, java.util.Properties config) throws SQLException
        {
            super(conn.url(), getFileName(conn.url()), config);
        }

        public synchronized void close() throws SQLException
        {
            if (!isClosed) {
                if (getAutoCommit() == false) rollback();
                //setAutoCommit(true); //<--Not sure why we need this line
                closedHandle();
                super.close(); //<--I had to add this line to explicitly close the connection...
                isClosed = true;
            }
        }

        public synchronized boolean isClosed() throws SQLException
        {
            return isClosed || super.isClosed();
        }

        protected synchronized void checkClosed(boolean write)
        {
            if (isClosed) {
                //throw DbException.get(ErrorCode.OBJECT_CLOSED);
            }

            //super.checkClosed(write);
        }

    } //PooledJdbcConnection

    /**
     * Close a statement without throwing an exception.
     *
     * @param stat the statement or null
     */
    public static void closeSilently(Statement stat)
    {
        if (stat != null) {
            try {
                stat.close();
            }
            catch (SQLException e) {
                // ignore
            }
        }
    }

    private static boolean debug = false;
    private static void debugCode(String str)
    {
        if (debug) System.err.println(str);
    }
}
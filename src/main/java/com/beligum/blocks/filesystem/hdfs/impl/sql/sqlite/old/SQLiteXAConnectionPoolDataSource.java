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

import com.beligum.blocks.filesystem.hdfs.impl.sql.sqlite.old.SQLiteXAPooledConnection;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteDataSource;

import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

//******************************************************************************
//**  SQLiteConnectionPoolDataSource
//******************************************************************************

/**
 * A data source for connection pools. It is a factory for XAConnection
 * and Connection objects. This class is usually registered in a JNDI naming
 * service.
 ******************************************************************************/

public class SQLiteXAConnectionPoolDataSource extends SQLiteDataSource implements javax.sql.ConnectionPoolDataSource
{

    //**************************************************************************
    //** Constructor
    //**************************************************************************
    /**
     * Creates a new instance of ConnectionPoolDataSource.
     */
    public SQLiteXAConnectionPoolDataSource()
    {
        super();
    }

    /**
     * Attempts to establish a physical database connection that can
     * be used as a pooled connection.
     */
    @Override
    public PooledConnection getPooledConnection() throws SQLException
    {
        return getXAConnection();
    }

    /**
     * Attempts to establish a physical database connection that can
     * be used as a pooled connection.
     */
    @Override
    public PooledConnection getPooledConnection(String user, String password) throws SQLException
    {
        return getXAConnection(user, password);
    }

    /**
     * Open a new connection using the current URL, user name and password.
     *
     * @return the connection
     */
    @Override
    public Connection getConnection() throws SQLException
    {
        //debugCodeCall("getConnection");
        return getJdbcConnection(null, null);
    }

    /**
     * Open a new connection using the current URL and the specified user name
     * and password.
     *
     * @param user     the user name
     * @param password the password
     * @return the connection
     */
    @Override
    public Connection getConnection(String user, String password) throws SQLException
    {
        //if (isDebugEnabled()) {
        //    debugCode("getConnection("+quote(user)+", "+quote(password)+");");
        //}
        return getJdbcConnection(user, password);
    }

    private XAConnection getXAConnection() throws SQLException
    {
        //int id = getNextId(XA_DATA_SOURCE);
        return getXAConnection(null, null);
    }
    private XAConnection getXAConnection(String user, String password) throws SQLException
    {
        //int id = getNextId(XA_DATA_SOURCE);
        return new SQLiteXAPooledConnection(getJdbcConnection(user, password), this.getConfig().toProperties());
    }

    private SQLiteConnection getJdbcConnection(String user, String password) throws SQLException
    {
        //System.out.println("getJdbcConnection: " + this.getUrl());
        String url = this.getUrl();
        String fileName = url.substring(JDBC.PREFIX.length());
        if (fileName.startsWith("//")) fileName = fileName.substring(2);
        return new SQLiteConnection(url, fileName, this.getConfig().toProperties());
    }
}
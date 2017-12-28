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

package com.beligum.blocks.filesystem.hdfs.impl.sql;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class TxConnection implements Connection
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Connection wrappedConnection;

    //-----CONSTRUCTORS-----
    public TxConnection(Connection wrappedConnection)
    {
        this.wrappedConnection = wrappedConnection;
    }

    //-----PUBLIC METHODS-----
    @Override
    public Statement createStatement() throws SQLException
    {
        return this.wrappedConnection.createStatement();
    }
    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        return this.wrappedConnection.prepareStatement(sql);
    }
    @Override
    public CallableStatement prepareCall(String sql) throws SQLException
    {
        return this.wrappedConnection.prepareCall(sql);
    }
    @Override
    public String nativeSQL(String sql) throws SQLException
    {
        return this.wrappedConnection.nativeSQL(sql);
    }
    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        this.wrappedConnection.setAutoCommit(autoCommit);
    }
    @Override
    public boolean getAutoCommit() throws SQLException
    {
        return this.wrappedConnection.getAutoCommit();
    }
    @Override
    public void commit() throws SQLException
    {
        this.wrappedConnection.commit();
    }
    @Override
    public void rollback() throws SQLException
    {
        this.wrappedConnection.rollback();
    }
    @Override
    public void close() throws SQLException
    {
        this.wrappedConnection.close();
    }
    @Override
    public boolean isClosed() throws SQLException
    {
        return this.wrappedConnection.isClosed();
    }
    @Override
    public DatabaseMetaData getMetaData() throws SQLException
    {
        return this.wrappedConnection.getMetaData();
    }
    @Override
    public void setReadOnly(boolean readOnly) throws SQLException
    {
        this.wrappedConnection.setReadOnly(readOnly);
    }
    @Override
    public boolean isReadOnly() throws SQLException
    {
        return this.wrappedConnection.isReadOnly();
    }
    @Override
    public void setCatalog(String catalog) throws SQLException
    {
        this.wrappedConnection.setCatalog(catalog);
    }
    @Override
    public String getCatalog() throws SQLException
    {
        return this.wrappedConnection.getCatalog();
    }
    @Override
    public void setTransactionIsolation(int level) throws SQLException
    {
        this.wrappedConnection.setTransactionIsolation(level);
    }
    @Override
    public int getTransactionIsolation() throws SQLException
    {
        return this.wrappedConnection.getTransactionIsolation();
    }
    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        return this.wrappedConnection.getWarnings();
    }
    @Override
    public void clearWarnings() throws SQLException
    {
        this.wrappedConnection.clearWarnings();
    }
    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return this.wrappedConnection.createStatement(resultSetType, resultSetConcurrency);
    }
    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return this.wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }
    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return this.wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }
    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException
    {
        return this.wrappedConnection.getTypeMap();
    }
    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException
    {
        this.wrappedConnection.setTypeMap(map);
    }
    @Override
    public void setHoldability(int holdability) throws SQLException
    {
        this.wrappedConnection.setHoldability(holdability);
    }
    @Override
    public int getHoldability() throws SQLException
    {
        return this.wrappedConnection.getHoldability();
    }
    @Override
    public Savepoint setSavepoint() throws SQLException
    {
        return this.wrappedConnection.setSavepoint();
    }
    @Override
    public Savepoint setSavepoint(String name) throws SQLException
    {
        return this.wrappedConnection.setSavepoint(name);
    }
    @Override
    public void rollback(Savepoint savepoint) throws SQLException
    {
        this.wrappedConnection.rollback(savepoint);
    }
    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException
    {
        this.wrappedConnection.releaseSavepoint(savepoint);
    }
    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return this.wrappedConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return this.wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return this.wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        return this.wrappedConnection.prepareStatement(sql, autoGeneratedKeys);
    }
    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        return this.wrappedConnection.prepareStatement(sql, columnIndexes);
    }
    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        return this.wrappedConnection.prepareStatement(sql, columnNames);
    }
    @Override
    public Clob createClob() throws SQLException
    {
        return this.wrappedConnection.createClob();
    }
    @Override
    public Blob createBlob() throws SQLException
    {
        return this.wrappedConnection.createBlob();
    }
    @Override
    public NClob createNClob() throws SQLException
    {
        return this.wrappedConnection.createNClob();
    }
    @Override
    public SQLXML createSQLXML() throws SQLException
    {
        return this.wrappedConnection.createSQLXML();
    }
    @Override
    public boolean isValid(int timeout) throws SQLException
    {
        return this.wrappedConnection.isValid(timeout);
    }
    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException
    {
        this.wrappedConnection.setClientInfo(name, value);
    }
    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException
    {
        this.wrappedConnection.setClientInfo(properties);
    }
    @Override
    public String getClientInfo(String name) throws SQLException
    {
        return this.wrappedConnection.getClientInfo(name);
    }
    @Override
    public Properties getClientInfo() throws SQLException
    {
        return this.wrappedConnection.getClientInfo();
    }
    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException
    {
        return this.wrappedConnection.createArrayOf(typeName, elements);
    }
    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException
    {
        return this.wrappedConnection.createStruct(typeName, attributes);
    }
    @Override
    public void setSchema(String schema) throws SQLException
    {
        this.wrappedConnection.setSchema(schema);
    }
    @Override
    public String getSchema() throws SQLException
    {
        return this.wrappedConnection.getSchema();
    }
    @Override
    public void abort(Executor executor) throws SQLException
    {
        this.wrappedConnection.abort(executor);
    }
    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
    {
        this.wrappedConnection.setNetworkTimeout(executor, milliseconds);
    }
    @Override
    public int getNetworkTimeout() throws SQLException
    {
        return this.wrappedConnection.getNetworkTimeout();
    }
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return this.wrappedConnection.unwrap(iface);
    }
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return this.wrappedConnection.isWrapperFor(iface);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

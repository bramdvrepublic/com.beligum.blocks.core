//// Copyright 2011 NexJ Systems Inc. This software is licensed under the terms of the Eclipse Public License 1.0
//package com.beligum.blocks.filesystem.hdfs.impl.sql.sqlite;
//
//import nexj.core.util.ObjUtil;
//import nexj.core.util.PropertyUtil;
//import org.apache.commons.io.Charsets;
//
//import javax.sql.DataSource;
//import javax.sql.XAConnection;
//import javax.sql.XADataSource;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.lang.reflect.Method;
//import java.nio.ByteBuffer;
//import java.sql.SQLException;
//import java.util.*;
//
///**
// * Class to wrap a regular DataSource to imitate an javax.sql.XADatasource by faking support for
// * Two-Phase Commit.
// * Note: DO NOT use this class if a real XADataSource implementations is available since this class
// * only emulates Two-Phase Commit for non-XA DataSoure.
// */
//public class PseudoXADataSourceWrapper implements XADataSource
//{
//    /**
//     * The parameter types used in property setter Method invocation.
//     */
//    protected final static Class[] METHOD_ARGS = new Class[] { String.class };
//
//    /**
//     * The wrapped DataSource
//     */
//    protected DataSource m_dataSource;
//
//    /**
//     * Reusable object array used to pass in the property setter value.
//     */
//    protected Object[] m_methodArg = new Object[1];
//
//    /**
//     * The map of supported property setter Method objects for DATA_SOURCE.
//     */
//    protected Map<String, Method> m_methodMap = new HashMap/*<String, Method>*/();
//
//    /**
//     * The property override values to initialize the DataSource with (null after use).
//     */
//    protected Properties m_propertyOverrides;
//
//    /**
//     * The property default values to initialize the DataSource with (null after use).
//     */
//    protected Properties m_propertyDefaults;
//
//    /**
//     * Initialize and return the wrapped DataSource.
//     *
//     * @return the initialized DataSource on null if none was set.
//     */
//    protected DataSource getDataSource()
//    {
//        if (m_dataSource != null) {
//            if (m_propertyDefaults != null) {
//                for (Enumeration/*<String>*/ itr = m_propertyDefaults.propertyNames();
//                     itr.hasMoreElements(); ) {
//                    String sKey = (String) itr.nextElement();
//
//                    setProperty("set" + sKey.substring(0, 1).toUpperCase(Locale.ENGLISH) + sKey.substring(1),
//                                m_propertyDefaults.getProperty(sKey));
//                }
//
//                m_propertyDefaults = null; // mark as initialized
//            }
//
//            if (m_propertyOverrides != null) {
//                for (Enumeration/*<String>*/ itr = m_propertyOverrides.propertyNames();
//                     itr.hasMoreElements(); ) {
//                    String sKey = (String) itr.nextElement();
//
//                    setProperty("set" + sKey.substring(0, 1).toUpperCase(Locale.ENGLISH) + sKey.substring(1),
//                                m_propertyOverrides.getProperty(sKey));
//                }
//
//                m_propertyOverrides = null; // mark as initialized
//            }
//        }
//
//        return m_dataSource;
//    }
//
//    /**
//     * @see javax.sql.XADataSource#getLogWriter()
//     */
//    public PrintWriter getLogWriter() throws SQLException
//    {
//        return getDataSource().getLogWriter();
//    }
//
//    /**
//     * @see javax.sql.XADataSource#getLoginTimeout()
//     */
//    public int getLoginTimeout() throws SQLException
//    {
//        return getDataSource().getLoginTimeout();
//    }
//
//    /**
//     * @see javax.sql.XADataSource#getXAConnection()
//     */
//    public javax.sql.XAConnection getXAConnection() throws SQLException
//    {
//        return new XAConnection(getDataSource().getConnection());
//    }
//
//    /**
//     * @see javax.sql.XADataSource#getXAConnection(java.lang.String, java.lang.String)
//     */
//    public javax.sql.XAConnection getXAConnection(String sUser, String sPassword) throws SQLException
//    {
//        return new XAConnection(getDataSource().getConnection(sUser, sPassword));
//    }
//
//    /**
//     * Set the wrapped DataSource class.
//     *
//     * @param sDataSource The wrapped DataSource class.
//     */
//    public void setDataSource(String sDataSource) throws Exception
//    {
//        m_dataSource = (DataSource) Class.forName(sDataSource).newInstance();
//    }
//
//    /**
//     * @see javax.sql.XADataSource#setLogWriter(java.io.PrintWriter)
//     */
//    public void setLogWriter(PrintWriter out) throws SQLException
//    {
//        getDataSource().setLogWriter(out);
//    }
//
//    /**
//     * @see javax.sql.XADataSource#setLoginTimeout(int)
//     */
//    public void setLoginTimeout(int nSeconds) throws SQLException
//    {
//        getDataSource().setLoginTimeout(nSeconds);
//    }
//
//    /**
//     * Set the specific parameter to the desired value.
//     *
//     * @param sParam The valid DataSource parameter to set (not null).
//     * @param sValue The value to set.
//     */
//    protected void setProperty(String sParam, String sValue) throws Exception
//    {
//        assert sParam != null;
//
//        Method method = (Method) m_methodMap.get(sParam);
//
//        if (method == null) {
//            method = m_dataSource.getClass().getMethod(sParam, METHOD_ARGS);
//            m_methodMap.put(sParam, method);
//        }
//
//        m_methodArg[0] = sValue;
//        method.invoke(m_dataSource, m_methodArg);
//        m_methodArg[0] = null;
//    }
//
//    /**
//     * Set the DataSource initialization properties.
//     *
//     * @param sProperties The dataSource initialization properties.
//     */
//    public void setProperties(String sProperties) throws IOException
//    {
//        m_propertyOverrides = new Properties();
//
//        if (sProperties != null) {
//            ByteBuffer byteBuffer = Charsets.UTF_8.encode(sProperties);
//            try (ByteArrayInputStream is = new ByteArrayInputStream(byteBuffer.array(), 0, byteBuffer.limit())) {
//                m_propertyOverrides.load(is);
//            }
//        }
//    }
//
//    /**
//     * Set the DataSOurce initialization property defaults.
//     *
//     * @param sProperties The dataSource initialization property defaults.
//     */
//    public void setPropertyDefaults(String sProperties)
//    {
//        try {
//            m_propertyDefaults = PropertyUtil.fromString(sProperties);
//        }
//        catch (Exception e) {
//            ObjUtil.rethrow(e);
//        }
//    }
//}
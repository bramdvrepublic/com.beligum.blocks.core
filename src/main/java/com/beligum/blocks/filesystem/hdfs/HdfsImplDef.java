package com.beligum.blocks.filesystem.hdfs;

import org.apache.hadoop.fs.AbstractFileSystem;

/**
 * Created by bram on 1/10/17.
 */
public class HdfsImplDef
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String scheme;
    private Class<? extends AbstractFileSystem> impl;

    //-----CONSTRUCTORS-----
    public HdfsImplDef(String scheme, Class<? extends AbstractFileSystem> impl)
    {
        this.scheme = scheme;
        this.impl = impl;
    }

    //-----PUBLIC METHODS-----
    public String getScheme()
    {
        return scheme;
    }
    public Class<? extends AbstractFileSystem> getImpl()
    {
        return impl;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MGMT METHODS-----

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof HdfsImplDef))
            return false;

        HdfsImplDef that = (HdfsImplDef) o;

        if (getScheme() != null ? !getScheme().equals(that.getScheme()) : that.getScheme() != null)
            return false;
        return getImpl() != null ? getImpl().equals(that.getImpl()) : that.getImpl() == null;
    }
    @Override
    public int hashCode()
    {
        int result = getScheme() != null ? getScheme().hashCode() : 0;
        result = 31 * result + (getImpl() != null ? getImpl().hashCode() : 0);
        return result;
    }
}

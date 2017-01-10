package com.beligum.blocks.fs.hdfs;

import org.apache.hadoop.fs.AbstractFileSystem;
import org.apache.hadoop.fs.FileSystem;

/**
 * Created by bram on 1/10/17.
 */
public class HdfsImplDef
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String scheme;
    private Class<? extends org.apache.hadoop.fs.FileSystem> oldImpl;
    private Class<? extends AbstractFileSystem> newImpl;

    //-----CONSTRUCTORS-----
    public HdfsImplDef(String scheme, Class<? extends org.apache.hadoop.fs.FileSystem> oldImpl, Class<? extends AbstractFileSystem> newImpl)
    {
        this.scheme = scheme;
        this.oldImpl = oldImpl;
        this.newImpl = newImpl;
    }

    //-----PUBLIC METHODS-----
    public String getScheme()
    {
        return scheme;
    }
    public Class<? extends FileSystem> getOldImpl()
    {
        return oldImpl;
    }
    public Class<? extends AbstractFileSystem> getNewImpl()
    {
        return newImpl;
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
        if (getOldImpl() != null ? !getOldImpl().equals(that.getOldImpl()) : that.getOldImpl() != null)
            return false;
        return getNewImpl() != null ? getNewImpl().equals(that.getNewImpl()) : that.getNewImpl() == null;
    }
    @Override
    public int hashCode()
    {
        int result = getScheme() != null ? getScheme().hashCode() : 0;
        result = 31 * result + (getOldImpl() != null ? getOldImpl().hashCode() : 0);
        result = 31 * result + (getNewImpl() != null ? getNewImpl().hashCode() : 0);
        return result;
    }
}

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

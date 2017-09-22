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

package com.beligum.blocks.filesystem.hdfs.xattr;

import com.beligum.base.utils.Logger;
import org.apache.hadoop.fs.FileContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 10/23/15.
 */
public class XAttrResolverFactory
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Map<String, XAttrMapper> mappers;

    //-----CONSTRUCTORS-----
    public XAttrResolverFactory()
    {
        this.mappers = new HashMap<>();
    }

    //-----PUBLIC METHODS-----
    public XAttrResolver create(FileContext fileContext)
    {
        return new XAttrResolver(fileContext, this.mappers);
    }
    public void register(XAttrMapper xAttrMapper)
    {
        if (mappers.containsKey(xAttrMapper.getXAttribute())) {
            Logger.warn("Re-registering (and overwriting) an XAttr mapping for '" + xAttrMapper.getXAttribute() + "'");
        }

        mappers.put(xAttrMapper.getXAttribute(), xAttrMapper);
    }
    public void deregister(XAttrMapper xAttrMapper)
    {
        if (xAttrMapper != null) {
            this.deregister(xAttrMapper.getXAttribute());
        }
    }
    public void deregister(String xAttr)
    {
        this.mappers.remove(xAttr);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

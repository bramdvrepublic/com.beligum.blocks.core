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

import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by bram on 10/23/15.
 */
public class XAttrResolver
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private FileContext fileContext;
    private Map<String, XAttrMapper> mappers;

    //-----CONSTRUCTORS-----
    public XAttrResolver(FileContext fileContext, Map<String, XAttrMapper> mappers)
    {
        this.fileContext = fileContext;
        this.mappers = mappers;
    }

    //-----PUBLIC METHODS-----
    //following 3 methods are convenience methods to be used directly in a HDFS filesystem
    public byte[] getXAttr(Path path, String name) throws IOException
    {
        Object retVal = this.resolve(name, path);
        if (retVal != null) {
            return retVal.toString().getBytes();
        }
        else {
            return null;
        }
    }
    public Map<String, byte[]> getXAttrs(Path path) throws IOException
    {
        Map<String, byte[]> retVal = new HashMap<>();

        //Note: this leans heavily on iterating all registered xattrs, perhaps somehow keep track of each xattr attached to a path
        //because if the list gets large, a lot of (possibly useless) iterations will need to be performed
        Set<String> allXAttrs = this.getRegisteredXAttributes();
        for (String xAttr : allXAttrs) {
            byte[] value = this.getXAttr(path, xAttr);
            if (value != null) {
                retVal.put(xAttr, value);
            }
        }

        return retVal;
    }
    public Map<String, byte[]> getXAttrs(Path path, List<String> names) throws IOException
    {
        Map<String, byte[]> retVal = new HashMap<>();

        for (String xattrName : names) {
            retVal.put(xattrName, this.getXAttr(path, xattrName));
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----
    protected Object resolve(String xAttr, Path path) throws IOException
    {
        XAttrMapper mapping = this.mappers.get(xAttr);
        if (mapping != null) {
            return mapping.resolveXAttribute(this.fileContext, path);
        }
        else {
            return null;
        }
    }

    //-----PRIVATE METHODS-----
    private Set<String> getRegisteredXAttributes()
    {
        return this.mappers.keySet();
    }
}

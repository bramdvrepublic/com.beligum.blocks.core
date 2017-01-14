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

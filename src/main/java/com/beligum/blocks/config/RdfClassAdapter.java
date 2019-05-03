package com.beligum.blocks.config;

import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * This adapter only sends out the CURIE of the type (instead of serializing the entire sub-object)
 *
 * Created by bram on May 02, 2019
 */
public class RdfClassAdapter extends XmlAdapter<String, RdfClass>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public String marshal(RdfClass type)
    {
        return type == null ? null : type.getCurie().toString();
    }
    @Override
    public RdfClass unmarshal(String val)
    {
        return RdfFactory.getClass(val);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

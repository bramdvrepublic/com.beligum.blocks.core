package com.beligum.blocks.utils.importer;

import java.util.Map;

/**
 * Created by bram on 3/22/16.
 */
public class RdfSubClass
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private RdfClassProperty rdfProperty;
    private Map<String, RdfClassProperty> classProperties;

    //-----CONSTRUCTORS-----
    public RdfSubClass(RdfClassProperty rdfProperty, Map<String, RdfClassProperty> classProperties)
    {
        this.rdfProperty = rdfProperty;
        this.classProperties = classProperties;
    }

    //-----PUBLIC METHODS-----
    public RdfClassProperty getClassProperty()
    {
        return rdfProperty;
    }
    public Map<String, RdfClassProperty> getClassProperties()
    {
        return classProperties;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

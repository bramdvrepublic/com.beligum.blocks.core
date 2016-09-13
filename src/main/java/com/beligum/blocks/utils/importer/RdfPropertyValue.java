package com.beligum.blocks.utils.importer;

/**
 * Created by bram on 3/25/16.
 */
public class RdfPropertyValue
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String value;

    //-----CONSTRUCTORS-----
    public RdfPropertyValue(String value)
    {
        this.value = value;
    }

    //-----PUBLIC METHODS-----
    public String getValue()
    {
        return value;
    }
    public void setValue(String value)
    {
        this.value = value;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "'"+value+"'";
    }
}

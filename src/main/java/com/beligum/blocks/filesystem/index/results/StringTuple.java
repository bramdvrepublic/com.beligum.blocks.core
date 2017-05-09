package com.beligum.blocks.filesystem.index.results;

import com.beligum.blocks.filesystem.index.ifaces.RdfResult;

/**
 * Created by bram on 6/05/17.
 */
public class StringTuple implements RdfResult.Tuple<String, String>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String label;
    private String value;

    //-----CONSTRUCTORS-----
    public StringTuple(String label, String value)
    {
        this.label = label;
        this.value = value;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getLabel()
    {
        return label;
    }
    @Override
    public String getValue()
    {
        return value;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.index.fields;

public class InternalField extends JsonField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public InternalField(String name)
    {
        super(name);
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean isInternal()
    {
        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MGMT METHODS-----

}

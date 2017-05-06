package com.beligum.blocks.filesystem.index;

import com.beligum.blocks.filesystem.index.ifaces.RdfTupleResult;
import gen.com.beligum.blocks.core.messages.blocks.core;

import java.util.NoSuchElementException;

/**
 * Created by bram on 6/05/17.
 */
public class BooleanRdfResult implements RdfTupleResult<String, String>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private int index;

    //-----CONSTRUCTORS-----
    public BooleanRdfResult()
    {
        this.index = 0;
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean hasNext()
    {
        return this.index < 2;
    }
    @Override
    public Tuple<String, String> next()
    {
        Tuple<String, String> retVal;

        switch (this.index) {
            case 0:
                retVal = new StringTuple(core.Entries.toggleLabelYes.toString(), "true");
                break;
            case 1:
                retVal = new StringTuple(core.Entries.toggleLabelNo.toString(), "false");
                break;
            default:
                throw new NoSuchElementException("No element at index " + this.index);
        }

        this.index++;

        return retVal;
    }
    @Override
    public void close() throws Exception
    {
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

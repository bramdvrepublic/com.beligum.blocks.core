package com.beligum.blocks.templating.blocks;

import com.google.common.base.Joiner;

import java.util.ArrayList;

/**
 * Same as ArrayList but with a less-verbose toString() method
 * Created by bram on 7/5/15.
 */
public class PropertyArray<E> extends ArrayList<E>
{
    //-----CONSTANTS-----
    //sync these two
    public static final String PROPARR_FIELD = "PROPARR";
    public static final boolean PROPARR = true;

    //-----VARIABLES-----
    private String cachedJoin = null;
    private int writeCounter = 0;

    //-----CONSTRUCTORS-----
    public PropertyArray()
    {
        super();
    }
    public PropertyArray(E firstElement)
    {
        this();
        this.add(firstElement);
    }

    //-----PUBLIC METHODS-----
    //NOTE sync these two
    // @see com.beligum.blocks.templating.blocks.HtmlParser for details about this
    public static final String WRITE_ONCE_METHOD_NAME = "writeOnce";
    public String writeOnce()
    {
        String retVal = "";

        if (this.writeCounter < 1) {
            //will use the overloaded toString() below
            retVal = this.buildString();
            this.writeCounter++;
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----
    private String buildString()
    {
        //this is safe because this array is constructed from the VTL while parsing and only rendered out when done
        if (this.cachedJoin == null) {
            this.cachedJoin = Joiner.on("").join(this);
        }

        return this.cachedJoin;
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    /**
     * @return the list without extra punctuation; copied and adapted from super.toString()
     */
    @Override
    public String toString()
    {
        return this.buildString();
    }
}

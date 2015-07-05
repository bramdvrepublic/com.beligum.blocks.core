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

    //-----VARIABLES-----
    private String cachedJoin = null;

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

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    /**
     * @return the list without extra punctuation; copied and adapted from super.toString()
     */
    @Override
    public String toString()
    {
        //this is safe because this array is constructed from the VTL while parsing and only rendered out when done
        if (this.cachedJoin==null) {
            this.cachedJoin = Joiner.on("").join(this);
        }

        return this.cachedJoin;
    }
}

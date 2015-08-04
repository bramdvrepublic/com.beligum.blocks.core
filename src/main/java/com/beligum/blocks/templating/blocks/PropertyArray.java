package com.beligum.blocks.templating.blocks;

import com.beligum.base.utils.Logger;
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

        if (this.writeCounter < this.size()) {
            E obj = this.get(this.writeCounter++);
            retVal = obj == null ? null : obj.toString();
        }
        else {
            Logger.warn("Trying to write out a PropertyArray at index " + this.writeCounter + " of " + this.size() + ", so overrun. This shouldn't happen.");
        }

        return retVal;
    }

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
        if (this.cachedJoin == null) {
            this.cachedJoin = Joiner.on("").join(this);
        }

        return this.cachedJoin;
    }
}

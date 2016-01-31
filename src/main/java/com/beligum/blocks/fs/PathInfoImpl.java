package com.beligum.blocks.fs;

import com.beligum.blocks.fs.ifaces.PathInfo;
import org.apache.poi.ss.formula.functions.T;

/**
 * Created by bram on 1/19/16.
 */
abstract class PathInfoImpl implements PathInfo
{
    //-----CONSTANTS-----


    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    protected abstract T getLockFile();

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    //force a toString because it's important for our loggings
    @Override
    public String toString()
    {
        return ""+this.getPath();
    }
}

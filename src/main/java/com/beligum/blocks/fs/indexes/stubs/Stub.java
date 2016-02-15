package com.beligum.blocks.fs.indexes.stubs;

import java.net.URI;

/**
 * Created by bram on 2/14/16.
 */
public interface Stub
{
    //-----CONSTANTS-----
    /**
     * Note: sync this with the name of the getter (getId()) below, if it would ever change
     */
    String ID_FIELD_NAME = "id";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    URI getId();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.config;

import java.util.HashMap;

/**
 * Created by bram on 3/7/16.
 */
public class InputTypeConfig extends HashMap<String, String>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    /**
     * Create an empty configuration
     */
    public InputTypeConfig()
    {
        super();
    }
    /**
     * Create a configuration filled with the supplied tuples
     */
    public InputTypeConfig(String[][] args)
    {
        this();

        for (String[] arg : args) {
            this.put(arg[0], arg[1]);
        }
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.fs.pages;

import com.beligum.blocks.config.Settings;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 5/2/16.
 */
public class ReadWritePage extends DefaultPageImpl
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ReadWritePage(URI uri) throws IOException
    {
        super(uri, Settings.instance().getPagesStorePath());
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

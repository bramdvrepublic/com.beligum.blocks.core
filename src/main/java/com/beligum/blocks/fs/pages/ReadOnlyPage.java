package com.beligum.blocks.fs.pages;

import com.beligum.blocks.config.Settings;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 5/2/16.
 */
public class ReadOnlyPage extends DefaultPageImpl
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ReadOnlyPage(URI uri) throws IOException
    {
        super(uri, Settings.instance().getPagesViewPath());
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

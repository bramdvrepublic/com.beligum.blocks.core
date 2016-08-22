package com.beligum.blocks.controllers;

import com.beligum.blocks.templating.blocks.DefaultTemplateController;

/**
 * Created by wouter on 2/09/15.
 */
public class GoogleMapsController extends DefaultTemplateController
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    @Override
    public void created()
    {

    }

    //-----PUBLIC METHODS-----
    public String getApiKey()
    {
        return com.beligum.blocks.config.Settings.instance().getGoogleMapsApiKey();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

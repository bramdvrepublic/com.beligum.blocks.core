package com.beligum.blocks.controllers;

import com.beligum.base.server.R;
import com.beligum.blocks.templating.blocks.DefaultTemplateController;

/**
 * Created by wouter on 2/09/15.
 */
public class GoogleMapsController extends DefaultTemplateController
{

    @Override
    public void created()
    {

    }

    public String getApiKey()
    {
        String retVal = R.configuration().getString("blocks.core.google-maps.api-key");
        if (retVal == null) {
            retVal = "";
        }
        return retVal;
    }
}

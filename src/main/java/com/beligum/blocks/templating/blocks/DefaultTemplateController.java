package com.beligum.blocks.templating.blocks;

import com.beligum.base.server.RequestContext;
import com.beligum.blocks.config.BlocksConfig;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Created by bram on 5/27/15.
 */
public abstract class DefaultTemplateController implements TemplateController
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected TemplateConfig config = new TemplateConfig();

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public TemplateController resetConfig()
    {
        this.config.clear();

        return this;
    }
    @Override
    public TemplateConfig putConfig(String key, String value)
    {
        this.config.put(key, value);

        return this.config;
    }

    //-----PROTECTED METHODS-----
    // Find the getLanguage of this url.
    protected Locale getCurrentLocale()
    {
        URI currentURI = RequestContext.getJaxRsRequest().getUriInfo().getRequestUri();
        Locale retVal = null;
        java.nio.file.Path path = Paths.get(currentURI.getPath());
        if (path.getNameCount() > 0) {
            retVal = BlocksConfig.instance().getLocaleForLanguage(path.getName(0).toString());
        }

        // if no language is found use default language
        if (retVal == null) {
            retVal = BlocksConfig.instance().getDefaultLanguage();
        }

        return retVal;
    }

    //-----PRIVATE METHODS-----

}

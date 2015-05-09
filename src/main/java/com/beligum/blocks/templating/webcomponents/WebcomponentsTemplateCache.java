package com.beligum.blocks.templating.webcomponents;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlImportTemplate;

import java.util.HashMap;

/**
 * Created by bram on 5/10/15.
 */
public class WebcomponentsTemplateCache extends HashMap<String, HtmlImportTemplate>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private WebcomponentsTemplateEngine templateEngine;

    //-----CONSTRUCTORS-----
    public WebcomponentsTemplateCache(WebcomponentsTemplateEngine templateEngine)
    {
        super();

        this.templateEngine = templateEngine;
    }

    //-----PUBLIC METHODS-----
    @Override
    public HtmlImportTemplate get(Object key)
    {
        HtmlImportTemplate retVal = super.get(key);

        if (!R.configuration().getProduction()) {
            try {
                retVal.checkReload(this.templateEngine);
            }
            catch (Exception e) {
                Logger.error("Caught exception while reloading a webcomponents template; "+retVal, e);
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

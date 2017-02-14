package com.beligum.blocks.templating.blocks;

import com.beligum.base.resources.ifaces.Source;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;

import java.io.IOException;
import java.net.URI;
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
    public TemplateConfig putConfig(String key, String value)
    {
        this.config.put(key, value);

        return this.config;
    }
    @Override
    public TemplateController resetConfig()
    {
        this.config.clear();

        return this;
    }
    @Override
    public void prepareForSave(Source source, Element element, OutputDocument htmlOutput) throws IOException
    {
        //NOOP, override in subclass if you want to do something special
    }
    @Override
    public void prepareForCopy(Source source, Element element, OutputDocument htmlOutput, URI targetUri, Locale targetLanguage) throws IOException
    {
        //NOOP, override in subclass if you want to do something special
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.templating.blocks;

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

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

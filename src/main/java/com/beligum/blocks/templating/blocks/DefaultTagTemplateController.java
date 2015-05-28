package com.beligum.blocks.templating.blocks;

/**
 * Created by bram on 5/27/15.
 */
public abstract class DefaultTagTemplateController implements TagTemplateController
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected TagTemplateConfig config = new TagTemplateConfig();

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public TagTemplateController resetConfig()
    {
        this.config.clear();

        return this;
    }
    @Override
    public TagTemplateConfig putConfig(String key, String value)
    {
        this.config.put(key, value);

        return this.config;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.templating.blocks;

/**
 * Created by bram on 5/27/15.
 */
public interface TemplateController
{
    /**
     * Internal method to clear the config values, no need to use this method directly
     *
     * @return
     */
    TemplateController resetConfig();

    /**
     * Internal method to set the config values, no need to use this method directly
     *
     * @param key
     * @param value
     * @return
     */
    TemplateConfig putConfig(String key, String value);

    /**
     * This method is called every time the controller is initialized for a template created (you can safely assume the putConfig has been set here)
     *
     * @return
     */
    void created();
}

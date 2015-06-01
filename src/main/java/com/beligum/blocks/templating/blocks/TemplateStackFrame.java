package com.beligum.blocks.templating.blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 6/1/15.
 */
public class TemplateStackFrame
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private TemplateController controller;
    private Map<String, Object> properties;

    //-----CONSTRUCTORS-----
    public TemplateStackFrame()
    {
        this.controller = null;
        this.properties = new HashMap<>();
    }

    //-----PUBLIC METHODS-----
    public TemplateController getController()
    {
        return controller;
    }
    public void setController(TemplateController controller)
    {
        this.controller = controller;
    }
    public Map<String, Object> getProperties()
    {
        return properties;
    }
    public void setProperties(Map<String, Object> properties)
    {
        this.properties = properties;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

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
    private HtmlTemplate template;
    private TemplateController controller;
    private Map<String, Object> properties;
    private int frameDepth;

    //-----CONSTRUCTORS-----
    public TemplateStackFrame(HtmlTemplate template, TemplateController controller, int frameDepth)
    {
        this.template = template;
        this.controller = controller;
        this.frameDepth = frameDepth;
        this.properties = new HashMap<>();
    }

    //-----PUBLIC METHODS-----
    public HtmlTemplate getTemplate()
    {
        return template;
    }
    public TemplateController getController()
    {
        return controller;
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

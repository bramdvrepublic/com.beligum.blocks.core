package com.beligum.blocks.templating.blocks.directives;

import java.io.StringWriter;

/**
 * Created by bram on 8/15/15.
 */
public class WriterBufferReference
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private TemplateResourcesDirective.Argument type;
    private int writerBufferPosition = -1;

    //-----CONSTRUCTORS-----
    public WriterBufferReference(TemplateResourcesDirective.Argument type, StringWriter writer)
    {
        //we save the position so we can insert right here later on (when everything is rendered)
        this.writerBufferPosition = writer.getBuffer().length();
        this.type = type;
    }

    //-----PUBLIC METHODS-----
    public TemplateResourcesDirective.Argument getType()
    {
        return type;
    }
    public int getWriterBufferPosition()
    {
        return writerBufferPosition;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

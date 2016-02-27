package com.beligum.blocks.rdf.ontology;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.models.AbstractJsonObject;
import com.beligum.blocks.config.SidebarWidget;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.net.URI;

/**
 * Created by bram on 2/25/16.
 */
public class RdfPropertyImpl extends AbstractJsonObject implements RdfProperty
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String name;
    private URI vocabulary;
    private MessagesFileEntry title;
    private MessagesFileEntry label;
    private URI dataType;
    private SidebarWidget widgetType;
    private URI[] isSameAs;

    //-----CONSTRUCTORS-----
    public RdfPropertyImpl(String name,
                           URI vocabulary,
                           MessagesFileEntry title,
                           MessagesFileEntry label,
                           URI dataType,
                           SidebarWidget widgetType,
                           URI[] isSameAs)
    {
        this.name = name;
        this.vocabulary = vocabulary;
        this.title = title;
        this.label = label;
        this.dataType = dataType;
        this.widgetType = widgetType;
        //make it uniform (always an array)
        this.isSameAs = isSameAs == null ? new URI[] {} : isSameAs;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return name;
    }
    @Override
    public URI getVocabulary()
    {
        return vocabulary;
    }
    @Override
    public String getTitle()
    {
        return title.getI18nValue();
    }
    @Override
    public String getLabel()
    {
        return label.getI18nValue();
    }
    @Override
    public URI getDataType()
    {
        return dataType;
    }
    @Override
    public String getWidgetType()
    {
        return widgetType.getConstant();
    }
    @Override
    public URI[] getIsSameAs()
    {
        return isSameAs;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

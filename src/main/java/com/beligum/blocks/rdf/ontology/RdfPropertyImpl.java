package com.beligum.blocks.rdf.ontology;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.blocks.config.SidebarWidget;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;

/**
 * Created by bram on 2/25/16.
 */
public class RdfPropertyImpl extends RdfClassImpl implements RdfProperty
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private RdfClass dataType;
    private SidebarWidget widgetType;

    //-----CONSTRUCTORS-----
    public RdfPropertyImpl(String name,
                           RdfVocabulary vocabulary,
                           MessagesFileEntry title,
                           MessagesFileEntry label,
                           RdfClass dataType,
                           SidebarWidget widgetType,
                           URI[] isSameAs)
    {
        super(name, vocabulary, title, label, isSameAs);

        this.widgetType = widgetType;
        this.dataType = dataType;
    }

    //-----PUBLIC METHODS-----
    @Override
    public RdfClass getDataType()
    {
        return dataType;
    }
    @Override
    public String getWidgetType()
    {
        return widgetType.getConstant();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.rdf.ontology;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.blocks.config.InputType;
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
    private InputType widgetType;

    //-----CONSTRUCTORS-----
    public RdfPropertyImpl(String name,
                           RdfVocabulary vocabulary,
                           MessagesFileEntry title,
                           MessagesFileEntry label,
                           RdfClass dataType,
                           InputType widgetType,
                           URI[] isSameAs)
    {
        this(name, vocabulary, title, label, dataType, widgetType, isSameAs, false);
    }
    public RdfPropertyImpl(String name,
                           RdfVocabulary vocabulary,
                           MessagesFileEntry title,
                           MessagesFileEntry label,
                           RdfClass dataType,
                           InputType widgetType,
                           URI[] isSameAs,
                           boolean isPublic)
    {
        super(name, vocabulary, title, label, isSameAs, isPublic);

        this.widgetType = widgetType;
        this.dataType = dataType;

        //we don't have subclasses so don't worry about type checking (yet)
        vocabulary.addProperty(this);
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

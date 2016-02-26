package com.beligum.blocks.rdf.ontology;

import com.beligum.base.filesystem.ConstantsFileEntry;
import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.models.AbstractJsonObject;
import com.beligum.base.server.R;

import java.net.URI;

/**
 * Created by bram on 2/25/16.
 */
public class Term extends AbstractJsonObject
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI predicate;
    private String name;
    private ConstantsFileEntry dataType;
    private ConstantsFileEntry widgetType;
    private URI[] isSameAs;

    //-----CONSTRUCTORS-----
    public Term(URI predicate, MessagesFileEntry name, ConstantsFileEntry dataType, ConstantsFileEntry widgetType, URI[] isSameAs)
    {
        this.predicate = predicate;
        this.name = R.i18nFactory().get(name);
        this.dataType = dataType;
        this.widgetType = widgetType;
        //make it uniform (always an array)
        this.isSameAs = isSameAs == null ? new URI[] {} : isSameAs;
    }

    //-----PUBLIC METHODS-----
    public URI getPredicate()
    {
        return predicate;
    }
    public String getName()
    {
        return name;
    }
    public String getDataType()
    {
        return dataType.getValue();
    }
    public String getWidgetType()
    {
        return widgetType.getValue();
    }
    public URI[] getIsSameAs()
    {
        return isSameAs;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

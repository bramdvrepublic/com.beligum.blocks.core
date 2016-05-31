package com.beligum.blocks.rdf.ontology;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.blocks.config.InputType;
import com.beligum.blocks.config.InputTypeConfig;
import com.beligum.blocks.fs.index.entries.RdfIndexer;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;
import com.beligum.blocks.rdf.ontology.indexers.DefaultRdfPropertyIndexer;
import org.openrdf.model.Value;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 2/25/16.
 */
public class RdfPropertyImpl extends RdfClassImpl implements RdfProperty
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private RdfClass dataType;
    private InputType widgetType;
    private InputTypeConfig widgetArgs;

    //-----CONSTRUCTORS-----
    public RdfPropertyImpl(String name,
                           RdfVocabulary vocabulary,
                           MessagesFileEntry title,
                           MessagesFileEntry label,
                           RdfClass dataType)
    {
        this(name, vocabulary, title, label, dataType, InputType.Undefined, null, null);
    }
    public RdfPropertyImpl(String name,
                           RdfVocabulary vocabulary,
                           MessagesFileEntry title,
                           MessagesFileEntry label,
                           RdfClass dataType,
                           InputType widgetType,
                           InputTypeConfig widgetArgs,
                           URI[] isSameAs)
    {
        this(name, vocabulary, title, label, dataType, widgetType, widgetArgs, isSameAs, false);
    }
    public RdfPropertyImpl(String name,
                           RdfVocabulary vocabulary,
                           MessagesFileEntry title,
                           MessagesFileEntry label,
                           RdfClass dataType,
                           InputType widgetType,
                           InputTypeConfig widgetArgs,
                           URI[] isSameAs,
                           boolean isPublic)
    {
        super(name, vocabulary, title, label, isSameAs, isPublic, null);

        this.widgetType = widgetType;
        //make it uniform; no nulls
        this.widgetArgs = widgetArgs == null ? new InputTypeConfig() : widgetArgs;
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
    public InputType getWidgetType()
    {
        return widgetType;
    }
    @Override
    public InputTypeConfig getWidgetConfig()
    {
        return widgetArgs;
    }
    @Override
    public void indexValue(RdfIndexer indexer, URI subject, Value value, Locale language) throws IOException
    {
        DefaultRdfPropertyIndexer.INSTANCE.index(indexer, subject, this, value, language);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

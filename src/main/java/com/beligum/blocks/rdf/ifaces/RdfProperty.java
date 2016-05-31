package com.beligum.blocks.rdf.ifaces;

import com.beligum.blocks.config.InputType;
import com.beligum.blocks.config.InputTypeAdapter;
import com.beligum.blocks.config.InputTypeConfig;
import com.beligum.blocks.fs.index.entries.RdfIndexer;
import org.openrdf.model.Value;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 2/26/16.
 */
public interface RdfProperty extends RdfClass
{
    /**
     * The full datatype (can also be XSD) of this property. This is used by the client side code, together with the WidgetType (see below),
     * to create an appropriate input method and validation for entering a value for this property.
     * Eg. http://www.w3.org/2001/XMLSchema#integer
     */
    RdfClass getDataType();

    /**
     * This widget-type to be used in the admin sidebar (or just inline, eg. in the case of the editor)
     * to enter a value for an instance of this property.
     * Eg. InlineEditor
     *
     * Note: we serialize this (eg. to JS client side) as it's constant, so we can easily check it's value client side
     */
    @XmlJavaTypeAdapter(InputTypeAdapter.class)
    InputType getWidgetType();

    /**
     * A map of key/value entries that contain specific settings for the input widget type
     */
    InputTypeConfig getWidgetConfig();

    /**
     * This method gets called when this property is indexed by our custom (currently only Lucene) indexer.
     * It should call the right method on the indexer to index the property value as closely as possible.
     */
    void indexValue(RdfIndexer indexer, URI subject, Value value, Locale language) throws IOException;
}

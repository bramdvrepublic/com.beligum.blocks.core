package com.beligum.blocks.index.ifaces;

import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

public interface IndexEntryField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * The name of this field, as it will be used in the index
     */
    String getName();

    /**
     * Returns the value of the index entry, associated with this field
     */
    String getValue(ResourceProxy resourceProxy);

    /**
     * Returns true if the value of the index entry has been set once (even if it was set to null)
     */
    boolean hasValue(ResourceProxy resourceProxy);

    /**
     * Sets the field of the indexEntry to the supplied value
     */
    void setValue(ResourceProxy resourceProxy, String value);

    /**
     * Returns true if this field is an internal field instead of a field wrapping an RDF property
     */
    boolean isInternal();

    /**
     * Virtual fields shouldn't be copied to the serialized JSON representation eg. because they
     * are implemented as Solr copyFields (eg. tokenizedUri)
     */
    boolean isVirtual();

    /**
     * Convert the RDF value to an indexable string value counterpart
     */
    String serialize(Value rdfValue, Locale language) throws IOException;

}

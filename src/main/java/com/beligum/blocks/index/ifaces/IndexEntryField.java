package com.beligum.blocks.index.ifaces;

import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

public interface IndexEntryField
{
    //-----CONSTANTS-----
    /**
     * This value is used to index "core" Fields (eg. the ones explicitly implementing IndexEntryField) to support
     * an easy means to search for null fields (eg. search for all fields NOT having this field). Analogue to:
     * https://www.elastic.co/guide/en/elasticsearch/reference/2.1/null-value.html
     * Note that this is required for the Block Join Children Query Parser, see
     * https://lucene.apache.org/solr/guide/6_6/other-parsers.html#OtherParsers-BlockJoinChildrenQueryParser
     * Note that in ES, this is NULL, but we want to mimic the JSON style as closely as possible, so we can create
     * parent-only queries like query.setParam("q", "{!child of=" + PageIndexEntry.parentUriField.getName() + ":null}");
     * This will transform eg. "image" : null to "image" : "null", but the above query still seems to work.
     */
    String NULL_VALUE = "null";

    /**
     * This will be appended to the name of this field if we're creating a "proxy field".
     * For more info, see https://github.com/republic-of-reinvention/com.stralo.framework/issues/50
     */
    String PROXY_FIELD_SUFFIX = "_proxy";

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

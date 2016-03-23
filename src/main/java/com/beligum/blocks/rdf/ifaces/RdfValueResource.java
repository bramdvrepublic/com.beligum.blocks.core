package com.beligum.blocks.rdf.ifaces;

/**
 * Created by bram on 3/22/16.
 */
public interface RdfValueResource extends RdfResource
{
    /**
     * Returns the string-representation of this literal or URI value.
     */
    String getValue();

    /**
     * Returns the full datatype (probably XSD or RDF) of this literal value.
     */
    RdfClass getDataType();
}

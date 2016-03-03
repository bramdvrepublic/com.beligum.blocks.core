package com.beligum.blocks.rdf.ifaces;

/**
 * Modeled after https://www.w3.org/TR/rdf11-concepts/#section-Graph-Literal
 *
 * Created by bram on 3/2/16.
 */
public interface RdfLiteral extends RdfResource
{
    /**
     * Returns the string-representation of this literal value.
     */
    String getValue();

    /**
     * Returns the full datatype (probably XSD or RDF) of this literal value.
     */
    RdfClass getDataType();
}

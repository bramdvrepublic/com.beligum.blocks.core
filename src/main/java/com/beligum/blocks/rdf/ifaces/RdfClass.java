package com.beligum.blocks.rdf.ifaces;

import java.net.URI;

/**
 * This is more or less the OO representation of the RDFS::Class
 *
 * Created by bram on 2/26/16.
 */
public interface RdfClass extends RdfResource
{
    /**
     * The human readable describing phrase for this class, to be used to build admin-side selection lists etc.
     * Eg. Water well
     */
    String getTitle();

    /**
     * The human readable describing phrase for this class, to be used in public HTML pages as a describing label next to the value of this class.
     * Eg. Water well
     */
    String getLabel();

    /**
     * Optional (can be null) list of other ontology URIs that describe the same concept of the class described by this class.
     * Eg. http://dbpedia.org/page/Water_well
     */
    URI[] getIsSameAs();
}

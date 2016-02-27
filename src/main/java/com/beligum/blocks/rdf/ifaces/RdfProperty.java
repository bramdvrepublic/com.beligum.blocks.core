package com.beligum.blocks.rdf.ifaces;

import com.beligum.base.models.ifaces.JsonObject;

import java.net.URI;

/**
 * Created by bram on 2/26/16.
 */
public interface RdfProperty extends JsonObject
{
    /**
     * The short, uncapitalized and camel-cased name that needs to be appended to the vocab to get the full describing URI for this term.
     * Eg. postalCode
     */
    String getName();

    /**
     * The site-specific ontology prefix URI for this property. Together with the name, it forms the full URI.
     * Eg. http://www.reinvention.be/ontology/
     */
    URI getVocabulary();

    /**
     * The human readable describing phrase for this property, to be used to build admin-side selection lists etc.
     * Eg. Postal code
     */
    String getTitle();

    /**
     * The human readable describing phrase for this property, to be used in public HTML pages as a describing label next to the value of this property.
     * Eg. zip code
     */
    String getLabel();

    /**
     * The full (non-curie) datatype (probably XSD) of this property. This is used by the client side code, together with the WidgetType (see below),
     * to create an appropriate input method and validation for entering a value for this property.
     * Eg. http://www.w3.org/2001/XMLSchema#integer
     */
    URI getDataType();

    /**
     * This widget-type to be used in the admin sidebar (or just inline, eg. in the case of the editor)
     * to enter a value for an instance of this property.
     * Eg. InlineEditor
     */
    String getWidgetType();

    /**
     * Optional (can be null) list of other ontology URIs that describe the same concept of the property described by this property.
     * Eg. http://dbpedia.org/ontology/postalCode
     */
    URI[] getIsSameAs();
}

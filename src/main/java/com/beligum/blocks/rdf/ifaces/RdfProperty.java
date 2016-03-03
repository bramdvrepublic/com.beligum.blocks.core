package com.beligum.blocks.rdf.ifaces;

/**
 * Created by bram on 2/26/16.
 */
public interface RdfProperty extends RdfResource
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
     */
    String getWidgetType();
}

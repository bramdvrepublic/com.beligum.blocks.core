package com.beligum.blocks.rdf.ifaces;

import com.beligum.base.database.models.ifaces.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This is the top-level interface for all RDF(S) related classes
 *
 * Created by bram on 3/2/16.
 */
public interface RdfResource extends JsonObject
{
    /**
     * Indicates whether this resource should be added to public comboboxes and so on (eg. the ones in the UI on the client side)
     */
    @JsonIgnore
    boolean isPublic();
}

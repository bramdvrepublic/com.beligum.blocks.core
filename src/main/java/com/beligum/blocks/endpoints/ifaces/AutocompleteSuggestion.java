package com.beligum.blocks.endpoints.ifaces;

import java.net.URI;

/**
 * Created by bram on 3/12/16.
 */
public interface AutocompleteSuggestion
{
    /**
     * The formal (machine-readable) value associated with this suggestion.
     * Mostly, this will be a URI (eg. for resource suggestions), but can also be a plain string value (eg. for enum suggestions)
     */
    String getValue();

    /**
     * The RDF type class of the returned suggestion
     */
    URI getResourceType();

    /**
     * The address of the public page of this suggestion (where you can surf to to get more information about this suggestion).
     * In case of a resource suggestion, this will be the public page (in a suitable language) of the resource.
     * In case of an enum suggestion, this will probably be null.
     */
    URI getPublicPage();

    /**
     * The main name to display to the user for this suggestions (eg. the top line in the auto-complete results entry)
     */
    String getTitle();

    /**
     * The (possibly empty) second line in the auto-complete results entry to further specify the details of the suggestion
     */
    String getSubTitle();
}

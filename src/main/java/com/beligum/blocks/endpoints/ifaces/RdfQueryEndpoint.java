package com.beligum.blocks.endpoints.ifaces;

import com.beligum.blocks.rdf.ifaces.RdfClass;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * Created by bram on 3/14/16.
 */
public interface RdfQueryEndpoint
{
    /**
     * Searches the (fast) index of the specified type for the supplied query string.
     * Mainly used to feed client-side autocomplete boxes
     *
     * @param query the query (prefix) string we're searching for
     * @param language the optional language to search for, may be null
     * @param maxResults the maximum results to return
     * @return a list of maxResults size
     * @throws IOException
     */
    List<AutocompleteSuggestion> search(RdfClass resourceType, String query, Locale language, int maxResults) throws IOException;

    /**
     * Gets the full value of the resource with the specified id-URI.
     *
     * @param resourceId the id of the resource
     * @param language the optional language we're searching a value for
     * @return the wrapped value with all possible fields indicating how to render this value (if may contain null values for some properties of AutocompleteValue)
     * @throws IOException
     */
    ResourceInfo getResource(RdfClass resourceType, URI resourceId, Locale language) throws IOException;

    /**
     * Allow the endpoint to redirect the public GET of the specified resource to another URL
     * (eg. when we're dealing with resources from external ontologies).
     * Return null if no such redirect needs to happen, then the local template will be rendered.
     *
     * @param resourceId the id of the resource
     * @param language the optional language we want to redirect to
     * @return the external URL to redirect to or null if nothing special needs to be done
     */
    URI getExternalResourceRedirect(URI resourceId, Locale language);
}

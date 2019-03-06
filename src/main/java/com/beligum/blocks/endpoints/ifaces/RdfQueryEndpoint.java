/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.endpoints.ifaces;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfOntologyMember;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.eclipse.rdf4j.model.Model;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Locale;

/**
 * Created by bram on 3/14/16.
 */
public interface RdfQueryEndpoint
{

    /**
     * Subclass this interface in endpoint-subclasses to pass specific options to the search endpoints
     */
    interface SearchOption
    {
    }

    /**
     * How to interpret the query string:
     *
     *   STARTS_WITH = prefix-search, mainly for autocomplete
     *   NAME = only search the name (or label or title) of the resource
     *   FULL = full-text search on everything we know about the resource
     */
    enum QueryType
    {
        STARTS_WITH,
        NAME,
        FULL
    }

    /**
     * Returns true if this endpoint is actually a foreign ontology, external from this website
     */
    boolean isExternal();

    /**
     * Searches the (fast) index of the specified type for the supplied query string.
     * Mainly used to feed client-side autocomplete boxes
     *
     * @param resourceType if not-null, filter only these types
     * @param query the query (prefix) string we're searching for
     * @param queryType is the query a true (google-like) query or a (more restrictive) prefix-search (eg. for autocomplete-boxes) or just a (more restrictive) name search?
     * @param language the optional language to search for, may be null
     * @param maxResults the maximum results to return
     * @param options unused (for now)
     * @return a list of maxResults size
     * @throws IOException
     */
    Collection<AutocompleteSuggestion> search(RdfOntologyMember resourceType, String query, QueryType queryType, Locale language, int maxResults, SearchOption... options) throws IOException;

    /**
     * Gets the full value of the resource with the specified id-URI.
     *
     * @param resourceType if not-null, filter only these types
     * @param resourceId the id of the resource
     * @param language the optional language we're searching a value for
     * @param options eg. can be used to pass intermediate values that are not indexed yet, but should already resolve
     * @return the wrapped value with all possible fields indicating how to render this value (if may contain null values for some properties of AutocompleteValue)
     * @throws IOException
     */
    ResourceInfo getResource(RdfOntologyMember resourceType, URI resourceId, Locale language, SearchOption... options) throws IOException;

    /**
     * Returns the RDF properties that are good candidates to find a human readable string representation of instances of the specified class.
     * Note: don't make this list too long because it impacts the SPARQL queries quite a lot.
     *
     * @param localResourceType the RDF class in the local ontology to find a label for
     * @return a list of good candidates (in priority order) of labels
     */
    RdfProperty[] getLabelCandidates(RdfClass localResourceType);

    /**
     * Translates a local resource id to a remote one for the external ontology.
     * This external URI will also be used to redirect the public GET of the specified resource to an external URL
     * (eg. when we're dealing with resources from external ontologies).
     * Return null if no such redirect needs to happen, then the local template will be rendered.
     *
     * @param resourceId the id of the resource
     * @param language the optional language we want to redirect to
     * @return the external URL to redirect to or null if nothing special needs to be done
     */
    URI getExternalResourceId(URI resourceId, Locale language);

    /**
     * Fetches the RDF model from the external ontology.
     * Returns null if this is a local endpoint.
     *
     * @param resourceType the datatype of the resource
     * @param resourceId the id of the resource
     * @param language the optional language we want to load the model for
     * @return the remote/external RDF model or null if this is a local endpoint.
     * @throws IOException
     */
    Model getExternalRdfModel(RdfClass resourceType, URI resourceId, Locale language) throws IOException;

    /**
     * Returns the RDF counterpart class in the external ontology for specified class in the local ontology.
     *
     * @param localResourceType the RDF class in the local ontology to find an external counterpart for
     * @return the external class or null if this is not an external endpoint or no counterpart class was found
     */
    RdfClass getExternalClasses(RdfClass localResourceType);

}

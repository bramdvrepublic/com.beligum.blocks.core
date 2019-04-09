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

package com.beligum.blocks.endpoints;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.endpoints.LocalQueryEndpoint;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static gen.com.beligum.blocks.core.constants.blocks.core.*;

/**
 * Created by bram on 2/25/16.
 */
@Path("/blocks/admin/rdf")
public class RdfEndpoint
{
    //-----CONSTANTS-----
    private static final RdfQueryEndpoint SEARCH_ALL_ENDPOINT = new LocalQueryEndpoint();
    //    //Note: the null-valued vocabulary indicates a dummy class to support search-all functionality
    //    //--> this was changed to use the local vocabulary instead because we don't support anonymous classes anymore...
    //    public static final RdfClass ALL_CLASSES = new RdfClassImpl("All",
    //                                                                Local.INSTANCE,
    //                                                                core.Entries.rdfClassAllTitle,
    //                                                                core.Entries.rdfClassAllLabel,
    //                                                                new URI[] {},
    //                                                                false,
    //                                                                new LocalQueryEndpoint(),
    //                                                                null);

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @GET
    @Path("/classes/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RDF_CLASS_READ_ALL_PERM)
    public Response getClasses() throws IOException
    {
        //TODO maybe we should think about specifying the ontology to get the classes of?
        return Response.ok(RdfFactory.getLocalOntology().getPublicClasses()).build();
    }

    @GET
    @Path("/properties/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RDF_PROPERTY_READ_ALL_PERM)
    public Response getProperties(@QueryParam(RDF_RES_TYPE_CURIE_PARAM) URI resourceTypeCurie) throws IOException
    {
        Iterable<RdfProperty> retVal = null;

        if (resourceTypeCurie != null) {
            RdfClass rdfClass = RdfFactory.getClass(resourceTypeCurie);
            if (rdfClass != null) {
                retVal = Iterables.filter(rdfClass.getProperties(), new Predicate<RdfProperty>()
                {
                    @Override
                    public boolean apply(RdfProperty property)
                    {
                        return property.isPublic();
                    }
                });
            }
            else {
                Logger.error("Encountered unknown RDF class; " + resourceTypeCurie);
            }
        }

        //don't return null
        if (retVal == null) {
            retVal = Collections.emptySet();
        }

        return Response.ok(retVal).build();
    }

    @GET
    @Path("/properties/main")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RDF_PROPERTY_READ_ALL_PERM)
    public Response getMainProperty(@QueryParam(RDF_RES_TYPE_CURIE_PARAM) URI resourceTypeCurie) throws IOException
    {
        RdfProperty retVal = null;

        if (resourceTypeCurie != null) {
            RdfClass rdfClass = RdfFactory.getClass(resourceTypeCurie);
            if (rdfClass != null) {
                retVal = rdfClass.getMainProperty();
            }
        }

        //we can't return null, json expects "null" instead
        return Response.ok(retVal == null ? "null" : retVal).build();
    }

    @GET
    @Path("/resources/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RDF_RESOURCE_READ_ALL_PERM)
    //Note: the "query" parameter needs to be last, because the JS side just appends the query string to this URL
    public Response getResources(@QueryParam(RDF_RES_TYPE_CURIE_PARAM) URI resourceTypeCurie, @QueryParam(RDF_MAX_RESULTS_PARAM) int maxResults,
                                 @QueryParam(RDF_PREFIX_SEARCH_PARAM) @DefaultValue("true") boolean prefixSearch, /* keep this last */@QueryParam(RDF_QUERY_PARAM) String query) throws IOException
    {
        Collection<AutocompleteSuggestion> retVal = new ArrayList<>();

        //support a search-all-types-query when this is empty
        RdfClass resourceClassFilter = null;
        RdfQueryEndpoint endpoint = null;
        if (resourceTypeCurie != null && !StringUtils.isEmpty(resourceTypeCurie.toString())) {
            resourceClassFilter = RdfFactory.getClass(resourceTypeCurie);
            if (resourceClassFilter != null) {
                endpoint = resourceClassFilter.getEndpoint();
            }
        }
        else {
            endpoint = SEARCH_ALL_ENDPOINT;
        }

        if (endpoint != null) {
            RdfQueryEndpoint.QueryType queryType = RdfQueryEndpoint.QueryType.FULL;
            if (prefixSearch) {
                queryType = RdfQueryEndpoint.QueryType.STARTS_WITH;
            }

            retVal = endpoint.search(resourceClassFilter, query, queryType, R.i18n().getOptimalRefererLocale(), maxResults);
        }
        else {
            Logger.warn("Encountered unknown resource type; " + resourceTypeCurie);
        }

        return Response.ok(retVal).build();
    }

    @GET
    @Path("/resource/")
    @Produces(MediaType.APPLICATION_JSON)
    //We disabled this because some javascript initialization code needs to access it,
    // and I think it's not really a security issue. Let's hope I'm right...
    @RequiresPermissions(RDF_RESOURCE_READ_ALL_PERM)
    public Response getResource(@QueryParam(RDF_RES_TYPE_CURIE_PARAM) URI resourceTypeCurie, @QueryParam(RDF_RES_URI_PARAM) URI resourceUri) throws IOException
    {
        ResourceInfo retVal = null;

        RdfClass rdfClass = RdfFactory.getClass(resourceTypeCurie);
        if (rdfClass != null) {
            RdfQueryEndpoint endpoint = rdfClass.getEndpoint();
            if (endpoint != null) {
                retVal = endpoint.getResource(rdfClass, resourceUri, R.i18n().getOptimalRefererLocale());
            }
        }
        else {
            Logger.warn("Encountered unknown resource type; " + resourceTypeCurie);
        }

        if (retVal == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        else {
            return Response.ok(retVal).build();
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

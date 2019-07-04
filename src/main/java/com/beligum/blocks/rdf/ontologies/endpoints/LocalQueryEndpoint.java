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

package com.beligum.blocks.rdf.ontologies.endpoints;

import com.beligum.base.server.R;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.index.ifaces.*;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfEndpoint;
import com.beligum.blocks.rdf.ifaces.RdfOntologyMember;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.RDFS;
import com.beligum.blocks.utils.RdfTools;
import com.google.common.collect.Iterables;
import org.eclipse.rdf4j.model.Model;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by bram on 3/14/16.
 */
public class LocalQueryEndpoint implements RdfEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //Note: don't make this static; it messes with the RdfFactory initialization
    //Also: don't initialize it in the constructor; it suffers from the same problem
    private RdfProperty[] cachedLabelProps;

    //-----CONSTRUCTORS-----
    public LocalQueryEndpoint()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean isExternal()
    {
        return false;
    }
    @Override
    public Iterable<ResourceProxy> search(RdfOntologyMember resourceType, String query, QueryType queryType, Locale language, int maxResults) throws IOException
    {
        IndexSearchRequest mainQuery = IndexSearchRequest.createFor(StorageFactory.getJsonIndexer().connect());

        //let's support search-all-type queries when this is null
        if (resourceType != null && resourceType.isClass()) {
            mainQuery.filter((RdfClass) resourceType, IndexSearchRequest.FilterBoolean.AND);
        }

        // This will group of the resource URI, selecting the best matching language
        //only perform a 'wildcardSuffix' search,  meaning query[wildcard]
        mainQuery.filter(IndexSearchRequest.createFor(mainQuery.getIndexConnection())
                                           .filter(ResourceIndexEntry.tokenisedUriField, query, true, false, false, IndexSearchRequest.FilterBoolean.OR)
                                           .filter(ResourceIndexEntry.labelField, query, true, false, false, IndexSearchRequest.FilterBoolean.OR),
                         IndexSearchRequest.FilterBoolean.AND);

        // This will group on the resource URI, selecting the best matching language
        mainQuery.language(language, ResourceIndexEntry.resourceField);

        mainQuery.pageSize(maxResults);

        return Iterables.filter(mainQuery.getIndexConnection().search(mainQuery), ResourceProxy.class);
    }
    @Override
    public ResourceProxy getResource(RdfOntologyMember resourceType, URI resourceId, Locale language) throws IOException
    {
        //resources are indexed with relative id's, so make sure the URI is relative
        String relResourceIdStr = RdfTools.relativizeToLocalDomain(resourceId).toString();

        IndexConnection indexConn = StorageFactory.getJsonIndexer().connect();
        IndexSearchResult<ResourceIndexEntry> matchingPages = indexConn.search(IndexSearchRequest.createFor(indexConn)
                                                                                                 // This will group on the resource URI, selecting the best matching language
                                                                                                 .language(language, ResourceIndexEntry.resourceField)
                                                                                                 //at least one of the id or resource should match (or both)
                                                                                                 .filter(ResourceIndexEntry.uriField, relResourceIdStr, IndexSearchRequest.FilterBoolean.OR)
                                                                                                 .filter(PageIndexEntry.resourceField, relResourceIdStr, IndexSearchRequest.FilterBoolean.OR)
                                                                                                 .pageSize(1));

        return matchingPages.getTotalHits() > 0 ? matchingPages.iterator().next() : null;
    }
    @Override
    public RdfProperty[] getLabelCandidates(RdfClass localResourceType)
    {
        if (this.cachedLabelProps == null) {
            Set<RdfProperty> labels = new LinkedHashSet<>();
            labels.add(RDFS.label);
            labels.add(Settings.instance().getRdfLabelProperty());
            this.cachedLabelProps = labels.toArray(new RdfProperty[0]);
        }

        return this.cachedLabelProps;
    }
    @Override
    public URI getExternalResourceId(URI resourceId, Locale language)
    {
        //nothing special to redirect to; we'll render the resource out ourselves
        return null;
    }
    @Override
    public Model getExternalRdfModel(RdfClass resourceType, URI resourceId, Locale language) throws IOException
    {
        //we're a local endpoint
        return null;
    }
    @Override
    public RdfClass getExternalClasses(RdfClass localResourceType)
    {
        //we're a local endpoint
        return null;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
}

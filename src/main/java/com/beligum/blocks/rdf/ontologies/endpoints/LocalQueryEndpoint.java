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
        IndexSearchRequest mainQuery = IndexSearchRequest.createFor(StorageFactory.getJsonQueryConnection());

        //let's support search-all-type queries when this is null
        if (resourceType != null && resourceType.isClass()) {
            mainQuery.filter((RdfClass) resourceType, IndexSearchRequest.FilterBoolean.AND);
        }

        // This will group of the resource URI, selecting the best matching language
        mainQuery.language(language, ResourceIndexEntry.resourceField);

        IndexSearchRequest subQuery = IndexSearchRequest.createFor(mainQuery.getIndexConnection());
        subQuery.wildcard(ResourceIndexEntry.tokenisedUriField, query, IndexSearchRequest.FilterBoolean.OR);
        subQuery.wildcard(ResourceIndexEntry.labelField, query, IndexSearchRequest.FilterBoolean.OR);
        mainQuery.filter(subQuery, IndexSearchRequest.FilterBoolean.AND);

        mainQuery.maxResults(maxResults);

        return Iterables.filter(mainQuery.getIndexConnection().search(mainQuery), ResourceProxy.class);

//        /*
//         * Note that this is not the best way to do this: it should actually be implemented with the grouping functionality of Lucene
//         * so that the maxResults number is honoured. This implementation will received maxResults from Lucene and then filter it down,
//         * based on the grouping of the resource URI, selecting the best matching language, effectively narrowing the results to a number < maxResults
//         */
//        Map<URI, EntryWithIndex<PageIndexEntry>> langMapping = new LinkedHashMap<>();
//        for (ResourceIndexEntry entry : matchingPages) {
//            PageIndexEntry page = (PageIndexEntry) entry;
//
//            URI pageId = page.getUri();
//            URI resourceUri = URI.create(page.getResource());
//            EntryWithIndex<PageIndexEntry> selectedEntry = langMapping.get(resourceUri);
//
//            //this means we have a double result with a different language, so we need to select which language we want to return
//            // or else we'll be returning doubles, which is annoying
//            if (selectedEntry != null) {
//                //we give priority to the requested language, then the default language, then any other language
//                // since the value in the map is the 'selected' value, we check here if we can improve that value.
//                // If so, replace it with the better value.
//                int entryLangScore = PageIndexEntry.getLanguageScore(page, language);
//                int selectedLangScore = PageIndexEntry.getLanguageScore(selectedEntry.entry, language);
//                if (entryLangScore > selectedLangScore) {
//                    //replace the entry in the result list
//                    retVal.set(selectedEntry.index, new ResourceSuggestion(resourceUri, resourceTypeCurie, pageId, page.getLabel(), pageId.toString()));
//                    //replace the entry in the lang mapping
//                    langMapping.replace(resourceUri, new EntryWithIndex(page, selectedEntry.index));
//                }
//            }
//            else {
//                //Note: the ID of a page is also it's public address, but for the returned ID, we use the resource URI, which is the base ID that describes the 'concept' behind the page
//                retVal.add(new ResourceSuggestion(resourceUri, resourceTypeCurie, pageId, page.getLabel(), pageId.toString()));
//                langMapping.put(resourceUri, new EntryWithIndex(page, retVal.size() - 1));
//            }
//        }
    }
    @Override
    public ResourceProxy getResource(RdfOntologyMember resourceType, URI resourceId, Locale language) throws IOException
    {
        ResourceProxy retVal = null;

        //resources are indexed with relative id's, so make sure the URI is relative
        String relResourceIdStr = RdfTools.relativizeToLocalDomain(resourceId).toString();

        IndexConnection indexConn = StorageFactory.getJsonQueryConnection();
        IndexSearchResult matchingPages = indexConn.search(IndexSearchRequest.createFor(indexConn)
                                                                             // This will group of the resource URI, selecting the best matching language
                                                                             .language(language, ResourceIndexEntry.resourceField)
                                                                             //at least one of the id or resource should match (or both)
                                                                             .filter(ResourceIndexEntry.uriField, relResourceIdStr, IndexSearchRequest.FilterBoolean.OR)
                                                                             .filter(PageIndexEntry.resourceField, relResourceIdStr, IndexSearchRequest.FilterBoolean.OR)
                                                                             .maxResults(1));

        return PageIndexEntry.selectBestLanguage(matchingPages);

//        if (selectedEntry != null) {
//            //we just wrap the index extry in a resource info wrapper
//            retVal = new WrappedPageResourceInfo(selectedEntry);
//        }
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

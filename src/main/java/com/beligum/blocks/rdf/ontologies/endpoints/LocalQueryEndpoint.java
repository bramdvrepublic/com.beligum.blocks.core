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
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.index.ifaces.*;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfOntologyMember;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.RDFS;
import com.beligum.blocks.rdf.ontologies.local.ResourceSuggestion;
import com.beligum.blocks.rdf.ontologies.local.WrappedPageResourceInfo;
import com.beligum.blocks.utils.RdfTools;
import org.eclipse.rdf4j.model.Model;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by bram on 3/14/16.
 */
public class LocalQueryEndpoint implements RdfQueryEndpoint
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
    public Collection<AutocompleteSuggestion> search(RdfOntologyMember resourceType, String query, QueryType queryType, Locale language, int maxResults) throws IOException
    {
        List<AutocompleteSuggestion> retVal = new ArrayList<>();

        PageIndexConnection mainIndexer = StorageFactory.getJsonQueryConnection();

        IndexSearchRequest mainQuery = IndexSearchRequest.createFor(mainIndexer);

        //let's support search-all-type queries when this is null
        if (resourceType != null && resourceType.isClass()) {
            mainQuery.filter((RdfClass) resourceType, IndexSearchRequest.FilterBoolean.AND);
        }

        IndexSearchRequest subQuery = IndexSearchRequest.createFor(mainIndexer);
        subQuery.wildcard(IndexEntry.tokenisedIdField, query, IndexSearchRequest.FilterBoolean.OR);
        subQuery.wildcard(IndexEntry.labelField, query, IndexSearchRequest.FilterBoolean.OR);
        mainQuery.filter(subQuery, IndexSearchRequest.FilterBoolean.AND);

        mainQuery.maxResults(maxResults);

        //See https://lucene.apache.org/core/5_4_1/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package_description
        //#typeOf:mot:Person #(tokenisedId:bra* title:bra*)
        //#typeOf:mot:Person #(-language:en language:nl) #(-language:nl language:en) #(tokenisedId:bram* title:bram*)
        //        StringBuilder luceneQuery = new StringBuilder();
        //        luceneQuery/*.append("#")*/.append(PageIndexEntry.typeOf.name()).append(":").append(QueryParser.escape(resourceType.getCurieName().toString()))/*.append("\"")*/;

        IndexSearchResult matchingPages = mainIndexer.search(mainQuery);

        /*
         * Note that this is not the best way to do this: it should actually be implemented with the grouping functionality of Lucene
         * so that the maxResults number is honoured. This implementation will received maxResults from Lucene and then filter it down,
         * based on the grouping of the resource URI, selecting the best matching language, effectively narrowing the results to a number < maxResults
         */
        Map<URI, EntryWithIndex<PageIndexEntry>> langMapping = new LinkedHashMap<>();
        for (IndexEntry entry : matchingPages) {
            PageIndexEntry page = (PageIndexEntry) entry;

            URI pageId = URI.create(page.getId());
            URI resourceUri = URI.create(page.getResource());
            EntryWithIndex<PageIndexEntry> selectedEntry = langMapping.get(resourceUri);
            //small optimization to re-use the passed resourceType if we have it so we don't need to parse the string
            URI resourceTypeCurie = resourceType == null ? URI.create(page.getTypeOf()) : resourceType.getCurieName();
            //this means we have a double result with a different language, so we need to select which language we want to return
            // or else we'll be returning doubles, which is annoying
            if (selectedEntry != null) {
                //we give priority to the requested language, then the default language, then any other language
                // since the value in the map is the 'selected' value, we check here if we can improve that value.
                // If so, replace it with the better value.
                int entryLangScore = PageIndexEntry.getLanguageScore(page, language);
                int selectedLangScore = PageIndexEntry.getLanguageScore(selectedEntry.entry, language);
                if (entryLangScore > selectedLangScore) {
                    //replace the entry in the result list
                    retVal.set(selectedEntry.index, new ResourceSuggestion(resourceUri, resourceTypeCurie, pageId, page.getLabel(), pageId.toString()));
                    //replace the entry in the lang mapping
                    langMapping.replace(resourceUri, new EntryWithIndex(page, selectedEntry.index));
                }
            }
            else {
                //Note: the ID of a page is also it's public address, but for the returned ID, we use the resource URI, which is the base ID that describes the 'concept' behind the page
                retVal.add(new ResourceSuggestion(resourceUri, resourceTypeCurie, pageId, page.getLabel(), pageId.toString()));
                langMapping.put(resourceUri, new EntryWithIndex(page, retVal.size() - 1));
            }
        }

        return retVal;
    }
    @Override
    public ResourceInfo getResource(RdfOntologyMember resourceType, URI resourceId, Locale language) throws IOException
    {
        ResourceInfo retVal = null;

        //resources are indexed with relative id's, so make sure the URI is relative
        String relResourceIdStr = RdfTools.relativizeToLocalDomain(resourceId).toString();

        //we'll wrap this one in a ResourceInfo
        PageIndexEntry selectedEntry = null;

        if (selectedEntry == null) {
            PageIndexConnection indexConn = StorageFactory.getJsonQueryConnection();
            IndexSearchResult matchingPages = indexConn.search(IndexSearchRequest.createFor(indexConn)
                                                                                 //at least one of the id or resource should match (or both)
                                                                                 .filter(IndexEntry.idField, relResourceIdStr, IndexSearchRequest.FilterBoolean.OR)
                                                                                 .filter(PageIndexEntry.resourceField, relResourceIdStr, IndexSearchRequest.FilterBoolean.OR)
                                                                                 .maxResults(R.configuration().getLanguages().size()));
            selectedEntry = PageIndexEntry.selectBestForLanguage(matchingPages, language);
        }

        if (selectedEntry != null) {
            //we just wrap the index extry in a resource info wrapper
            retVal = new WrappedPageResourceInfo(selectedEntry);
        }

        return retVal;
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
    private class EntryWithIndex<T>
    {
        public T entry;
        public int index;
        public EntryWithIndex(T entry, int index)
        {
            this.entry = entry;
            this.index = index;
        }
    }
}

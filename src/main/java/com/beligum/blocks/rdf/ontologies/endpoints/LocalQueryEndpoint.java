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
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.filesystem.index.entries.IndexEntry;
import com.beligum.blocks.filesystem.index.entries.pages.IndexSearchResult;
import com.beligum.blocks.filesystem.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.local.factories.Terms;
import com.beligum.blocks.rdf.ontologies.RDFS;
import com.beligum.blocks.rdf.ontologies.local.ResourceSuggestion;
import com.beligum.blocks.rdf.ontologies.local.WrappedPageResourceInfo;
import com.beligum.blocks.utils.RdfTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
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
    public static class IntermediateResourcesOption implements SearchOption
    {
        private Map<String, Map<String, PageIndexEntry>> store;

        public IntermediateResourcesOption()
        {
            this.store = new LinkedHashMap<>();
        }

        public void add(PageIndexEntry indexEntry) throws IOException
        {
            Map<String, PageIndexEntry> storedVariants = this.store.get(indexEntry.getResource());
            if (storedVariants == null) {
                this.store.put(indexEntry.getResource(), storedVariants = new HashMap<>());
            }

            if (storedVariants.containsKey(indexEntry.getLanguage())) {
                throw new IOException("Overwriting intermediate resource for language " + indexEntry.getLanguage() + ", can't continue because this is probably an error; " + indexEntry.getResource());
            }
            else {
                storedVariants.put(indexEntry.getLanguage(), indexEntry);
            }
        }

        public PageIndexEntry get(String relativeResourceUri, String language)
        {
            PageIndexEntry retVal = null;

            Map<String, PageIndexEntry> storedResourceVariants = this.store.get(relativeResourceUri);
            if (storedResourceVariants != null) {
                retVal = storedResourceVariants.get(language);
            }

            return retVal;
        }
    }

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
    public Collection<AutocompleteSuggestion> search(RdfClass resourceType, String query, QueryType queryType, Locale language, int maxResults, SearchOption... options) throws IOException
    {
        List<AutocompleteSuggestion> retVal = new ArrayList<>();

        LuceneQueryConnection mainIndexer = StorageFactory.getMainPageQueryConnection();

        BooleanQuery mainQuery = new BooleanQuery();

        //let's support search-all-type queries when this is null
        if (resourceType != null) {
            mainQuery.add(new TermQuery(new Term(PageIndexEntry.Field.typeOf.name(), resourceType.getCurieName().toString())), BooleanClause.Occur.FILTER);
        }

        BooleanQuery subQuery = new BooleanQuery();
        subQuery.add(mainIndexer.buildWildcardQuery(IndexEntry.Field.tokenisedId.name(), query), BooleanClause.Occur.SHOULD);
        subQuery.add(mainIndexer.buildWildcardQuery(IndexEntry.Field.title.name(), query), BooleanClause.Occur.SHOULD);
        mainQuery.add(subQuery, BooleanClause.Occur.FILTER);

        //See https://lucene.apache.org/core/5_4_1/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package_description
        //#typeOf:mot:Person #(tokenisedId:bra* title:bra*)
        //#typeOf:mot:Person #(-language:en language:nl) #(-language:nl language:en) #(tokenisedId:bram* title:bram*)
        //        StringBuilder luceneQuery = new StringBuilder();
        //        luceneQuery/*.append("#")*/.append(PageIndexEntry.Field.typeOf.name()).append(":").append(QueryParser.escape(resourceType.getCurieName().toString()))/*.append("\"")*/;

        IndexSearchResult matchingPages = mainIndexer.search(mainQuery, maxResults);

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
                    retVal.set(selectedEntry.index, new ResourceSuggestion(resourceUri, resourceTypeCurie, pageId, page.getTitle(), pageId.toString()));
                    //replace the entry in the lang mapping
                    langMapping.replace(resourceUri, new EntryWithIndex(page, selectedEntry.index));
                }
            }
            else {
                //Note: the ID of a page is also it's public address, but for the returned ID, we use the resource URI, which is the base ID that describes the 'concept' behind the page
                retVal.add(new ResourceSuggestion(resourceUri, resourceTypeCurie, pageId, page.getTitle(), pageId.toString()));
                langMapping.put(resourceUri, new EntryWithIndex(page, retVal.size() - 1));
            }
        }

        return retVal;
    }
    @Override
    public ResourceInfo getResource(RdfClass resourceType, URI resourceId, Locale language, SearchOption... options) throws IOException
    {
        ResourceInfo retVal = null;

        //resources are indexed with relative id's, so make sure the URI is relative
        String relResourceIdStr = RdfTools.relativizeToLocalDomain(resourceId).toString();

        //we'll wrap this one in a ResourceInfo
        PageIndexEntry selectedEntry = null;

        //this is a bit of a hack and needs some explaining: while indexing a resource with sub-resources
        //(eg. a Page containing an Object widget), we index the sub-resources before the page-resource,
        //but when this method is called to lookup the sub-resources in that page, they're not committed to
        //the index yet, that's why we created a temporary store for them.
        //See LucenePageIndexConnection.update()
        //Update: note that this needs to be checked first because it's possible we're re-saving (updating) an existing sub-resource
        //that's already present in the index (with the old values), resulting in a valid hit. This means, if the options contain
        //a hit for this resourceId, we should expect it's in a more updated state and return that instead of the indexed one.
        IntermediateResourcesOption intermediateResourcesOption = null;

        if (options != null) {
            for (SearchOption option : options) {
                //for now, this is the only option we support here
                if (option instanceof IntermediateResourcesOption) {
                    intermediateResourcesOption = (IntermediateResourcesOption) option;
                    break;
                }
                else {
                    throw new IOException("Encountered unsupported/unimplemented option, this shouldn't happen; " + option);
                }
            }
        }

        if (intermediateResourcesOption != null) {
            selectedEntry = intermediateResourcesOption.get(relResourceIdStr, language.getLanguage());
        }

        if (selectedEntry == null) {
            org.apache.lucene.search.BooleanQuery pageQuery = new org.apache.lucene.search.BooleanQuery();
            //at least one of the id or resource should match (or both)
            pageQuery.add(new TermQuery(new Term(IndexEntry.Field.id.name(), relResourceIdStr)), BooleanClause.Occur.SHOULD);
            pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.resource.name(), relResourceIdStr)), BooleanClause.Occur.SHOULD);

            IndexSearchResult matchingPages = StorageFactory.getMainPageQueryConnection().search(pageQuery, R.configuration().getLanguages().size());
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
            this.cachedLabelProps = new RdfProperty[] { RDFS.LABEL, Terms.title };
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

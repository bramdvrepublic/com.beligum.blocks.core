package com.beligum.blocks.rdf.ontology.vocabularies.endpoints;

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
import com.beligum.blocks.rdf.ontology.factories.Terms;
import com.beligum.blocks.rdf.ontology.vocabularies.RDFS;
import com.beligum.blocks.rdf.ontology.vocabularies.local.ResourceSuggestion;
import com.beligum.blocks.rdf.ontology.vocabularies.local.WrappedPageResourceInfo;
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
public class SettingsQueryEndpoint implements RdfQueryEndpoint
{
    //-----CONSTANTS-----
    //Note: don't make this static; it messes with the RdfFactory initialization
    private final RdfProperty[] LABEL_PROPS;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public SettingsQueryEndpoint()
    {
        this.LABEL_PROPS = new RdfProperty[] { RDFS.LABEL, Terms.title };
    }

    @Override
    public boolean isExternal()
    {
        return false;
    }
    //-----PUBLIC METHODS-----
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
        boolean complexWildcard = queryType == QueryType.FULL ? false : true;
        subQuery.add(mainIndexer.buildWildcardQuery(IndexEntry.Field.tokenisedId.name(), query, complexWildcard), BooleanClause.Occur.SHOULD);
        subQuery.add(mainIndexer.buildWildcardQuery(IndexEntry.Field.title.name(), query, true), BooleanClause.Occur.SHOULD);
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
        for (IndexEntry entry : matchingPages.getResults()) {
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
    public ResourceInfo getResource(RdfClass resourceType, URI resourceId, Locale language) throws IOException
    {
        ResourceInfo retVal = null;

        //resources are indexed with relative id's, so make sure the URI is relative
        String resourceIdStr = RdfTools.relativizeToLocalDomain(resourceId).toString();

        org.apache.lucene.search.BooleanQuery pageQuery = new org.apache.lucene.search.BooleanQuery();
        pageQuery.add(new TermQuery(new Term(IndexEntry.Field.id.name(), resourceIdStr)), BooleanClause.Occur.SHOULD);
        pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.resource.name(), resourceIdStr)), BooleanClause.Occur.SHOULD);

        IndexSearchResult matchingPages = StorageFactory.getMainPageQueryConnection().search(pageQuery, R.configuration().getLanguages().size());
        PageIndexEntry selectedEntry = PageIndexEntry.selectBestForLanguage(matchingPages, language);
        if (selectedEntry != null) {
            //we just wrap the index extry in a resource info wrapper
            retVal = new WrappedPageResourceInfo(selectedEntry);
        }

        return retVal;
    }
    @Override
    public RdfProperty[] getLabelCandidates(RdfClass localResourceType)
    {
        return LABEL_PROPS;
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

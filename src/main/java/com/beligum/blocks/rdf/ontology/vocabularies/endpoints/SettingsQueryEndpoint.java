package com.beligum.blocks.rdf.ontology.vocabularies.endpoints;

import com.beligum.base.server.R;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ontology.vocabularies.local.ResourceSuggestion;
import com.beligum.blocks.rdf.ontology.vocabularies.local.WrappedPageResourceInfo;
import org.apache.lucene.search.BooleanClause;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by bram on 3/14/16.
 */
public class SettingsQueryEndpoint implements RdfQueryEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public SettingsQueryEndpoint()
    {

    }

    //-----PUBLIC METHODS-----
    @Override
    public List<AutocompleteSuggestion> search(RdfClass resourceType, String query, QueryType queryType, Locale language, int maxResults, SearchOption... options) throws IOException
    {
        List<AutocompleteSuggestion> retVal = new ArrayList<>();

        LuceneQueryConnection.FieldQuery.Type fieldQueryType = null;
        switch (queryType) {
            case STARTS_WITH:
            case NAME:
                fieldQueryType = LuceneQueryConnection.FieldQuery.Type.WILDCARD_COMPLEX;
                break;
            case FULL:
                fieldQueryType = LuceneQueryConnection.FieldQuery.Type.WILDCARD;
                break;
            default:
                throw new IOException("Unsupported or unimplemented query type encountered, can't proceed; "+queryType);
        }

        LuceneQueryConnection.FieldQuery[] queries =
                        new LuceneQueryConnection.FieldQuery[] { new LuceneQueryConnection.FieldQuery(PageIndexEntry.Field.typeOf, resourceType.getCurieName().toString(), BooleanClause.Occur.FILTER, LuceneQueryConnection.FieldQuery.Type.EXACT),
                                                                 new LuceneQueryConnection.FieldQuery(IndexEntry.Field.tokenisedId, query, BooleanClause.Occur.SHOULD, fieldQueryType, 1),
                                                                 new LuceneQueryConnection.FieldQuery(IndexEntry.Field.title, query, BooleanClause.Occur.SHOULD, LuceneQueryConnection.FieldQuery.Type.WILDCARD_COMPLEX, 1)
                        };

        //See https://lucene.apache.org/core/5_4_1/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package_description
        //#typeOf:mot:Person #(tokenisedId:bra* title:bra*)
        //#typeOf:mot:Person #(-language:en language:nl) #(-language:nl language:en) #(tokenisedId:bram* title:bram*)
        //        StringBuilder luceneQuery = new StringBuilder();
        //        luceneQuery/*.append("#")*/.append(PageIndexEntry.Field.typeOf.name()).append(":").append(QueryParser.escape(resourceType.getCurieName().toString()))/*.append("\"")*/;

        List<PageIndexEntry> matchingPages = StorageFactory.getMainPageQueryConnection().search(queries, maxResults);

        /*
         * Note that this is not the best way to do this: it should actually be implemented with the grouping functionality of Lucene
         * so that the maxResults number is honoured. This implementation will received maxResults from Lucene and then filter it down,
         * based on the grouping of the resource URI, selecting the best matching language, effectively narrowing the results to a number < maxResults
         */
        Map<URI, EntryWithIndex<PageIndexEntry>> langMapping = new LinkedHashMap<>();
        for (PageIndexEntry page : matchingPages) {

            URI resourceUri = page.getResource();
            EntryWithIndex<PageIndexEntry> selectedEntry = langMapping.get(resourceUri);
            //this means we have a double result with a different language, so we need to select which language we want to return
            // or else we'll be returning doubles, which is annoying
            if (selectedEntry != null) {
                //we give priority to the requested language, then the default language, then any other language
                // since the value in the map is the 'selected' value, we check here if we can improve that value.
                // If so, replace it with the better value.
                int entryLangScore = this.getLanguageScore(page, language);
                int selectedLangScore = this.getLanguageScore(selectedEntry.entry, language);
                if (entryLangScore > selectedLangScore) {
                    //replace the entry in the result list
                    retVal.set(selectedEntry.index, new ResourceSuggestion(resourceUri, resourceType.getCurieName(), page.getTitle(), page.getId().getPath()));
                    //replace the entry in the lang mapping
                    langMapping.replace(resourceUri, new EntryWithIndex(page, selectedEntry.index));
                }
            }
            else {
                //Note: the ID of a page is also it's public address, but for the returned ID, we use the resource URI, which is the base ID that describes the 'concept' behind the page
                retVal.add(new ResourceSuggestion(resourceUri, resourceType.getCurieName(), page.getTitle(), page.getId().getPath()));
                langMapping.put(resourceUri, new EntryWithIndex(page, retVal.size() - 1));
            }
        }

        return retVal;
    }
    @Override
    public ResourceInfo getResource(RdfClass resourceType, URI resourceId, Locale language) throws IOException
    {
        ResourceInfo retVal = null;

        String resourceIdStr = resourceId.toString();
        LuceneQueryConnection.FieldQuery[] queries =
                        new LuceneQueryConnection.FieldQuery[] {
                                        new LuceneQueryConnection.FieldQuery(IndexEntry.Field.id, resourceIdStr, BooleanClause.Occur.SHOULD, LuceneQueryConnection.FieldQuery.Type.EXACT),
                                        new LuceneQueryConnection.FieldQuery(PageIndexEntry.Field.resource, resourceIdStr, BooleanClause.Occur.SHOULD, LuceneQueryConnection.FieldQuery.Type.EXACT)
                        };

        List<PageIndexEntry> matchingPages = StorageFactory.getMainPageQueryConnection().search(queries, R.configuration().getLanguages().size());
        if (!matchingPages.isEmpty()) {
            PageIndexEntry selectedEntry = null;
            for (PageIndexEntry entry : matchingPages) {
                if (selectedEntry==null) {
                    selectedEntry = entry;
                }
                else {
                    int entryLangScore = this.getLanguageScore(entry, language);
                    int selectedLangScore = this.getLanguageScore(selectedEntry, language);
                    if (entryLangScore > selectedLangScore) {
                        selectedEntry = entry;
                    }
                }
            }

            //we just wrap the index extry in a resource info wrapper
            retVal = new WrappedPageResourceInfo(selectedEntry);
        }

        return retVal;
    }
    @Override
    public URI getExternalResourceRedirect(URI resourceId, Locale language)
    {
        //nothing special to redirect to; we'll render the resource out ourselves
        return null;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * This will return an int value according to the language of the entry;
     * 1) entry language = no special language
     * 2) entry language = default language
     * 3) entry language = requested language
     */
    private int getLanguageScore(PageIndexEntry entry, Locale requestLanguage)
    {
        int retVal = 1;

        if (entry.getLanguage().equals(requestLanguage)) {
            retVal = 3;
        }
        else if (entry.getLanguage().equals(R.configuration().getDefaultLanguage())) {
            retVal = 2;
        }

        return retVal;
    }

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

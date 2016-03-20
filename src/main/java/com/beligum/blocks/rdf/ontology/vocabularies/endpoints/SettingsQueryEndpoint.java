package com.beligum.blocks.rdf.ontology.vocabularies.endpoints;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.AutocompleteValue;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ontology.vocabularies.local.DefaultResourceValue;
import com.beligum.blocks.rdf.ontology.vocabularies.local.ResourceSuggestion;
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
    public List<AutocompleteSuggestion> search(RdfClass resourceType, String query, Locale language, int maxResults) throws IOException
    {
        List<AutocompleteSuggestion> retVal = new ArrayList<>();

        PageIndexConnection.FieldQuery[] queries =
                        new PageIndexConnection.FieldQuery[] { new PageIndexConnection.FieldQuery(PageIndexEntry.Field.typeOf, resourceType.getCurieName().toString(), BooleanClause.Occur.FILTER,
                                                                                                  PageIndexConnection.FieldQuery.Type.EXACT),
                                                               new PageIndexConnection.FieldQuery(IndexEntry.Field.tokenisedId, query, BooleanClause.Occur.SHOULD,
                                                                                                  PageIndexConnection.FieldQuery.Type.WILDCARD_COMPLEX, 1),
                                                               new PageIndexConnection.FieldQuery(PageIndexEntry.Field.title, query, BooleanClause.Occur.SHOULD,
                                                                                                  PageIndexConnection.FieldQuery.Type.WILDCARD_COMPLEX, 1) };

        //See https://lucene.apache.org/core/5_4_1/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package_description
        //#typeOf:mot:Person #(tokenisedId:bra* title:bra*)
        //#typeOf:mot:Person #(-language:en language:nl) #(-language:nl language:en) #(tokenisedId:bram* title:bram*)
        //        StringBuilder luceneQuery = new StringBuilder();
        //        luceneQuery/*.append("#")*/.append(PageIndexEntry.Field.typeOf.name()).append(":").append(QueryParser.escape(resourceType.getCurieName().toString()))/*.append("\"")*/;

        List<PageIndexEntry> matchingPages = StorageFactory.getMainPageIndexer().connect().search(queries, maxResults);

        /*
         * Note that this is not the best way to do this: it should actually be implemented with the grouping functionality of Lucene
         * so that the maxResults number is honoured. This implementation will received maxResults from Lucene and then filter it down,
         * based on the grouping of the resource URI, selecting the best matching language, effectively narrowing the results to a number < maxResults
         */
        Map<URI, EntryWithIndex<PageIndexEntry>> langMapping = new LinkedHashMap<>();
        for (PageIndexEntry page : matchingPages) {

            URI resourceUri = URI.create(page.getResource());
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
    public AutocompleteValue getResource(RdfClass resourceType, URI resourceId, Locale language) throws IOException
    {
        AutocompleteValue retVal = null;

        PageIndexConnection.FieldQuery[] queries =
                        new PageIndexConnection.FieldQuery[] {
                                        new PageIndexConnection.FieldQuery(PageIndexEntry.Field.resource, resourceId.toString(), BooleanClause.Occur.MUST, PageIndexConnection.FieldQuery.Type.EXACT)
                        };

        List<PageIndexEntry> matchingPages = StorageFactory.getMainPageIndexer().connect().search(queries, 1);
        if (!matchingPages.isEmpty()) {
            PageIndexEntry entry = matchingPages.iterator().next();
            //note: the ID of a page is the public URL
            retVal = new DefaultResourceValue(URI.create(entry.getResource()), resourceType.getCurieName(), entry.getTitle(), entry.getId(), null, entry.getTitle());
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

        if (entry.getLanguage().equals(requestLanguage.getLanguage())) {
            retVal = 3;
        }
        else if (entry.getLanguage().equals(Settings.instance().getDefaultLanguage().getLanguage())) {
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

package com.beligum.blocks.rdf.ontology.vocabularies.endpoints;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by bram on 3/14/16.
 */
public class SettingsQueryEndpoint implements RdfQueryEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public List<AutocompleteSuggestion> search(RdfClass resourceType, String query, Locale language, int maxResults) throws IOException
    {
        List<AutocompleteSuggestion> retVal = new ArrayList<>();

        PageIndexConnection.FieldQuery[] queries =
                        new PageIndexConnection.FieldQuery[] { new PageIndexConnection.FieldQuery(PageIndexEntry.Field.typeOf, resourceType.getCurieName().toString(), BooleanClause.Occur.FILTER,
                                                                                                  PageIndexConnection.FieldQuery.Type.EXACT),
                                                               new PageIndexConnection.FieldQuery(IndexEntry.Field.tokenisedId, query, BooleanClause.Occur.SHOULD,
                                                                                                  PageIndexConnection.FieldQuery.Type.WILDCARD, 1),
                                                               new PageIndexConnection.FieldQuery(PageIndexEntry.Field.title, query, BooleanClause.Occur.SHOULD,
                                                                                                  PageIndexConnection.FieldQuery.Type.WILDCARD, 1) };

        List<PageIndexEntry> matchingPages = StorageFactory.getMainPageIndexer().connect().search(queries, maxResults);
        //TODO this iterates and re-packages the results yet another time -> avoid this
        for (PageIndexEntry page : matchingPages) {
            //Note: the ID of a page is also it's public address
            retVal.add(new ResourceSuggestion(page.getId(), resourceType.getCurieName(), page.getTitle(), page.getId().getPath()));
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
            retVal = new DefaultResourceValue(URI.create(entry.getResource()), resourceType.getCurieName(), entry.getTitle(), entry.getId(), entry.getTitle());
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

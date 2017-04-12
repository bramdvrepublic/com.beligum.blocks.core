package com.beligum.blocks.filesystem.index.ifaces;

import com.beligum.blocks.filesystem.index.entries.pages.IndexSearchResult;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.apache.lucene.search.Query;

import java.io.IOException;

/**
 * Created by bram on 4/7/16.
 */
public interface LuceneQueryConnection extends QueryConnection
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Search for the low-level lucene query, returning the maxResults best results
     */
    IndexSearchResult search(Query luceneQuery, RdfProperty sortField, boolean sortReversed, int pageSize, int pageOffset) throws IOException;

    /**
     * Convenience method for the one above
     */
    IndexSearchResult search(Query luceneQuery, int maxResults) throws IOException;

    /**
     * Builds a wildcard phrase query for a user-input text, taking care of all special modalities internal to the above search() methods
     */
    Query buildWildcardQuery(String fieldName, String queryPhrase, boolean complex) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

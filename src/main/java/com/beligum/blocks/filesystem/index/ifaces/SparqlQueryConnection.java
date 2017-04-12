package com.beligum.blocks.filesystem.index.ifaces;

import com.beligum.blocks.filesystem.index.entries.pages.IndexSearchResult;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.openrdf.query.TupleQuery;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Created by bram on 4/7/16.
 */
public interface SparqlQueryConnection extends QueryConnection
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * Search the triple store with the specified parameters
     *
     * @param type the type of resource you're looking for or null to look for any type
     * @param luceneQuery the lucene query to query for (ignored if empty or null)
     * @param fieldValues the name of the fields and their value-mapping to filter by
     * @param sortField the field to sort on
     * @param sortAscending whether to sort ascending or descending
     * @param pageSize the number of results to return
     * @param pageOffset the offset (in pages, not in results, so the real offset will be pageOffset*pageSize) where to start returning results
     * @param language the language you're interested in or null to return all languages
     * @return the result list
     * @throws IOException
     */
    IndexSearchResult search(RdfClass type, String luceneQuery, Map<RdfProperty, String> fieldValues, RdfProperty sortField, boolean sortAscending, int pageSize, int pageOffset, Locale language) throws IOException;

    /**
     * Search the triple store with the specified raw Sparql query
     */
    IndexSearchResult search(String sparqlQuery, Locale language) throws IOException;

    /**
     * Build a low-level query object from the supplied SPARQL query
     */
    TupleQuery query(String sparqlQuery) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

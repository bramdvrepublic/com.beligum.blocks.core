package com.beligum.blocks.fs.index.ifaces;

import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.fs.index.entries.resources.ResourceIndexEntry;
import com.beligum.blocks.rdf.ifaces.RdfClass;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by bram on 4/7/16.
 */
public interface SparqlQueryConnection<T extends PageIndexEntry> extends QueryConnection<T>
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
     * @param fieldValues the name (in the default local namespace, eg. "isVerified") of the fields and their value-mapping to filter by
     * @param sortField the field (in the default local namespace, eg. "isVerified") to sort on
     * @param sortAscending whether to sort ascending or descending
     * @param pageSize the number of results to return
     * @param pageOffset the offset (in pages, not in results, so the real offset will be pageOffset*pageSize) where to start returning results
     * @param language the language you're interested in or null to return all languages
     * @return the result list
     * @throws IOException
     */
    <T extends ResourceIndexEntry> List<T> search(RdfClass type, String luceneQuery, Map<String, String> fieldValues, String sortField, boolean sortAscending, int pageSize, int pageOffset, Locale language) throws IOException;

    /**
     * Search the triple store with the specified raw Sparql query
     */
    <T extends ResourceIndexEntry> List<T> search(String sparqlQuery, RdfClass type, Locale language) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

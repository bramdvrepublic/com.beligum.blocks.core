package com.beligum.blocks.fs.index.ifaces;

import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;

import java.io.IOException;
import java.util.List;
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
     */
    List<T> search(String luceneQuery, Map<String, String> fieldValues, String sortField, boolean sortAscending, int pageSize, int pageOffset) throws IOException;

    /**
     * Search the triple store with the specified raw Sparql query
     */
    List<T> search(String sparqlQuery) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.fs.index.entries.pages;

import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.util.List;
import java.util.Map;

/**
 * Created by bram on 6/13/16.
 */
public class IndexSearchRequest
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String searchTerm;
    private Map<RdfProperty, List<String>> fieldFilters;
    private RdfProperty sortField;

    /**
     * The format in which the results are presented: a list, ordered by first letter, ...
     */
    private String resultsFormat;

    //-----CONSTRUCTORS-----
    public IndexSearchRequest(String searchTerm, Map<RdfProperty, List<String>> fieldFilters, RdfProperty sortField, String resultsFormat)
    {
        this.searchTerm = searchTerm;
        this.fieldFilters = fieldFilters;
        this.sortField = sortField;
        this.resultsFormat = resultsFormat;
    }

    //-----PUBLIC METHODS-----
    public String getSearchTerm()
    {
        return searchTerm;
    }
    public Map<RdfProperty, List<String>> getFieldFilters()
    {
        return fieldFilters;
    }
    public RdfProperty getSortField()
    {
        return sortField;
    }
    public String getResultsFormat()
    {
        return resultsFormat;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

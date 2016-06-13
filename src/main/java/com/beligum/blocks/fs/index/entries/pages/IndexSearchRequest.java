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

    //-----CONSTRUCTORS-----
    public IndexSearchRequest(String searchTerm, Map<RdfProperty, List<String>> fieldFilters, RdfProperty sortField)
    {
        this.searchTerm = searchTerm;
        this.fieldFilters = fieldFilters;
        this.sortField = sortField;
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
    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

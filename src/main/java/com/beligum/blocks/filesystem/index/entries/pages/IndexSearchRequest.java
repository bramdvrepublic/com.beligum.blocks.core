package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.util.List;

/**
 * Created by bram on 6/13/16.
 */
public class IndexSearchRequest
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String searchTerm;
    private List<String> fieldFilters;
    private RdfProperty sortField;
    private Integer pageIndex;
    private Integer pageSize;
    private RdfClass typeOf;
    private String format;

    //-----CONSTRUCTORS-----
    public IndexSearchRequest()
    {
    }

    //-----PUBLIC METHODS-----
    public String getSearchTerm()
    {
        return searchTerm;
    }
    public void setSearchTerm(String searchTerm)
    {
        this.searchTerm = searchTerm;
    }
    public List<String> getFieldFilters()
    {
        return fieldFilters;
    }
    public void setFieldFilters(List<String> fieldFilters)
    {
        this.fieldFilters = fieldFilters;
    }
    public RdfProperty getSortField()
    {
        return sortField;
    }
    public void setSortField(RdfProperty sortField)
    {
        this.sortField = sortField;
    }
    public Integer getPageIndex()
    {
        return pageIndex;
    }
    public void setPageIndex(Integer pageIndex)
    {
        this.pageIndex = pageIndex;
    }
    public Integer getPageSize()
    {
        return pageSize;
    }
    public void setPageSize(Integer pageSize)
    {
        this.pageSize = pageSize;
    }
    public RdfClass getTypeOf()
    {
        return typeOf;
    }
    public void setTypeOf(RdfClass typeOf)
    {
        this.typeOf = typeOf;
    }
    public String getFormat()
    {
        return format;
    }
    public void setFormat(String format)
    {
        this.format = format;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

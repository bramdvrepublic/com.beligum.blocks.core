package com.beligum.blocks.index.request;

import com.beligum.blocks.index.fields.JsonField;
import com.beligum.blocks.index.ifaces.IndexConnection;
import com.beligum.blocks.index.ifaces.IndexEntryField;
import com.beligum.blocks.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.io.IOException;
import java.util.*;

public abstract class AbstractIndexSearchRequest implements IndexSearchRequest
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final IndexConnection indexConnection;
    protected Map<String, Boolean> sortFields;
    protected int pageSize = DEFAULT_PAGE_SIZE;
    protected int pageOffset = DEFAULT_PAGE_OFFSET;
    protected Locale language;
    protected LanguageFilterType languageFilterType;
    protected IndexEntryField languageGroupField;

    //-----CONSTRUCTORS-----
    protected AbstractIndexSearchRequest(IndexConnection indexConnection)
    {
        this.indexConnection = indexConnection;
        this.sortFields = new LinkedHashMap<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public IndexConnection getIndexConnection()
    {
        return indexConnection;
    }
    @Override
    public int getPageSize()
    {
        return pageSize;
    }
    @Override
    public int getPageOffset()
    {
        return pageOffset;
    }
    @Override
    public Locale getLanguage()
    {
        return language;
    }

    //-----BUILDER METHODS-----
    @Override
    public IndexSearchRequest query(String value, FilterBoolean filterBoolean)
    {
        return this.query(value, false, filterBoolean);
    }
    @Override
    public IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        return this.filter(field, value, false, filterBoolean);
    }
    @Override
    public IndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean)
    {
        return this.filter(property, value, false, filterBoolean);
    }
    @Override
    public IndexSearchRequest all(String value, FilterBoolean filterBoolean)
    {
        return this.all(value, false, filterBoolean);
    }
    @Override
    public IndexSearchRequest sort(RdfProperty property, boolean sortAscending)
    {
        // this allows us to always implement the sort, even if there's no value
        if (property != null) {
            this.sortFields.put(this.nameOf(property), sortAscending);
        }

        return this;
    }
    @Override
    public IndexSearchRequest sort(IndexEntryField field, boolean sortAscending)
    {
        // this allows us to always implement the sort, even if there's no value
        if (field != null) {
            this.sortFields.put(field.getName(), sortAscending);
        }

        return this;
    }
    @Override
    public IndexSearchRequest pageSize(int pageSize)
    {
        this.pageSize = pageSize;

        return this;
    }
    @Override
    public IndexSearchRequest pageOffset(int pageOffset)
    {
        this.pageOffset = pageOffset;

        return this;
    }
    @Override
    public IndexSearchRequest language(Locale language)
    {
        this.language = language;
        this.languageFilterType = LanguageFilterType.STRICT;

        return this;
    }
    @Override
    public IndexSearchRequest language(Locale language, IndexEntryField field)
    {
        this.language = language;
        this.languageFilterType = LanguageFilterType.PREFERRED;
        this.languageGroupField = field;

        return this;
    }

    //-----PROTECTED METHODS-----
    protected String nameOf(RdfProperty rdfProperty)
    {
        return new JsonField(rdfProperty).getName();
    }

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----

}

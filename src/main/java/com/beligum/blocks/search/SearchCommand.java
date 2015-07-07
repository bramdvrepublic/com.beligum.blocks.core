package com.beligum.blocks.search;

import com.beligum.blocks.models.interfaces.Resource;

import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 27/06/15.
 */
public interface SearchCommand
{
    public final int RESOURCES_ON_PAGE = 25;
    public List<Resource> search(String query, long page, Locale locale);

    public List<String> getTerms(String firstLetter, Locale locale);

    public long totalHits(String query, Locale locale);
}

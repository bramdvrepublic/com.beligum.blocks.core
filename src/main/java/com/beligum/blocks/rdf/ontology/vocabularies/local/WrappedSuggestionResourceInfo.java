package com.beligum.blocks.rdf.ontology.vocabularies.local;

import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;

import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 3/12/16.
 */
public class WrappedSuggestionResourceInfo implements ResourceInfo
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private AutocompleteSuggestion suggestion;
    private Locale language;

    //-----CONSTRUCTORS-----
    //For json deserialization
    private WrappedSuggestionResourceInfo()
    {
        this(null, null);
    }
    public WrappedSuggestionResourceInfo(AutocompleteSuggestion suggestion, Locale language)
    {
        this.suggestion = suggestion;
        this.language = language;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getResourceUri()
    {
        return this.suggestion.getValue() == null ? null : URI.create(this.suggestion.getValue());
    }
    @Override
    public URI getResourceType()
    {
        return this.suggestion.getResourceType();
    }
    @Override
    public String getLabel()
    {
        return this.suggestion.getTitle();
    }
    @Override
    public URI getLink()
    {
        return this.suggestion.getPublicPage();
    }
    @Override
    public boolean isExternalLink()
    {
        //local enums aren't external
        return false;
    }
    @Override
    public URI getImage()
    {
        //suggestions don't return images, so we can't return anything here
        return null;
    }
    @Override
    public String getName()
    {
        return this.suggestion.getTitle();
    }
    @Override
    public Locale getLanguage()
    {
        return this.language;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

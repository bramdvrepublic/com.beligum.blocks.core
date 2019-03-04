/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.rdf.ontologies.local;

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

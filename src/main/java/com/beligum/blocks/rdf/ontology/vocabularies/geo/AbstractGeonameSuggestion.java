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

package com.beligum.blocks.rdf.ontology.vocabularies.geo;

import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

/**
 * Created by bram on 3/12/16.
 */
public abstract class AbstractGeonameSuggestion extends AbstractGeoname implements AutocompleteSuggestion
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected URI resourceType;
    protected String geonameId;
    protected String name;
    protected String toponymName;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public String getValue()
    {
        return RdfTools.createRelativeResourceId(RdfFactory.getClassForResourceType(this.getResourceType()), geonameId).toString();
    }
    @Override
    public URI getResourceType()
    {
        return resourceType;
    }
    @Override
    public URI getPublicPage()
    {
        //Note: it makes sense to return the resource address as the public page; the application endpoint will decide what to do with it
        return RdfTools.createRelativeResourceId(RdfFactory.getClassForResourceType(this.getResourceType()), geonameId);
    }
    @Override
    public String getTitle()
    {
        return name;
    }
    @Override
    public String getSubTitle()
    {
        return toponymName;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    @JacksonInject(RESOURCE_TYPE_INJECTABLE)
    private void setResourceType(URI resourceType)
    {
        this.resourceType = resourceType;
    }
    @JsonIgnore
    private String getGeonameId()
    {
        return geonameId;
    }
    @JsonProperty
    private void setGeonameId(String geonameId)
    {
        this.geonameId = geonameId;
    }
    @JsonIgnore
    private String getName()
    {
        return name;
    }
    @JsonProperty
    private void setName(String name)
    {
        this.name = name;
    }
    @JsonIgnore
    private String getToponymName()
    {
        return toponymName;
    }
    @JsonProperty
    private void setToponymName(String toponymName)
    {
        this.toponymName = toponymName;
    }
}

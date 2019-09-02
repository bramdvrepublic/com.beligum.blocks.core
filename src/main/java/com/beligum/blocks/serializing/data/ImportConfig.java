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

package com.beligum.blocks.serializing.data;

import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.templating.TagTemplate;
import com.beligum.blocks.templating.TemplateCache;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.net.URI;

/**
 * This are the config parameters available while importing resources
 *
 * Created by bram on 4/5/16.
 */
public class ImportConfig implements Serializable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    /**
     * The relative path of the template to use to import (serialize) the resources
     */
    protected URI template;

    /**
     * The CURIE of the sameas property to use while importing resources
     */
    protected URI sameasProperty;

    /**
     * The CURIE of the title property to use while importing resources
     */
    protected URI titleProperty;

    /**
     * The CURIE of the image property to use while importing resources
     */
    protected URI imageProperty;

    /**
     * The CURIE of the video property to use while importing resources
     */
    protected URI videoProperty;

    /**
     * The name of the tag to use for serializing fact entries of the resources
     */
    protected String factBlock;

    /**
     * The name of the tag to use for serializing images of the resources
     */
    protected String imageBlock;

    /**
     * The name of the tag to use for serializing videos of the resources
     */
    protected String videoBlock;

    //-----CONSTRUCTORS-----
    public ImportConfig()
    {
    }

    //-----PUBLIC METHODS-----
    @XmlTransient
    public URI getTemplate()
    {
        return template;
    }
    @XmlTransient
    public RdfProperty getSameasProperty()
    {
        return RdfFactory.getProperty(sameasProperty);
    }
    @XmlTransient
    public RdfProperty getTitleProperty()
    {
        return RdfFactory.getProperty(titleProperty);
    }
    @XmlTransient
    public RdfProperty getImageProperty()
    {
        return RdfFactory.getProperty(imageProperty);
    }
    @XmlTransient
    public RdfProperty getVideoProperty()
    {
        return RdfFactory.getProperty(videoProperty);
    }
    @XmlTransient
    public TagTemplate getFactBlock()
    {
        return (TagTemplate) TemplateCache.instance().getByTagName(factBlock);
    }
    @XmlTransient
    public TagTemplate getImageBlock()
    {
        return (TagTemplate) TemplateCache.instance().getByTagName(imageBlock);
    }
    @XmlTransient
    public TagTemplate getVideoBlock()
    {
        return (TagTemplate) TemplateCache.instance().getByTagName(videoBlock);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

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

import com.beligum.base.server.R;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.utils.RdfTools;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This is basically a serialization contract for all resources that enter the system through our importer.
 *
 * Created by bram on 4/5/16.
 */
public class ImportResource implements Serializable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    /**
     * The relative, publicly visible address of this resource
     */
    protected URI uri;

    /**
     * The CURIE of the RdfClass of this resource
     */
    protected URI typeof;

    /**
     * The low-level (machine-generated?) relative URI of this resource (the one that links multiple languages together)
     */
    protected URI about;

    /**
     * A list of <property, value> mappings that make up the real data of this resource
     * Note that a list (instead of a map) allows us to add double mappings.
     */
    protected List<ImportPropertyMapping> properties;

    //-----CONSTRUCTORS-----
    public ImportResource()
    {
        this.properties = new ArrayList<>();
    }

    //-----PUBLIC METHODS-----
    @XmlTransient
    public URI getUri()
    {
        return uri;
    }
    @XmlTransient
    public RdfClass getTypeof()
    {
        return RdfFactory.getClass(this.typeof);
    }
    @XmlTransient
    public URI getAbout()
    {
        // we (manually) generate a base resource URI here (instead of letting the PageEndpoint.save() do it for us by detecting an empty @about tag on the <html>)
        // so we can interlink translations of resources together and letting them have a separate SEO-friendly URL at the same time
        // (instead of just using the first and adding a 'lang' query parameter)
        return this.about == null ? RdfTools.createRelativeResourceId(this.getTypeof()) : this.about;
    }
    @XmlTransient
    public Locale getLanguage()
    {
        return R.i18n().getUrlLocale(this.getUri());
    }
    @XmlTransient
    public Iterable<ImportPropertyMapping> getProperties()
    {
        return properties;
    }
    @XmlTransient
    public void addRdfProperty(RdfProperty rdfProperty, String rdfPropertyValue)
    {
        this.properties.add(new ImportPropertyMapping(rdfProperty.getCurie(), rdfPropertyValue));
    }
    @XmlTransient
    public ImportPropertyMapping getMapping(RdfProperty rdfProperty)
    {
        ImportPropertyMapping retVal = null;

        //lazy solution...
        for (ImportPropertyMapping entry : this.properties) {
            if (entry.getRdfProperty().equals(rdfProperty)) {
                retVal = entry;
                break;
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

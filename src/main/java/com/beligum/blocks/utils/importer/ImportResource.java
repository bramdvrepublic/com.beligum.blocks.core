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

package com.beligum.blocks.utils.importer;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bram on 4/5/16.
 */
public class ImportResource extends AbstractComparableProperty implements Serializable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //a list instead of a map allows us to add double mappings...
    @XmlElement
    private List<ImportPropertyMapping> properties;
    @XmlElement
    private List <ImportResourceObject> resourceObjects;

    //-----CONSTRUCTORS-----
    public ImportResource()
    {
        this.properties = new ArrayList<>();
        this.resourceObjects = new ArrayList<>();
    }

    //-----PUBLIC METHODS-----
    public void addRdfProperty(RdfProperty rdfProperty, String rdfPropertyValue, Integer index)
    {
        this.properties.add(new ImportPropertyMapping(rdfProperty.getCurieName(), rdfPropertyValue, index));
    }
    public void addResourceObject(ImportResourceObject resourceObject)
    {
        this.resourceObjects.add(resourceObject);
    }
    public ImportPropertyMapping getMapping(String rdfPropertyCurieName)
    {
        ImportPropertyMapping retVal = null;

        //lazy solution...
        for (ImportPropertyMapping entry : this.properties) {
            if (entry.getRdfPropertyCurieName().equals(rdfPropertyCurieName)) {
                retVal = entry;
                break;
            }
        }

        return retVal;
    }

    public List<ImportPropertyMapping> getRdfProperties()
    {
        return properties;
    }

    public List<ImportResourceObject> getResourceObjects()
    {
        return resourceObjects;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

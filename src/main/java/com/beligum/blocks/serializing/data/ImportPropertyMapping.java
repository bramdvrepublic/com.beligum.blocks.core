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

import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.net.URI;

/**
 * Created by bram on 4/5/16.
 */
public class ImportPropertyMapping implements Serializable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI rdfPropertyCurie;
    private String rdfPropertyValue;

    //-----CONSTRUCTORS-----
    public ImportPropertyMapping()
    {
    }
    public ImportPropertyMapping(URI rdfPropertyCurie, String rdfPropertyValue)
    {
        this.rdfPropertyCurie = rdfPropertyCurie;
        this.rdfPropertyValue = rdfPropertyValue;
    }

    //-----PUBLIC METHODS-----
    @XmlTransient
    public RdfProperty getRdfProperty()
    {
        return RdfFactory.getProperty(rdfPropertyCurie);
    }
    @XmlTransient
    public String getValue()
    {
        return rdfPropertyValue;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof ImportPropertyMapping))
            return false;

        ImportPropertyMapping that = (ImportPropertyMapping) o;

        if (rdfPropertyCurie != null ? !rdfPropertyCurie.equals(that.rdfPropertyCurie) : that.rdfPropertyCurie != null)
            return false;
        return rdfPropertyValue != null ? rdfPropertyValue.equals(that.rdfPropertyValue) : that.rdfPropertyValue == null;

    }
    @Override
    public int hashCode()
    {
        int result = rdfPropertyCurie != null ? rdfPropertyCurie.hashCode() : 0;
        result = 31 * result + (rdfPropertyValue != null ? rdfPropertyValue.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "" + rdfPropertyCurie + " -> '" + rdfPropertyValue + "'";
    }
}

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

import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.utils.importer.interfaces.ComparableProperty;

import java.util.Comparator;

/**
 * Created by bram on 3/22/16.
 */
public class RdfClassProperty extends AbstractComparableProperty
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private RdfProperty rdfProperty;
    private ValueFilter filter;

    //-----CONSTRUCTORS-----
    public RdfClassProperty(RdfProperty rdfProperty, int index)
    {
        this(rdfProperty, index, null);
    }
    public RdfClassProperty(RdfProperty rdfProperty, int index, ValueFilter filter)
    {
        this.rdfProperty = rdfProperty;
        super.index = index;
        this.filter = filter;
    }
    /**
     * This constructor can be used to map a property below another property (as extra information, eg. a country below a city to help querying for the right city),
     * instead of adding it as a top-level property.
     */
    public RdfClassProperty(RdfProperty rdfProperty, ValueFilter filter)
    {
        this.rdfProperty = rdfProperty;
        super.index = -1;
        this.filter = filter;
    }

    //-----PUBLIC METHODS-----
    public RdfProperty getRdfProperty()
    {
        return rdfProperty;
    }
    public ValueFilter getFilter()
    {
        return filter;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return this.getRdfProperty().getCurie().toString();
    }

    //-----INNER CLASSES-----
    public interface ValueFilter
    {
        String filterValue(String value);
    }


}

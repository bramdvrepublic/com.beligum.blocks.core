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

package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.IOException;

/**
 * Created by bram on 2/13/16.
 */
public class SparqlIndexEntry extends AbstractPageIndexEntry implements PageIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Model model;

    //-----CONSTRUCTORS-----
    public SparqlIndexEntry(String publicRelativeAddress, Model model) throws IOException
    {
        super(publicRelativeAddress);

        this.model = model;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getResource()
    {
        return null;
    }
    @Override
    public String getSubResourceExtraProperty()
    {
        return null;
    }
    @Override
    public String getParentId()
    {
        return null;
    }
    @Override
    public String getTypeOf()
    {
        return null;
    }
    @Override
    public String getLanguage()
    {
        return null;
    }
    @Override
    public String getCanonicalAddress()
    {
        return null;
    }
    public String getObject(RdfProperty predicate)
    {
        return this.getModelValue(this.model, predicate);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private String getModelValue(Model model, RdfProperty property)
    {
        String retVal = null;

        Model filteredModel = model.filter(null, SimpleValueFactory.getInstance().createIRI(property.getFullName().toString()), null);
        if (!filteredModel.isEmpty()) {
            retVal = filteredModel.iterator().next().getObject().stringValue();
        }

        return retVal;
    }

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "SparqlIndexEntry{" +
               "id='" + id + '\'' +
               '}';
    }
}

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

import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexEntry;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.google.common.collect.Sets;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by bram on 2/13/16.
 */
public class SparqlIndexEntry extends AbstractIndexEntry implements PageIndexEntry
{
    //-----CONSTANTS-----
    //Note: this doesn't really have internal fields, no?
    private static Collection<IndexEntryField> INTERNAL_FIELDS = Sets.newHashSet();

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
    public Iterable<IndexEntryField> getInternalFields()
    {
        return INTERNAL_FIELDS;
    }
    @Override
    public String getResource()
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

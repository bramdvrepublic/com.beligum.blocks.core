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

package com.beligum.blocks.filesystem.index.sparql;

import com.beligum.blocks.filesystem.index.entries.AbstractPageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.google.common.collect.Sets;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.Collection;

/**
 * Created by bram on 2/13/16.
 */
public class SparqlSelectIndexEntry extends AbstractPageIndexEntry
{
    //-----CONSTANTS-----
    //Note: this doesn't really have internal fields, no?
    private static Collection<IndexEntryField> INTERNAL_FIELDS = Sets.newHashSet();

    //-----VARIABLES-----
    private final BindingSet bindingSet;

    //-----CONSTRUCTORS-----
    public SparqlSelectIndexEntry(String id, BindingSet bindingSet)
    {
        super(id);

        this.bindingSet = bindingSet;
    }

    //-----PUBLIC METHODS-----
    @Override
    public Iterable<IndexEntryField> getInternalFields()
    {
        return INTERNAL_FIELDS;
    }
    public BindingSet getBindingSet()
    {
        return bindingSet;
    }
    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "SparqlSelectIndexEntry{" +
               "id='" + id + '\'' +
               '}';
    }
}

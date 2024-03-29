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

package com.beligum.blocks.index.sparql;

import com.beligum.base.resources.ifaces.ResourceAction;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.IndexEntryField;
import com.google.common.collect.Sets;
import org.eclipse.rdf4j.query.BindingSet;

import java.net.URI;
import java.util.Collection;

/**
 * Created by bram on 2/13/16.
 */
public class SparqlSelectIndexEntry extends AbstractIndexEntry
{
    //-----CONSTANTS-----
    //Note: this doesn't really have internal fields, no?
    private static Collection<IndexEntryField> INTERNAL_FIELDS = Sets.newHashSet();

    //-----VARIABLES-----
    private final BindingSet bindingSet;

    //-----CONSTRUCTORS-----
    public SparqlSelectIndexEntry(URI uri, BindingSet bindingSet)
    {
        super(uri);

        this.bindingSet = bindingSet;
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean isExternal()
    {
        return false;
    }
    @Override
    public boolean isPermitted(ResourceAction action)
    {
        // TODO see https://github.com/republic-of-reinvention/com.stralo.framework/issues/58
        //throw new UnsupportedOperationException("SPARQL result security filtering is not yet implemented, please look into this; " + action);
        return true;
    }
    public BindingSet getBindingSet()
    {
        return bindingSet;
    }

    //-----PROTECTED METHODS-----
    protected Iterable<IndexEntryField> getInternalFields()
    {
        return INTERNAL_FIELDS;
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "SparqlSelectIndexEntry{" +
               "uri='" + this.getUri() + '\'' +
               '}';
    }
}

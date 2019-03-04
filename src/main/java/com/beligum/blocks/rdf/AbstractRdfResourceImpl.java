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

package com.beligum.blocks.rdf;

import com.beligum.base.database.models.AbstractJsonObject;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.rdf.ifaces.RdfResource;
import org.apache.commons.lang.StringUtils;

/**
 * Created by bram on 3/3/16.
 */
public abstract class AbstractRdfResourceImpl extends AbstractJsonObject implements RdfResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //this is private because it's only accessible through the constructor
    private final String name;

    //protected because of the builder, see below
    protected boolean isPublic;

    //-----CONSTRUCTORS-----
    /**
     * Needed for RdfOntologyImpl
     */
    protected AbstractRdfResourceImpl()
    {
        this(null, false);
    }
    protected AbstractRdfResourceImpl(String name)
    {
        this(name, false);
    }
    protected AbstractRdfResourceImpl(String name, boolean isPublic)
    {
        if (StringUtils.isEmpty(name)) {
            throw new RdfInitializationException("Trying to create a nameless RDF resource, this isn't allowed.");
        }

        this.name = name;
        this.isPublic = isPublic;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return name;
    }
    @Override
    public boolean isPublic()
    {
        return isPublic;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

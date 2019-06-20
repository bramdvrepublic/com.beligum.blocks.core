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
import com.beligum.blocks.exceptions.RdfInstantiationException;
import com.beligum.blocks.rdf.ifaces.RdfResource;
import org.apache.commons.lang.StringUtils;

/**
 * Created by bram on 3/3/16.
 */
public abstract class AbstractRdfResourceImpl extends AbstractJsonObject implements RdfResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    boolean isPublic;
    // WARNING: when added fields, check RdfPropertyImpl.initFromToClone()

    //-----CONSTRUCTORS-----
    /**
     * Needed for RdfOntologyImpl
     */
    protected AbstractRdfResourceImpl()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean isOntology()
    {
        return this.getType().equals(Type.ONTOLOGY);
    }
    @Override
    public boolean isClass()
    {
        return this.getType().equals(Type.CLASS);
    }
    @Override
    public boolean isProperty()
    {
        return this.getType().equals(Type.PROPERTY);
    }
    @Override
    public boolean isDatatype()
    {
        return this.getType().equals(Type.DATATYPE);
    }
    @Override
    public boolean isPublic()
    {
        return isPublic;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "" + this.getName();
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof AbstractRdfResourceImpl)) return false;

        AbstractRdfResourceImpl that = (AbstractRdfResourceImpl) o;

        return getName() != null ? getName().equals(that.getName()) : that.getName() == null;

    }
    @Override
    public int hashCode()
    {
        return getName() != null ? getName().hashCode() : 0;
    }
}

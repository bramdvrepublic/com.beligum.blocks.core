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

import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.rdf.ifaces.RdfDatatype;
import com.beligum.blocks.rdf.ifaces.RdfOntology;

/**
 * Created by bram on 2/25/16.
 */
public class RdfDatatypeImpl extends RdfClassImpl implements RdfDatatype
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    RdfDatatypeImpl(RdfOntologyImpl ontology, String name)
    {
        super(ontology, name);
    }

    //-----PUBLIC METHODS-----
    @Override
    public Type getType()
    {
        return Type.DATATYPE;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    public static class Builder extends AbstractRdfOntologyMember.Builder<RdfDatatype, RdfDatatypeImpl, RdfDatatypeImpl.Builder>
    {
        Builder(RdfFactory rdfFactory, RdfDatatypeImpl rdfDataType)
        {
            super(rdfFactory, rdfDataType);
        }

        @Override
        RdfDatatype create() throws RdfInitializationException
        {
            //Note: this call will add us to the ontology
            return super.create();
        }
    }

}

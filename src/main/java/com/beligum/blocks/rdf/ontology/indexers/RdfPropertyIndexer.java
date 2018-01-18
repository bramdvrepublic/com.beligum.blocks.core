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

package com.beligum.blocks.rdf.ontology.indexers;

import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.filesystem.index.entries.RdfIndexer;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 5/31/16.
 */
public interface RdfPropertyIndexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * This method gets called when an RDF property is indexed by the indexer.
     * It should call the right method on the indexer to index the property value as closely as possible.
     * @return the value-object as it was indexed
     * @see com.beligum.blocks.rdf.ifaces.RdfProperty
     */
    RdfIndexer.IndexResult index(RdfIndexer indexer, URI subject, RdfProperty property, Value value, Locale language, RdfQueryEndpoint.SearchOption... options) throws IOException;

    /**
     * Converts the supplied value to an object to be used during index lookups
     * @see com.beligum.blocks.rdf.ifaces.RdfProperty
     */
    Object prepareIndexValue(RdfProperty property, String value, Locale language) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

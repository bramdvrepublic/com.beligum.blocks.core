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

package com.beligum.blocks.rdf.ifaces;

/**
 * The data type of a property, eg. XSD:string for a RDFS:label property.
 * This class in itself is more or less a representation of rdfs:Datatype, while instances
 * of this class attached to RdfProperties more or less coincide with the rdfs:range concept.
 *
 * Note that, contrary to RdfProperty, this does extend RdfClass because a regular class can be a datatype too and
 * it is used that way a well; if we want to add a property as an instance of a certain class to an ontology, the datatype
 * of that property will be a true RdfClass and not a RdfDataType. Actually, RdfDataType is a bit superfluous because it doesn't
 * really add a lot more to RdfClass. However, we keep it around because of XSD entries are actually more like true datatypes
 * than classes...
 *
 * Created by bram on 3/2/16.
 */
public interface RdfDatatype extends RdfClass
{
}

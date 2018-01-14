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

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.filesystem.index.entries.resources.ResourceIndexer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/**
 * This is more or less the OO representation of the RDFS::Class
 * <p>
 * Created by bram on 2/26/16.
 */
//don't really remember why we used custom de/serializing, but had to disable it
//because the JS-side relies on the metadata in this object
//@JsonSerialize(using = RdfClass._JsonSerializer.class)
//@JsonDeserialize(using = RdfClass._JsonDeserializer.class)
public interface RdfClass extends RdfResource
{
    enum Type
    {
        CLASS,
        PROPERTY,
        DATATYPE,
    }

    /**
     * Since both RdfProperty and RdfDataType are both subclasses of RdfClass,
     * this will allow us to detect pure classes (that are not a property or datatype).
     */
    Type getType();

    /**
     * The short, capitalized and camel-cased name that needs to be appended to the vocab to get the full describing URI for this class.
     * Eg. WaterWell
     */
    String getName();

    /**
     * The site-specific ontology URI for this class. Together with the name, it forms the full URI.
     * Eg. http://www.reinvention.be/ontology/
     */
    //note: data (URI and prefix) serialized in getFullName and getCurieName
    @JsonIgnore
    RdfVocabulary getVocabulary();

    /**
     * The full, absolute URI of this class that is built from the vocabulary URI and the name
     * Eg. http://www.reinvention.be/ontology/WaterWell
     */
    URI getFullName();

    /**
     * The full, absolute URI of this class that is built from the vocabulary CURIE and the name
     * Eg. mot:WaterWell
     */
    URI getCurieName();

    /**
     * The human readable describing phrase for this class, to be used to build admin-side selection lists etc.
     * This is the admin-side of this value; returns the key to this resource bundle
     */
    String getTitleKey();

    /**
     * The human readable describing phrase for this class, to be used to build admin-side selection lists etc.
     * Eg. Water well
     */
    String getTitle();

    /**
     * The human readable describing phrase for this class, to be used to build admin-side selection lists etc.
     * This is a more low-level (eg. API) accessor to this value, so know what you're doing.
     */
    @JsonIgnore
    MessagesFileEntry getTitleMessage();

    /**
     * The human readable describing phrase for this class, to be used in public HTML pages as a describing label next to the value of this class.
     * This is the admin-side of this value; returns the key to this resource bundle
     */
    String getLabelKey();

    /**
     * The human readable describing phrase for this class, to be used in public HTML pages as a describing label next to the value of this class.
     * Eg. Water well
     */
    String getLabel();

    /**
     * The human readable describing phrase for this class, to be used in public HTML pages as a describing label next to the value of this class.
     * This is a more low-level (eg. API) accessor to this value, so know what you're doing.
     */
    @JsonIgnore
    MessagesFileEntry getLabelMessage();

    /**
     * Optional (can be null) list of other ontology URIs that describe the same concept of the class described by this class.
     * Eg. http://dbpedia.org/page/Water_well
     */
    @JsonIgnore
    URI[] getIsSameAs();

    /**
     * Factory method to get a reference to the endpoint for this class.
     * The endpoint is used to lookup remote or local values for autocomplete boxes etc. of resources with this type.
     * Can return null to signal there is no such functionality and this class is for syntax/semantic-use only
     */
    @JsonIgnore
    RdfQueryEndpoint getEndpoint();

    /**
     * A collection of all properties of this class. This list is returned by the RDF endpoint when the properties for a page of type 'this' is requested.
     * When this method returns null, all RdfProperties known to this server are returned. When an empty list is returned, this class doesn't have any properties.
     */
    @JsonIgnore
    Set<RdfProperty> getProperties();

    /**
     * An indexer that pulls out search-relevant information from this instance during indexing.
     * If this is null, a generic SimpleResourceIndexer is used.
     */
    @JsonIgnore
    ResourceIndexer getResourceIndexer();

    /**
     * Setter for the value above. Note that this was pulled out of the constructor and made public to allow us to set it in a separate mapping.
     * This was because there was a cyclic static initialization dependency while creating the objects: terms relied on classes which relied on terms (to initialize this set).
     */
    @JsonIgnore
    void addProperties(RdfProperty... properties);

    /**
     * This method was added for inline object main property support: a main property of an inline object is
     * the property that the object actually represents (the other properties being extra information, like title + title type)
     */
    @JsonIgnore
    void setMainProperty(RdfProperty property);

    /**
     * The main property, see setMainProperty() for details
     */
    RdfProperty getMainProperty();

    //-----INNER CLASSES-----
    class _JsonSerializer extends JsonSerializer<RdfClass>
    {
        @Override
        public void serialize(RdfClass value, JsonGenerator gen, SerializerProvider serializers) throws IOException
        {
            URI curie = value == null ? null : (value.getCurieName() == null ? null : value.getCurieName());
            if (curie != null) {
                //ToStringSerializer.instance.serialize(curie, gen, serializers);
                gen.writeString(value.toString());
            }
        }
        @Override
        public void serializeWithType(RdfClass value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException
        {
            typeSer.writeTypePrefixForScalar(value, gen);
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffixForScalar(value, gen);
        }
    }

    class _JsonDeserializer extends JsonDeserializer<RdfClass>
    {
        @Override
        public RdfClass deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException
        {
            ObjectCodec oc = p.getCodec();
            JsonNode node = oc.readTree(p);
            String value = node.textValue();

            return value == null ? null : RdfFactory.getClassForResourceType(URI.create(value));
        }
    }
}

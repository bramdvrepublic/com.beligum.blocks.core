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

import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.index.ifaces.ResourceSummarizer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;
import java.net.URI;

/**
 * This is more or less the OO representation of the RDFS:Class
 * <p>
 * Created by bram on 2/26/16.
 */
//don't really remember why we used custom de/serializing, but had to disable it
//because the JS-side relies on the metadata in this object
//@JsonSerialize(using = RdfClass._JsonSerializer.class)
//@JsonDeserialize(using = RdfClass._JsonDeserializer.class)
public interface RdfClass extends RdfOntologyMember
{

    /**
     * A collection of all superclasses of this class. Note that RDF classes can extend multiple super classes.
     * This class inherits all properties of all registered superclasses.
     */
    @JsonIgnore
    Iterable<RdfClass> getSuperClasses();

    /**
     * A collection of all subclasses of this class.
     * This is basically the inverse of getSuperClasses()
     */
    @JsonIgnore
    Iterable<RdfClass> getSubClasses();

    /**
     * A collection of all properties of this class. This list is returned by the RDF endpoint when the properties for a page of type 'this' is requested.
     * When this method returns null, all RdfProperties known to this server are returned. When an empty list is returned, this class doesn't have any properties.
     * Note that this will return the properties of all super classes as well.
     */
    @JsonIgnore
    Iterable<RdfProperty> getProperties();

    /**
     * A check to see if the supplied property exists inside the iterable of getProperties()
     */
    @JsonIgnore
    boolean hasProperty(RdfProperty property);

    /**
     * A summarizer that pulls out search-relevant information from this instance during indexing.
     * If this is null, a generic SimpleResourceSummarizer is used.
     */
    @JsonIgnore
    ResourceSummarizer getSummarizer();

    /**
     * Added for inline object (rdf subclasses) main property support: a main property of an inline object is
     * the property that the object actually represents (the other properties being extra information, like title + title type).
     * This property is very closely related to the application-wide 'label property' (see settings), but separating it
     * (and calling it a 'main property', not the 'label property') allows us to skip the auto-adding of the label property
     * to specific sub-objects when there's an explicit main property set.
     * So, summarized: the (application-wide) label property is marked default, so it will be automatically added to add public classes,
     * with the exception of classes that have an explicit main property set. This method will return the label property if
     * no such explicit main property was set.
     */
    RdfProperty getMainProperty();

    /**
     * The validator of this class or null if no validation is required.
     * By definition, all non-null validators of all classes will be called
     * during instantiating/saving a member, allowing them to throw exceptions if any
     * attempts are made to create invalid classes.
     */
    @JsonIgnore
    RdfClassValidator getValidator();

    //-----INNER CLASSES-----
    class _JsonSerializer extends JsonSerializer<RdfClass>
    {
        @Override
        public void serialize(RdfClass value, JsonGenerator gen, SerializerProvider serializers) throws IOException
        {
            URI curie = value == null ? null : (value.getCurie() == null ? null : value.getCurie());
            if (curie != null) {
                //ToStringSerializer.instance.serialize(curie, gen, serializers);
                gen.writeString(value.toString());
            }
        }
        @Override
        public void serializeWithType(RdfClass value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException
        {
            typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.VALUE_STRING));
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeSer.typeId(value, JsonToken.VALUE_STRING));
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

            return value == null ? null : RdfFactory.getClass(value);
        }
    }
}

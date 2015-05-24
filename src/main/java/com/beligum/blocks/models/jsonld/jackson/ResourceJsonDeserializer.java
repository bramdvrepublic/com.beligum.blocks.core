package com.beligum.blocks.models.jsonld.jackson;

import com.beligum.blocks.models.jsonld.interfaces.Resource;
import com.beligum.blocks.models.jsonld.jsondb.ResourceImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Created by wouter on 21/05/15.
 */
public class ResourceJsonDeserializer extends JsonDeserializer<Resource>
{
    @Override
    public Resource deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException
    {
        return new ResourceImpl();
    }
}

package com.beligum.blocks.resources.jackson;

import com.beligum.blocks.resources.interfaces.Resource;
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
        throw new UnsupportedOperationException();
    }
}

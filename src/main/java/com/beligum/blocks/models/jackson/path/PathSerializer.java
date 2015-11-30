package com.beligum.blocks.models.jackson.path;

import com.beligum.blocks.models.sql.DBPath;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Created by wouter on 30/06/15.
 */
public class PathSerializer extends JsonSerializer<DBPath>
{

    @Override
    public void serialize(DBPath value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                JsonProcessingException
    {
        jgen.writeStartObject();
        jgen.writeFieldName("id");
        jgen.writeString(value.getDBid());

        jgen.writeFieldName("block_id");
        jgen.writeString(value.getBlockId().toString());

        jgen.writeFieldName("language");
        jgen.writeString(value.getLanguage().getLanguage());

        jgen.writeFieldName("url");
        jgen.writeString(value.getUrl().toString());

        jgen.writeFieldName("localized_url");
        jgen.writeString(value.getLocalizedUrl().toString());

        jgen.writeFieldName("statuscode");
        jgen.writeNumber(value.getStatusCode());
        jgen.writeEndObject();

    }

}

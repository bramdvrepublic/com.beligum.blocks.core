package com.beligum.blocks.models.jackson.path;


import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.models.sql.DBPath;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Created by wouter on 30/06/15.
 */
public class PathDeserializer extends JsonDeserializer<DBPath>
{

    @Override
    public DBPath deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException
    {
        DBPath retVal = null;
        JsonNode node = jsonParser.readValueAsTree();
        if (node.isObject()) {
            URI masterPage = UriBuilder.fromUri(node.get("masterPage").asText()).build();
            Path url = Paths.get(node.get("url").asText());
            Locale locale = BlocksConfig.instance().getLocaleForLanguage(node.get("language").asText());
            retVal = new DBPath(masterPage, url, locale);
            retVal.setStatusCode(node.get("statuscode").asInt());
        }

        return retVal;
    }

}

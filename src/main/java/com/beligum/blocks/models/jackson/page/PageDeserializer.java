package com.beligum.blocks.models.jackson.page;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.WebPageImpl;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.jackson.resource.ResourceJsonDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.LocalDateTime;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by wouter on 30/06/15.
 */
public class PageDeserializer<T extends WebPage> extends ResourceJsonDeserializer
{

    @Override
    public Resource deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException
    {
        JsonNode node = jsonParser.readValueAsTree();
        WebPage retVal = null;
        try {
            if (node.isObject()) {
                URI masterpage = UriBuilder.fromUri(node.get("master_page").asText()).build();

                // Insert all resource values into webpage
                Resource resource =  parseResource(node);
                retVal = new WebPageImpl(masterpage, resource.getBlockId(), resource.getLanguage());

                Set<URI> fields = resource.getFields();
                Iterator<URI> fieldIterator = fields.iterator();
                while (fieldIterator.hasNext()) {
                    URI field = fieldIterator.next();
                    retVal.set(field, resource.get(field));
                }

                String language = node.get(ParserConstants.JSONLD_LANGUAGE).asText();
                retVal.setLanguage(new Locale(language));
                String html = node.get("html").asText();
                retVal.setParsedHtml(html);
                String pageTemplate = node.get("page_template").asText();
                retVal.setPageTemplate(pageTemplate);

                if (node.get("page_title") != null) {
                    String pageTitle = node.get("page_title").asText();
                    retVal.setPageTitle(pageTitle);
                }

                String text = node.get("text").asText();
                retVal.setText(text);

                if (node.has("updated_at")) {
                    String updatedBy = node.get("updated_by").asText();
                    retVal.setUpdatedBy(updatedBy);
                    LocalDateTime updatedAt = new LocalDateTime(node.get("updated_at").asLong());
                    retVal.setUpdatedAt(updatedAt);
                }

                if (node.has("created_at")) {
                    String createdBy = node.get("created_by").asText();
                    retVal.setCreatedBy(createdBy);
                    LocalDateTime createdAt = new LocalDateTime(node.get("created_at").asLong());
                    retVal.setCreatedAt(createdAt);
                }

                Set<String> templates = new HashSet<String>();
                if (node.get("templates").isArray()) {
                    Iterator<JsonNode> iterator = node.get("templates").iterator();
                    while (iterator.hasNext()) {
                        templates.add(iterator.next().asText());
                    }
                }
                retVal.setTemplates(templates);

                Set<String> resources = new HashSet<String>();
                if (node.get("resources").isArray()) {
                    Iterator<JsonNode> iterator = node.get("resources").iterator();
                    while (iterator.hasNext()) {
                        resources.add(iterator.next().asText());
                    }
                }
                retVal.setResources(resources);

                Set<HashMap<String, String>> links = new HashSet<HashMap<String, String>>();
                if (node.get("links").isArray()) {
                    Iterator<JsonNode> iterator = node.get("links").iterator();
                    while (iterator.hasNext()) {
                        JsonNode next = iterator.next();
                        if (next.isObject()) {
                            HashMap<String, String> value = new HashMap<>();
                            value.put("absolute", next.get("absolute").asText());
                            value.put("html", next.get("html").asText());
                            value.put("page", next.get("page").asText());
                            links.add(value);
                        }
                    }
                }
                retVal.setLinks(links);

            }

        }
        catch (URISyntaxException e) {
            Logger.error("Exception while deserializing resource", e);
        }
        return retVal;
    }

}
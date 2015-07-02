package com.beligum.blocks.resources.jackson.page;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.beligum.blocks.resources.jackson.ResourceSerializer;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by wouter on 30/06/15.
 */
public class PageSerializer<T extends WebPage> extends ResourceSerializer
{

    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                  JsonProcessingException
    {
        WebPage webPage = (WebPage)value;

        printWebPageResource(jgen, webPage);



    }

    protected void printWebpageValues(JsonGenerator jgen, WebPage webPage) throws IOException
    {
        jgen.writeFieldName(ParserConstants.JSONLD_LANGUAGE);
        jgen.writeString(webPage.getLanguage().getLanguage());
        jgen.writeFieldName("html");
        jgen.writeString(webPage.getParsedHtml());
        jgen.writeFieldName("master_page");
        jgen.writeString(webPage.getMasterpageId().toString());
        jgen.writeFieldName("text");
        jgen.writeString(webPage.getText());
        jgen.writeFieldName("page_template");
        jgen.writeString(webPage.getPageTemplate());

        jgen.writeFieldName("updated_by");
        jgen.writeString(webPage.getUpdatedBy());
        jgen.writeFieldName("updated_at");
        jgen.writeNumber(webPage.getUpdatedAt().toDateTime(DateTimeZone.UTC).getMillis());
        jgen.writeFieldName("created_by");
        jgen.writeString(webPage.getCreatedBy());
        jgen.writeFieldName("created_at");
        jgen.writeNumber(webPage.getCreatedAt().toDateTime(DateTimeZone.UTC).getMillis());

        jgen.writeFieldName("templates");
        jgen.writeStartArray();
        for (String value: webPage.getTemplates()) {
            jgen.writeString(value);
        }
        jgen.writeEndArray();

        jgen.writeFieldName("resources");
        jgen.writeStartArray();
        for (String value: webPage.getResources()) {
            jgen.writeString(value);
        }
        jgen.writeEndArray();

        jgen.writeFieldName("links");
        jgen.writeStartArray();
        for (HashMap<String, String> value: webPage.getLinks()) {
            jgen.writeStartObject();
            jgen.writeFieldName("html");
            jgen.writeString(value.get("html"));
            jgen.writeFieldName("absolute");
            jgen.writeString(value.get("absolute"));
            jgen.writeFieldName("page");
            jgen.writeString(value.get("page"));
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

    }

    protected void printWebPageResource(JsonGenerator jgen, WebPage webPage) throws IOException {
        HashMap<String, String> context = new HashMap<>();

        jgen.writeStartObject();
        if (printRootFields()) {

            // Add @id to json
            if (webPage.getBlockId() != null) {
                jgen.writeStringField(ParserConstants.JSONLD_ID, webPage.getBlockId().toString());
            }

            // Add @type to json
            if (webPage.getRdfType() != null) {
                Set<URI> typeNode = webPage.getRdfType();
                jgen.writeFieldName(ParserConstants.JSONLD_TYPE);
                if (typeNode != null) {
                    jgen.writeStartArray();
                    for (URI fieldValue : typeNode) {
                        jgen.writeString(fieldValue.toString());
                    }
                    jgen.writeEndArray();
                }
            }
        }

        // Add other fields to json
        Iterator<URI> it = getFieldIterator(webPage);
        while (it.hasNext()) {
            URI field = it.next();
            String stringField = RdfTools.makeDbFieldFromUri(field);
            context.put(stringField, field.toString());
            Node fieldNode = webPage.get(field);
            jgen.writeFieldName(stringField);
            jgen.writeStartArray();
            printListNode(jgen, fieldNode, webPage.getLanguage());
            jgen.writeEndArray();

        }

        if (printRootFields()) {
            // Write context
            jgen.writeFieldName(ParserConstants.JSONLD_CONTEXT);
            jgen.writeStartObject();
            for (String key : context.keySet()) {
                jgen.writeFieldName(key);
                jgen.writeString(context.get(key));
            }
            jgen.writeEndObject();
        }


        // now wri
        printWebpageValues(jgen, webPage);


        jgen.writeEndObject();


    }
}

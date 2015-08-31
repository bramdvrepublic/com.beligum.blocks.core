package com.beligum.blocks.models.jackson.page;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.models.interfaces.Node;
import com.beligum.blocks.models.jackson.NodeSerializer;
import com.beligum.blocks.models.jackson.resource.ResourceSerializer;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 30/06/15.
 */
public class PageSerializer<T extends WebPage> extends NodeSerializer
{

    @Override
    protected void writeSpecialProperties(JsonGenerator jgen, Resource resource) throws IOException
    {
        if (resource instanceof WebPage) {
            WebPage webPage = (WebPage) resource;
            jgen.writeFieldName(ParserConstants.PAGE_PROPERTY);
            jgen.writeStartObject();

            if (webPage.getParsedHtml(Locale.ROOT, false) != null) {
                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_HTML);
                jgen.writeString(webPage.getParsedHtml(Locale.ROOT, false));
            }
            writeLocalValue(jgen, ParserConstants.PAGE_PROPERTY_HTML, webPage);

            if (webPage.getText(Locale.ROOT) != null) {
                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_TEXT);
                jgen.writeString(webPage.getText(Locale.ROOT));
            }
            writeLocalValue(jgen, ParserConstants.PAGE_PROPERTY_TEXT, webPage);

            if (webPage.getPageTemplate(Locale.ROOT) != null) {
                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_PAGETEMPLATE);
                jgen.writeString(webPage.getPageTemplate(Locale.ROOT));
            }
            writeLocalValue(jgen, ParserConstants.PAGE_PROPERTY_PAGETEMPLATE, webPage);

            if (webPage.getPageTitle(Locale.ROOT, false) != null) {
                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_PAGETITLE);
                jgen.writeString(webPage.getPageTitle(Locale.ROOT, false));
            }
            writeLocalValue(jgen, ParserConstants.PAGE_PROPERTY_PAGETITLE, webPage);

            if (webPage.getUpdatedAt() != null) {
                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_UPDATED_BY);
                jgen.writeString(webPage.getUpdatedBy(Locale.ROOT));
                writeLocalValues(jgen, ParserConstants.PAGE_PROPERTY_UPDATED_BY, webPage);

                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_UPDATED_AT);
                jgen.writeNumber(webPage.getUpdatedAt(Locale.ROOT).toDateTime(DateTimeZone.UTC).getMillis());
                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_UPDATED_AT + ParserConstants.LOCALIZED_PROPERTY);
                jgen.writeStartObject();
                for (Locale locale : webPage.getLanguages()) {
                    jgen.writeFieldName(locale.getLanguage());
                    LocalDateTime value = webPage.getUpdatedAt(locale);
                    if (value != null) {
                        jgen.writeNumber(value.toDateTime(DateTimeZone.UTC).getMillis());
                    }
                }
                jgen.writeEndObject();
            }

            if (webPage.getCreatedAt() != null) {
                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_CREATED_BY);
                jgen.writeString(webPage.getCreatedBy());
                writeLocalValues(jgen, ParserConstants.PAGE_PROPERTY_CREATED_BY, webPage);

                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_CREATED_AT);
                jgen.writeNumber(webPage.getCreatedAt().toDateTime(DateTimeZone.UTC).getMillis());
                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_UPDATED_AT + ParserConstants.LOCALIZED_PROPERTY);
                jgen.writeStartObject();
                for (Locale locale : webPage.getLanguages()) {
                    jgen.writeFieldName(locale.getLanguage());
                    LocalDateTime value = webPage.getCreatedAt(locale);
                    if (value != null) {
                        jgen.writeNumber(value.toDateTime(DateTimeZone.UTC).getMillis());
                    }
                }
                jgen.writeEndObject();
            }

            jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_TEMPLATES);
            jgen.writeStartArray();
            for (String value : webPage.getTemplates()) {
                jgen.writeString(value);
            }
            jgen.writeEndArray();
            writeLocalValues(jgen, ParserConstants.PAGE_PROPERTY_TEMPLATES, webPage);

            jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_RESOURCES);
            jgen.writeStartArray();
            for (String value : webPage.getResources()) {
                jgen.writeString(value);
            }
            jgen.writeEndArray();
            writeLocalValues(jgen, ParserConstants.PAGE_PROPERTY_RESOURCES, webPage);

            jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_LINKS);
            jgen.writeStartArray();
            for (Map<String, String> value : webPage.getLinks(Locale.ROOT)) {
                jgen.writeStartObject();
                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_HTML);
                jgen.writeString(value.get(ParserConstants.PAGE_PROPERTY_HTML));
                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_ABSOLUTE);
                jgen.writeString(value.get(ParserConstants.PAGE_PROPERTY_ABSOLUTE));
                jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_REFERENCED_PAGE);
                jgen.writeString(value.get(ParserConstants.PAGE_PROPERTY_REFERENCED_PAGE));
                jgen.writeEndObject();
            }
            jgen.writeEndArray();

            // Localized links
            jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_LINKS + ParserConstants.LOCALIZED_PROPERTY);
            jgen.writeStartObject();
            for (Locale locale : webPage.getLanguages()) {
                jgen.writeFieldName(locale.getLanguage());
                jgen.writeStartArray();
                for (Map<String, String> value : webPage.getLinks(locale)) {
                    jgen.writeStartObject();
                    jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_HTML);
                    jgen.writeString(value.get(ParserConstants.PAGE_PROPERTY_HTML));
                    jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_ABSOLUTE);
                    jgen.writeString(value.get(ParserConstants.PAGE_PROPERTY_ABSOLUTE));
                    jgen.writeFieldName(ParserConstants.PAGE_PROPERTY_REFERENCED_PAGE);
                    jgen.writeString(value.get(ParserConstants.PAGE_PROPERTY_REFERENCED_PAGE));
                    jgen.writeEndObject();
                }
                jgen.writeEndArray();
            }
            jgen.writeEndObject(); // end links

            // End Page property
            jgen.writeEndObject();
        }

    }

    // ---------- PRIVATE METHODS ------------

    private void writeLocalValue(JsonGenerator jgen, String property, WebPage resource) throws IOException
    {
        jgen.writeFieldName(property + ParserConstants.LOCALIZED_PROPERTY);
        jgen.writeStartObject();
        for (Locale locale: resource.getLanguages()) {
            String value = null;
            if (property.equals(ParserConstants.PAGE_PROPERTY_HTML)) {
                value = resource.getParsedHtml(locale, false);
                if (value != null) {
                    jgen.writeFieldName(locale.getLanguage());
                    jgen.writeString(value);
                }
            } else if (property.equals(ParserConstants.PAGE_PROPERTY_TEXT)) {
                value = resource.getText(locale);
                if (value != null) {
                    jgen.writeFieldName(locale.getLanguage());
                    jgen.writeString(value);
                }
            } else if (property.equals(ParserConstants.PAGE_PROPERTY_PAGETEMPLATE)) {
                value = resource.getPageTemplate(locale);
                if (value != null) {
                    jgen.writeFieldName(locale.getLanguage());
                    jgen.writeString(value);
                }
            } else if (property.equals(ParserConstants.PAGE_PROPERTY_PAGETITLE)) {
                value = resource.getPageTitle(locale, false);
                if (value != null) {
                    jgen.writeFieldName(locale.getLanguage());
                    jgen.writeString(value);
                }
            } else if (property.equals(ParserConstants.PAGE_PROPERTY_UPDATED_BY)) {
                value = resource.getUpdatedBy(locale);
                if (value != null) {
                    jgen.writeFieldName(locale.getLanguage());
                    jgen.writeString(value);
                }
            } else if (property.equals(ParserConstants.PAGE_PROPERTY_CREATED_BY)) {
                value = resource.getCreatedBy(locale);
                if (value != null) {
                    jgen.writeFieldName(locale.getLanguage());
                    jgen.writeString(value);
                }
            } else {
                Logger.error("Trying to write localized string field while serializing webpage for unknown property");
            }

        }
        jgen.writeEndObject();
    }

    private void writeLocalValues(JsonGenerator jgen, String property, WebPage resource) throws IOException
    {
        jgen.writeFieldName(property + ParserConstants.LOCALIZED_PROPERTY);
        jgen.writeStartObject();
        for (Locale locale: resource.getLanguages()) {
            jgen.writeFieldName(locale.getLanguage());

            if (property.equals(ParserConstants.PAGE_PROPERTY_TEMPLATES)) {
                jgen.writeStartArray();
                for (String value : resource.getTemplates(locale)) {
                    jgen.writeString(value);
                }
                jgen.writeEndArray();
            } else if (property.equals(ParserConstants.PAGE_PROPERTY_RESOURCES)) {
                jgen.writeStartArray();
                for (String value: resource.getResources(locale)) {
                    jgen.writeString(value);
                }
                jgen.writeEndArray();
            } else {
                Logger.error("Trying to write localized string field while serializing webpage for unknown property");
            }
        }
        jgen.writeEndObject();

    }

}

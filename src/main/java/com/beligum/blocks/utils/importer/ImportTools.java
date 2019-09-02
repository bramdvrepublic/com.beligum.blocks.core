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

package com.beligum.blocks.utils.importer;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.WidgetType;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import com.beligum.blocks.rdf.ifaces.RdfEndpoint;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.utils.RdfTools;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import static java.time.ZoneOffset.UTC;

/**
 * Created by bram on 3/22/16.
 */
public abstract class ImportTools
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    public static Object importRawPropertyValue(RdfProperty property, String value, Locale language) throws IOException
    {
        Object retVal = null;

        boolean isNumber = NumberUtils.isNumber(value);
        switch (property.getWidgetType()) {
//            case Immutable:
//                //this can be both a string and a number.
            case InlineEditor:
                //FIXME Is there any cleaner way to handle this?
                retVal = StringEscapeUtils.escapeHtml(value);
                break;
            case Editor:
                value = StringEscapeUtils.escapeHtml(value);
                retVal = value.replaceAll("(\r\n|\r|\n|\n\r)", "<br>");
                break;
            case Boolean:
                retVal = RdfTools.parseRdfaBoolean(value);
                break;

            case Number:
                //FIXME handles numbers that are Integers and Floats. Throws an exception otherwise
                try{
                    retVal = new Integer(value);
                }catch (NumberFormatException ex){
                    retVal = new Float(value);
                }
                break;

            case Date:
                if (isNumber) {
                    retVal = epochToLocalDateTime(Long.parseLong(value)).toLocalDate();
                }
                else {
                    retVal = LocalDate.parse(value);
                }
                break;

            case Time:
                if (isNumber) {
                    retVal = epochToLocalDateTime(Long.parseLong(value)).toLocalTime();
                }
                else {
                    retVal = LocalTime.parse(value);
                }
                break;

            case DateTime:
                if (isNumber) {
                    retVal = epochToLocalDateTime(Long.parseLong(value));
                }
                else {
                    retVal = LocalDateTime.parse(value);
                }
                break;

            case Color:
                retVal = Color.decode(value);
                break;

            case Uri:

                //little trick to allow a Link name to be passed to us
                URI uri = null;
                String linkName = null;
                if (value.contains("|")) {
                    String[] s = value.split("\\|");
                    retVal = new NamedUri(URI.create(s[0]), s[1]);
                }
                else {
                    retVal = new NamedUri(URI.create(value));
                }

                break;

            case Enum:
                retVal = value;
                break;
            case Object:
//            case ResourceList:
            case Resource:

                //if the value is a resource string, parse it directly, otherwise, query the endpoint for matches
                if (value.startsWith(Settings.RESOURCE_ENDPOINT)) {
                    retVal = URI.create(value);
                }
                else {
                    //I think it's better to use a true query-like search, so we can add more liberties (eg. specify "Loenhout,Belgium" instead of "Loenhout" and hope for the best)
                    RdfEndpoint.QueryType queryType = RdfEndpoint.QueryType.FULL;
                    //Note: we disabled this when factoring out the ontologies from blocks-core, please re-enable it if necessary
                    //                    //Hmmm, this doesn't seem to work well with countries (like a query for 'Belgium' yields Zimbabwe if you do a full 'q' query (instead of searching by name))
                    //                    if (property.equals(Terms.country)) {
                    //                        queryType = RdfQueryEndpoint.QueryType.NAME;
                    //                    }

                    //extra wrapper around this part because it might call a remote API and the return values (eg. 404) can be mixed up
                    // with the return values of this endpoint. By wrapping it in a different try-catch, we get extra logging if something goes wrong.
                    try {
                        Iterable<ResourceProxy> searchSuggestions = property.getDataType().getEndpoint().search(property.getDataType(), value, queryType, language, 1);
                        //just take the first hit
                        Iterator<ResourceProxy> iter = searchSuggestions.iterator();
                        if (iter.hasNext()) {
                            retVal = URI.create(iter.next().getResource());
                        }
                        else {
                            throw new IOException("Unable to find a resource of type " + property.getDataType() + " for input '" + value +
                                                  "', please fix this (eg. with filters) or you'll end up with empty fields");
                        }
                    }
                    catch (Exception e) {
                        throw new IOException("Error happened while looking up resource during import of " + property + "; " + value, e);
                    }
                }
                break;

            default:
                throw new IOException("Encountered unimplemented widget type parser, please fix; " + property.getWidgetType());
        }

        return retVal;
    }
    public static String propertyValueToHtml(RdfProperty property, Object value, Locale language, RdfProperty previous, Map<URI, ImportResourceObject> importResourceObjectMap)
                    throws IOException, URISyntaxException, ParseException
    {
        StringBuilder factEntryHtml = new StringBuilder();

        if (previous != null && previous.equals(property)) {
            factEntryHtml.append("<blocks-fact-entry class=\"double\">");
        }
        else {
            factEntryHtml.append("<blocks-fact-entry>");
        }
        factEntryHtml.append("<div data-property=\"name\"><p>").append(R.i18n().get(property.getLabelKey(), language)).append("</p></div>");
        factEntryHtml.append("<div data-property=\"value\">");
        factEntryHtml.append("<div class=\"property ").append(property.getWidgetType().getConstant()).append("\"");
        factEntryHtml.append(" property=\"").append(property.getCurie()).append("\"");

        //"#"+Integer.toHexString(color.getRGB()).substring(2)

        boolean addDataType = true;
        String content = null;
        String html = "";
        ZoneId localZone = ZoneId.systemDefault();
        switch (property.getWidgetType()) {
            //            case Immutable:
            //                //this does  not have real type attributed to it.
            //                //For now this can be either a number or a string
            //                if(property.getDataType().equals(XSD.STRING)){
            //                    //is a string
            //                    html = value.toString();
            //                }else if(property.getDataType().equals(XSD.INTEGER) || property.getDataType().equals(XSD.INT)){
            //                    //is a number
            //                    content = value.toString();
            //                    html = value.toString();
            //                }else{
            //                    //not supported
            //                    throw new IOException(property.getWidgetType().name()+ " does not support "+property.getDataType());
            //                }
            //                break;
            case Editor:
                html = value.toString();
                break;
            case InlineEditor:
                factEntryHtml.append(" data-editor-options=\"force-inline no-toolbar\"");
                html = value.toString();
                break;
            case Boolean:
                content = value.toString();
                html = "<div class=\"" + gen.com.beligum.blocks.core.constants.blocks.core.Entries.WIDGET_TYPE_BOOLEAN_VALUE_CLASS + "\"></div>";
                break;
            case Number:
                content = value.toString();
                html = value.toString();
                break;
            case Date:
                TemporalAccessor utcDate;
                utcDate = (TemporalAccessor) value;
//                if (value instanceof LocalDate) {
//                    utcDate = ZonedDateTime.ofInstant(((LocalDate) value).atStartOfDay(localZone).toInstant(), UTC);
//                }
//                else {
//                    utcDate = (TemporalAccessor) value;
//                }

                //Note: local because we only support timezones in dateTime
                content = DateTimeFormatter.ISO_LOCAL_DATE.format(utcDate);
                factEntryHtml.append(" data-gmt=\"false\"");

                //https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
                //eg. Wednesday September 4 1986
                html = RdfTools.serializeDateHtml(localZone, language, utcDate).toString();

                break;

            case Time:
                TemporalAccessor utcTime;
                if (value instanceof LocalTime) {
                    utcTime = ZonedDateTime.ofInstant(ZonedDateTime.of(LocalDate.now(), (LocalTime) value, localZone).toInstant(), UTC);
                }
                else {
                    utcTime = (TemporalAccessor) value;
                }

                //Note: local because we only support timezones in dateTime
                content = DateTimeFormatter.ISO_LOCAL_TIME.format(utcTime);
                factEntryHtml.append(" data-gmt=\"false\"");

                //html = "1:00 AM<span class=\"timezone\">(UTC+01:00)</span>";
                html =
                                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(localZone).withLocale(language).format(utcTime) + "<span class=\"timezone\">(UTC" +
                                DateTimeFormatter.ofPattern("xxxxx").withZone(localZone).withLocale(language).format(utcTime) + ")</span>";

                break;

            case DateTime:
                TemporalAccessor utcDateTime;
                if (value instanceof LocalDateTime) {
                    utcDateTime = ZonedDateTime.ofInstant(ZonedDateTime.of((LocalDateTime) value, localZone).toInstant(), UTC);
                }
                else {
                    utcDateTime = (TemporalAccessor) value;
                }

                content = DateTimeFormatter.ISO_DATE_TIME.format(utcDateTime);
                factEntryHtml.append(" data-gmt=\"false\"");

                //html = "Friday January 1, 2016 - 1:00 AM<span class=\"timezone\">(UTC+01:00)</span>";
                html = RdfTools.serializeDateTimeHtml(localZone, language, utcDateTime).toString();

                break;

            case Color:
                content = "#" + Integer.toHexString(((Color) value).getRGB()).substring(2);
                html = "<div class=\"" + gen.com.beligum.blocks.core.constants.blocks.core.Entries.WIDGET_TYPE_COLOR_VALUE_CLASS.getValue() + "\" style=\"background-color: " + content + "\"></div>";
                break;

            case Uri:
                NamedUri uriValue = (NamedUri) value;
                addDataType = false;

                // We need to also add the hyperlink href as a property-value, because when we wrap the <a> tag with a <div property=""> tag,
                // the content of the property tag (eg. the entire <a> tag) gets serialized by the RDFa parser as a I18N-string, using the human readable
                // text of the hyperlink as a value (instead of using the href value and serializing it as a URI). This is because the property attribute is set on the
                // wrapping <div> instead of on the <a> tag.
                //Note: from the RDFa docs: "@content is used to indicate the value of a plain literal", and since it's a URI, we add it as a resource value
                factEntryHtml.append(" resource=\"" + uriValue.getUri().toString() + "\"");

                html = "<a href=\"" + uriValue.getUri().toString() + "\"";
                if (uriValue.getUri().isAbsolute()) {
                    html += " target=\"_blank\"";
                }
                html += ">" + (StringUtils.isEmpty(uriValue.getName()) ? uriValue.getUri().toString() : uriValue.getName()) + "</a>";

                break;

            case Enum:
                //this is an enum key that needs to be looked up for validation
                String enumKey = value.toString();

                //note: contrary to the resource endpoint below, we want the endpoint of the property, not the class, so don't use property.getDataType().getEndpoint() here
                Iterable<ResourceProxy> enumSuggestion = property.getEndpoint().search(property, enumKey, RdfEndpoint.QueryType.NAME, language, 1);
                Iterator<ResourceProxy> iter = enumSuggestion.iterator();
                if (iter.hasNext()) {
                    ResourceProxy enumValue = iter.next();
                    addDataType = true;
                    content = enumValue.getResource();
                    html = RdfTools.serializeEnumHtml(enumValue).toString();
                }
                else {
                    throw new IOException("Unable to find enum value; " + enumKey);
                }

                break;
            //            case ResourceList:
            case Resource:

                URI resourceId = (URI) value;
                ResourceProxy resourceInfo = property.getDataType().getEndpoint().getResource(property.getDataType(), resourceId, language);
                if(resourceInfo == null){
                    Logger.error("resourceinfo is null. Retrying");
                    resourceInfo = property.getDataType().getEndpoint().getResource(property.getDataType(), resourceId, language);
                }
//                if(resourceInfo == null){
//                    Logger.error("Unable to find resource. Ignoring; " + resourceId);
//                    return "";
////                    throw new IOException("Unable to find resource; " + resourceId);
//                }
                addDataType = false;
                if(resourceInfo != null){
                    factEntryHtml.append(" resource=\"" + resourceInfo.getResource() + "\"");
                }else{
                    factEntryHtml.append(" resource=\"" + resourceId + "\"");
                }
                if (resourceInfo != null) {
                    html = RdfTools.serializeResourceHtml(property, resourceInfo).toString();
                }
                else {
                    resourceInfo = property.getDataType().getEndpoint().getResource(property.getDataType(), resourceId, language);
                    throw new IOException("Unable to find resource; " + resourceId);
                }

            break;
            case Object:

                URI objectId = (URI) value;
                //                ResourceInfo objectInfo = property.getDataType().getEndpoint().getResource(property.getDataType(), objectId, language);
                ImportResourceObject importResourceObject = importResourceObjectMap.get(objectId);
                addDataType = false;
                factEntryHtml.append(" typeof=\"" + importResourceObject.getResourceType().toString() + "\"");
                factEntryHtml.append(" resource=\"" + objectId.toString() + "\"");
                try{
                    html = RdfTools.serializeObjectHtml(importResourceObject, language).toString();
                }catch (Exception ex){
                    RdfTools.serializeObjectHtml(importResourceObject, language).toString();
                    throw ex;
                }
                break;
            default:
                throw new IOException("Encountered unimplemented widget type parser, please fix; " + property.getWidgetType());
        }

        //Some extra filtering, based on the datatype
        if (property.getDataType().equals(RDF.langString)) {
            //see the comments in blocks-fact-entry.js and RDF.LANGSTRING for why we remove the datatype in case of a rdf:langString
            //for a langstring we have to add a language tag
            addDataType = false;
        }

        if (addDataType) {
            factEntryHtml.append(" datatype=\"").append(property.getDataType().getCurie()).append("\"");
        }

        if (content != null) {
            factEntryHtml.append(" content=\"").append(content).append("\"");
        }

        //extra tag options
        factEntryHtml.append(">");

        factEntryHtml.append(html);

        factEntryHtml.append("</div>");
        factEntryHtml.append("</div>");
        factEntryHtml.append("</blocks-fact-entry>");

        return factEntryHtml.toString();
    }
    public static String buildResourceImageHtml(NamedUri uri)
    {
        StringBuilder retVal = new StringBuilder();

        retVal.append("<blocks-image class=\"bordered\">");
        retVal.append("<img property=\"image\" src=\"").append(uri.getUri().toString()).append("\" >");
        retVal.append("</blocks-image>");

        return retVal.toString();
    }

    //-----PRIVATE METHODS-----
    private static LocalDateTime epochToLocalDateTime(long value)
    {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneId.systemDefault());
    }

    //-----INNER CLASSES-----
    public static class NamedUri
    {
        URI uri;
        String name;

        public NamedUri(URI uri)
        {
            this(uri, null);
        }
        public NamedUri(URI uri, String name)
        {
            this.uri = uri;
            this.name = name;
        }

        public URI getUri()
        {
            return uri;
        }
        public String getName()
        {
            return name;
        }
    }
}

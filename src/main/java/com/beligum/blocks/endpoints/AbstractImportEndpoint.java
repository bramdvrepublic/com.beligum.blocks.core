package com.beligum.blocks.endpoints;

import com.beligum.base.server.R;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontology.factories.Terms;
import com.beligum.blocks.utils.RdfTools;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Locale;

import static java.time.ZoneOffset.UTC;

/**
 * Created by bram on 3/22/16.
 */
public abstract class AbstractImportEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    protected Object importRawPropertyValue(RdfProperty property, String value, Locale language) throws IOException, ParseException
    {
        Object retVal = null;

        boolean isNumber = NumberUtils.isNumber(value);
        switch (property.getWidgetType()) {
            case Editor:
            case InlineEditor:
                retVal = value;
                break;

            case Boolean:
                retVal = RdfTools.parseRdfaBoolean(value);
                break;

            case Number:
                retVal = new Integer(value);
                break;

            case Date:
                if (isNumber) {
                    retVal = this.epochToLocalDateTime(Long.parseLong(value)).toLocalDate();
                }
                else {
                    retVal = LocalDate.parse(value);
                }
                break;

            case Time:
                if (isNumber) {
                    retVal = this.epochToLocalDateTime(Long.parseLong(value)).toLocalTime();
                }
                else {
                    retVal = LocalTime.parse(value);
                }
                break;

            case DateTime:
                if (isNumber) {
                    retVal = this.epochToLocalDateTime(Long.parseLong(value));
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

            case Resource:

                //if the value is a resource string, parse it directly, otherwise, query the endpoint for matches
                if (value.startsWith(Settings.RESOURCE_ENDPOINT)) {
                    retVal = URI.create(value);
                }
                else {
                    //I think it's better to use a true query-like search, so we can add more liberties (eg. specify "Loenhout,Belgium" instead of "Loenhout" and hope for the best)
                    RdfQueryEndpoint.QueryType queryType = RdfQueryEndpoint.QueryType.FULL;
                    //Hmmm, this doesn't seem to work well with countries (like a query for 'Belgium' yields Zimbabwe if you do a full 'q' query (instead of searching by name))
                    if (property.equals(Terms.country)) {
                        queryType = RdfQueryEndpoint.QueryType.NAME;
                    }

                    //extra wrapper around this part because it might call a remote API and the return values (eg. 404) can be mixed up
                    // with the return values of this endpoint. By wrapping it in a different try-catch, we get extra logging if something goes wrong.
                    try {
                        Collection<AutocompleteSuggestion> searchSuggestions = property.getDataType().getEndpoint().search(property.getDataType(), value, queryType, language, 1);
                        //just take the first hit
                        if (!searchSuggestions.isEmpty()) {
                            retVal = URI.create(searchSuggestions.iterator().next().getValue());
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
    protected String propertyValueToHtml(RdfProperty property, Object value, Locale language, RdfProperty previous) throws IOException, ParseException
    {
        StringBuilder factEntryHtml = new StringBuilder();

        if (previous!=null && previous.equals(property)) {
            factEntryHtml.append("<blocks-fact-entry class=\"double\">");
        }
        else {
            factEntryHtml.append("<blocks-fact-entry>");
        }
        factEntryHtml.append("<div data-property=\"name\"><p>").append(R.i18n().get(property.getLabelKey(), language)).append("</p></div>");
        factEntryHtml.append("<div data-property=\"value\">");
        factEntryHtml.append("<div class=\"property ").append(property.getWidgetType().getConstant()).append("\"");
        factEntryHtml.append(" property=\"").append(property.getCurieName()).append("\"");

        //"#"+Integer.toHexString(color.getRGB()).substring(2)

        boolean addDataType = true;
        String content = null;
        String html = "";
        ZoneId localZone = ZoneId.systemDefault();
        switch (property.getWidgetType()) {
            case Editor:
                html = value.toString();
                break;
            case InlineEditor:
                factEntryHtml.append(" data-editor-options=\"force-inline no-toolbar\"");
                html = value.toString();
                break;
            case Boolean:
                content = value.toString();
                html = "<div class=\"" + gen.com.beligum.blocks.core.constants.blocks.core.Entries.INPUT_TYPE_BOOLEAN_VALUE_CLASS + "\"></div>";
                break;
            case Number:
                content = value.toString();
                html = value.toString();
                break;
            case Date:
                LocalDate localDate = (LocalDate) value;
                ZonedDateTime utcDate = ZonedDateTime.ofInstant(localDate.atStartOfDay(localZone).toInstant(), UTC);
                content = utcDate.format(DateTimeFormatter.ISO_DATE);
                factEntryHtml.append(" data-gmt=\"false\"");

                //https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
                //eg. Wednesday September 4 1986
                html = RdfTools.serializeDateHtml(localZone, language, utcDate).toString();

                break;

            case Time:
                LocalTime localTime = (LocalTime) value;
                ZonedDateTime utcTime = ZonedDateTime.ofInstant(ZonedDateTime.of(LocalDate.now(), localTime, localZone).toInstant(), UTC);
                content = utcTime.format(DateTimeFormatter.ISO_TIME);
                factEntryHtml.append(" data-gmt=\"false\"");

                //html = "1:00 AM<span class=\"timezone\">(UTC+01:00)</span>";
                html =
                                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(localZone).withLocale(language).format(utcTime) + "<span class=\"timezone\">(UTC" +
                                DateTimeFormatter.ofPattern("xxxxx").withZone(localZone).withLocale(language).format(utcTime) + ")</span>";

                break;

            case DateTime:
                LocalDateTime localDateTime = (LocalDateTime) value;
                ZonedDateTime utcDateTime = ZonedDateTime.ofInstant(ZonedDateTime.of(localDateTime, localZone).toInstant(), UTC);
                content = utcDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
                factEntryHtml.append(" data-gmt=\"false\"");

                //html = "Friday January 1, 2016 - 1:00 AM<span class=\"timezone\">(UTC+01:00)</span>";
                html = RdfTools.serializeDateTimeHtml(localZone, language, utcDateTime).toString();

                break;

            case Color:
                content = "#" + Integer.toHexString(((Color) value).getRGB()).substring(2);
                html = "<div class=\"" + gen.com.beligum.blocks.core.constants.blocks.core.Entries.INPUT_TYPE_COLOR_VALUE_CLASS.getValue() + "\" style=\"background-color: " + content + "\"></div>";
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
                Collection<AutocompleteSuggestion> enumSuggestion = property.getEndpoint().search(property, enumKey, RdfQueryEndpoint.QueryType.NAME, language, 1);
                if (enumSuggestion.size()==1) {
                    AutocompleteSuggestion enumValue = enumSuggestion.iterator().next();
                    addDataType = true;
                    content = enumValue.getValue();
                    html = RdfTools.serializeEnumHtml(enumValue).toString();
                }
                else {
                    throw new IOException("Unable to find enum value; " + enumKey);
                }

                break;

            case Resource:

                URI resourceId = (URI) value;
                ResourceInfo resourceInfo = property.getDataType().getEndpoint().getResource(property.getDataType(), resourceId, language);

                addDataType = false;
                factEntryHtml.append(" resource=\"" + resourceInfo.getResourceUri().toString() + "\"");
                if (resourceInfo != null) {
                    html = RdfTools.serializeResourceHtml(resourceInfo).toString();
                }
                else {
                    throw new IOException("Unable to find resource; " + resourceId);
                }

                break;
            default:
                throw new IOException("Encountered unimplemented widget type parser, please fix; " + property.getWidgetType());
        }

        if (addDataType) {
            factEntryHtml.append(" datatype=\"").append(property.getDataType().getCurieName()).append("\"");
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

    protected String buildResourceImageHtml(NamedUri uri)
    {
        StringBuilder retVal = new StringBuilder();

        retVal.append("<blocks-image class=\"bordered\">");
        retVal.append("<img property=\"image\" src=\"").append(uri.getUri().toString()).append("\" >");
        retVal.append("</blocks-image>");

        return retVal.toString();
    }

    //-----PRIVATE METHODS-----
    private LocalDateTime epochToLocalDateTime(long value)
    {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneId.systemDefault());
    }

    //-----INNER CLASSES-----
    protected class NamedUri
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

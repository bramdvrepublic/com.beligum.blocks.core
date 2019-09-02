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

package com.beligum.blocks.utils;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.*;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.utils.importer.ImportPropertyMapping;
import com.beligum.blocks.utils.importer.ImportResourceObject;
import gen.com.beligum.blocks.core.constants.blocks.core;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static gen.com.beligum.blocks.core.constants.blocks.core.WIDGET_TYPE_TIME_TZONE_CLASS;

/**
 * Created by wouter on 27/04/15.
 * <p/>
 * Simple functions to make the RDF life easier
 */
public class RdfTools
{
    //-----CONSTANTS-----
    // Simpleflake generates a Long id, based on timestamp
    private static final SimpleFlake SIMPLE_FLAKE = new SimpleFlake();
    private static final URI ROOT = URI.create("/");

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * Create an absolute resource based on the resource endpoint and a type.
     * Generate a new id-value
     * e.g. http://www.stralo.com/resource/877920329832560392
     */
    public static URI createAbsoluteResourceId(RdfResource entity)
    {
        return createAbsoluteResourceId(entity, new Long(RdfTools.SIMPLE_FLAKE.generate()).toString(), true);
    }

    /**
     * Create a local, relative (to the current root) resource based on the resource endpoint and a type.
     * Generate a new id-value
     * e.g. /resource/877920329832560392
     */
    public static URI createRelativeResourceId(RdfResource entity)
    {
        return createRelativeResourceId(entity, new Long(RdfTools.SIMPLE_FLAKE.generate()).toString(), true);
    }

    /**
     * Create a absolute resource id, based on the type and an existing id-value
     * e.g. http://www.republic.be/v1/resource/address/big-street-in-antwerp
     */
    public static URI createAbsoluteResourceId(RdfResource entity, String id)
    {
        return createAbsoluteResourceId(entity, id, false);
    }

    /**
     * Create a locale resource id, based on the type and an existing id-value
     * e.g. /v1/resource/address/big-street-in-antwerp
     */
    public static URI createRelativeResourceId(RdfResource entity, String id)
    {
        return createRelativeResourceId(entity, id, false);
    }

    /**
     * Returns true if the string looks like an URI, here to centralize this decision
     */
    public static boolean isUri(String uri)
    {
        return uri.contains(":");
    }

    /**
     * Returns true if the url is a CURIE, but a full-blown URI
     */
    public static boolean isCurie(URI uri)
    {
        //note: a CURIE:
        // - is absolute
        // - has it's prefix as scheme
        // - has a null path
        // - has a null host
        // - has it's suffix as schemeSpecificPart
        // So this test below checks if the URI is a CURIE
        return uri.isAbsolute() && uri.getHost() == null && uri.getPath() == null;
    }

    /**
     * Converts a URI to it's CURIE variant, using the locally known ontologies
     */
    public static URI fullToCurie(URI fullUri)
    {
        URI retVal = null;

        if (fullUri != null) {
            RdfResource resource = RdfFactory.lookup(fullUri);
            if (resource != null) {
                retVal = resource.getCurie();
            }
        }

        if (retVal == null) {
            Logger.warn("Encountered unknown ontology member, returning null for; " + fullUri);
        }

        return retVal;
    }

    /**
     * Convenience wrapper for IRI
     */
    public static URI fullToCurie(IRI fullIri)
    {
        return RdfTools.fullToCurie(RdfTools.iriToUri(fullIri));
    }

    /**
     * Converts a CURIE to a full URI, using the locally known ontologies
     */
    public static URI curieToFull(URI curie)
    {
        //if we find nothing, we return null, which kind of makes sense to indicate an setRollbackOnly
        URI retVal = null;

        RdfOntology ontology = RdfFactory.getOntology(curie.getScheme());
        if (ontology != null) {
            retVal = ontology.resolve(curie.getSchemeSpecificPart());
        }
        else {
            Logger.warn("Encountered unknown curie schema, returning null for; " + curie);
        }

        return retVal;
    }

    /**
     * Convenience wrapper for IRI
     */
    public static URI curieToFull(IRI fullIri)
    {
        return RdfTools.curieToFull(RdfTools.iriToUri(fullIri));
    }

    /**
     * Make the URI relative to the locally configured domain if it's absolute (or just return it if it's not)
     */
    public static URI relativizeToLocalDomain(URI uri)
    {
        URI retVal = uri;

        if (uri.isAbsolute()) {
            //Note: if the url is not relative to the siteDomain url, the url is simply returned
            URI relative = R.configuration().getSiteDomain().relativize(retVal);
            //if it's not absolute (eg. it doesn't start with http://..., this means the relativize 'succeeded' and the retVal starts with the RDF ontology URI)
            if (!relative.isAbsolute()) {
                retVal = ROOT.resolve(relative);
            }
        }

        return retVal;
    }

    /**
     * Standard boolean parsing is too restrictive
     */
    public static boolean parseRdfaBoolean(String value)
    {
        Boolean retval = false;

        if ("1".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) ||
            "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
            retval = Boolean.TRUE;
        }

        return retval;
    }

    /**
     * Generate a RDFa-compatible HTML string from the supplied date
     */
    public static CharSequence serializeDateHtml(ZoneId zone, Locale language, TemporalAccessor utcDateTime)
    {
        return new StringBuilder()
                        .append(DateTimeFormatter.ofPattern("cccc").withZone(zone).withLocale(language).format(utcDateTime))
                        .append(" ")
                        .append(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withZone(zone).withLocale(language).format(utcDateTime));
    }

    /**
     * Generate a RDFa-compatible HTML string from the supplied time
     */
    public static CharSequence serializeTimeHtml(ZoneId zone, Locale language, TemporalAccessor utcDateTime)
    {
        return new StringBuilder()
                        .append(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).withLocale(language).format(utcDateTime))
                        .append("<span class=\"").append(WIDGET_TYPE_TIME_TZONE_CLASS).append("\">(UTC")
                        .append(DateTimeFormatter.ofPattern("xxxxx").withZone(zone).withLocale(language).format(utcDateTime))
                        .append(")</span>");
    }
    public static CharSequence serializeObjectHtml(ImportResourceObject importResourceObject, Locale language) throws IOException, URISyntaxException
    {
        //get main property of the ResourceObject
        RdfProperty mainProperty = RdfFactory.getClass(importResourceObject.getResourceType()).getMainProperty();
        StringBuilder objectHtml = new StringBuilder();
        Iterator<ImportPropertyMapping> it = importResourceObject.getRdfProperties().iterator();

        while (it.hasNext()) {
            String contentString = null;
            String value = null;
            String resourceString = null;
            ImportPropertyMapping importPropertyMapping = it.next();
            RdfProperty rdfProperty = RdfFactory.getProperty(importPropertyMapping.getRdfPropertyCurieName());
            if (mainProperty != null && rdfProperty.equals(mainProperty)) {
                //this is the mainproperty, add css "main" class to div
                objectHtml.append("<div class=\"main\"><label>");
            }
            else {
                //div without css "main class
                objectHtml.append("<div><label>");
            }

            objectHtml.append(rdfProperty.getLabelMessage().toString(language));
            objectHtml.append("</label>");
            boolean addDataType = true;
            switch (rdfProperty.getWidgetType()) {
                case Editor:
                case InlineEditor:
                    value = StringEscapeUtils.escapeHtml(importPropertyMapping.getRdfPropertyValue());
                    break;
                case Enum:
                    Iterable<ResourceProxy> enumSuggestion = rdfProperty.getEndpoint().search(rdfProperty, importPropertyMapping.getRdfPropertyValue(), RdfEndpoint.QueryType.NAME, language, 1);
                    Iterator<ResourceProxy> iter = enumSuggestion.iterator();
                    if (iter.hasNext()) {
                        ResourceProxy enumValue = iter.next();
                        addDataType = true;
                        contentString = "\" content=\"" + enumValue.getResource();
                        //                        content = enumValue.getResource();
                        value = RdfTools.serializeEnumHtml(enumValue).toString();
                        if (value.equalsIgnoreCase("null")) {
                            throw new IOException("null value found for enum. This shouldn't happen");
                        }
                    }
                    else {
                        throw new IOException("Unable to find enum value; ");
                    }
                    break;
                case Date:
                    ZoneId localZone = ZoneId.systemDefault();
                    Object retVal = null;
                    if (NumberUtils.isNumber(importPropertyMapping.getRdfPropertyValue())) {
                        retVal = epochToLocalDateTime(Long.parseLong(importPropertyMapping.getRdfPropertyValue())).toLocalDate();
                    }
                    else {
                        retVal = LocalDate.parse(importPropertyMapping.getRdfPropertyValue());
                    }
                    TemporalAccessor utcDate;
//                    if (retVal instanceof LocalDate) {
//                        utcDate = ZonedDateTime.ofInstant(((LocalDate) retVal).atStartOfDay(localZone).toInstant(), UTC);
//                    }
//                    else {
//                        utcDate = (TemporalAccessor) retVal;
//                    }
                    utcDate = (TemporalAccessor) retVal;
                    value = RdfTools.serializeDateHtml(localZone, language, utcDate).toString();
                    contentString = "\" content=\"" + DateTimeFormatter.ISO_LOCAL_DATE.format(utcDate);
                    break;
                //                case ResourceList:
                case Resource:
                    try{
                        addDataType = false;
                        URI resourceId = new URI(importPropertyMapping.getRdfPropertyValue());
                        ResourceProxy resourceInfo = rdfProperty.getDataType().getEndpoint().getResource(rdfProperty.getDataType(), resourceId, language);
                        resourceString = "\" resource=\"" + resourceInfo.getResource();
                        value = "<a href=\""+resourceInfo.getResource().toString()+"\">"+resourceInfo.getLabel()+"</a> ";
                    }catch (NullPointerException ex){
                        throw ex;
                    }



//                    value = "<a href=\"" + resourceId + "\">" + resourceIndexEntry == null || resourceIndexEntry.getLabel() == null ? resourceId.toString() : resourceIndexEntry.getLabel() + "</a> ";
                    break;
                case Uri:
                    addDataType = false;
                    // We need to also add the hyperlink href as a property-value, because when we wrap the <a> tag with a <div property=""> tag,
                    // the content of the property tag (eg. the entire <a> tag) gets serialized by the RDFa parser as a I18N-string, using the human readable
                    // text of the hyperlink as a value (instead of using the href value and serializing it as a URI). This is because the property attribute is set on the
                    // wrapping <div> instead of on the <a> tag.
                    //Note: from the RDFa docs: "@content is used to indicate the value of a plain literal", and since it's a URI, we add it as a resource value
                    value = "<a href=\"" + importPropertyMapping.getRdfPropertyValue() + "\"";

                    value += ">" + importPropertyMapping.getRdfPropertyValue() + "</a>";

                    break;
                case Number:
                    value = importPropertyMapping.getRdfPropertyValue();
                    contentString = "\" content=\"" + importPropertyMapping.getRdfPropertyValue().toString();
                    break;
                //duration
                /**
                 *   <div data-property="name">duration</div>
                 *     <div data-property="value">
                 *         <div class="property object" typeof="crb:Duration" property="crb:duration">
                 *         <div><label>manual duration</label>
                 *         <div class="property duration" property="crb:manualDuration" datatype="xsd:long" content="90061001">1 day, 1 hour, 1 minute, 1 second, 1 undefined</div></div>
                 *         </div>
                 *     </div>
                 */
                //TODO: check if this works
                case Duration:
                    long millitime = 0;
                    try{
                        millitime = Long.valueOf(importPropertyMapping.getRdfPropertyValue());
                    }catch (NumberFormatException ex){

                    }
                    addDataType = true;
                    long dayz = TimeUnit.MILLISECONDS.toDays(millitime);
                    millitime -= TimeUnit.DAYS.toMillis(dayz);
                    long hourz = TimeUnit.MILLISECONDS.toHours(millitime);
                    millitime -= TimeUnit.HOURS.toMillis(hourz);
                    long minutez = TimeUnit.MILLISECONDS.toMinutes(millitime);
                    millitime -= TimeUnit.MINUTES.toMillis(minutez);
                    long secondz = TimeUnit.MILLISECONDS.toSeconds(millitime);
                    millitime -= TimeUnit.SECONDS.toMillis(secondz);

                    StringBuilder stringb = new StringBuilder();
                    //FIXME. do not hard code this
                    //problem: these are in the fact module
                    if(dayz > 0){
                        stringb.append(dayz);
                        stringb.append(" days ");
                    }
                    if(hourz > 0){
                        stringb.append(hourz);
                        stringb.append(" hours ");
                    }
                    if(minutez > 0){
                        stringb.append(minutez);
                        stringb.append(" minutes ");
                    }
                   if(secondz > 0){
                       stringb.append(secondz);
                       stringb.append(" seconds ");
                   }
//                    stringb.append(millitime);
//                    stringb.append(" milliseconds");
                    contentString = "\" content=\"" + importPropertyMapping.getRdfPropertyValue();
                    value = stringb.toString();
                    break;

                    /**
                     *     <blocks-fact-entry>
                     *
                     *     <div data-property="name"> duration </div>
                     *     <div data-property="value">
                     *      <div class="property object" typeof="crb:Duration" resource="/resource/1134456079732598221" property="crb:duration"><div>
                     *         <label>manual duration</label>
                     *         <div class="property duration" property="crb:manualDuration" datatype="xsd:long" content="32201.003">08:56:41:03</div>
                     *         </div></div>
                     *      </div></blocks-fact-entry>
                     */
                    //TODO: check if this works
                case Timecode:
                    String valString =  importPropertyMapping.getRdfPropertyValue().replaceAll(",",".");
                    String[] strValues = String.valueOf(importPropertyMapping.getRdfPropertyValue()).split("\\.");
                    long timeInSeconds = 0;
                    try{
                        timeInSeconds = Long.valueOf(strValues[0]);
                    }catch (NumberFormatException ex){

                    }
                    addDataType = true;
                    long hours = TimeUnit.SECONDS.toHours(timeInSeconds);
                    timeInSeconds -= TimeUnit.HOURS.toSeconds(hours);
                    long minutes = TimeUnit.SECONDS.toMinutes(timeInSeconds);
                    timeInSeconds -= TimeUnit.MINUTES.toSeconds(minutes);

                    StringBuilder sb = new StringBuilder();
                    sb.append(hours);
                    sb.append(":");
                    sb.append(minutes);
                    sb.append(":");
                    sb.append(timeInSeconds);
                    sb.append(":");
                    sb.append(strValues[1]);
                    contentString = "\" content=\"" + importPropertyMapping.getRdfPropertyValue();
                    value = sb.toString();
                    break;
                default:
                    addDataType = false;
                    value = importPropertyMapping.getRdfPropertyValue();
            }
            objectHtml.append("<div class=\"property ");
            objectHtml.append((RdfFactory.getProperty(importPropertyMapping.getRdfPropertyCurieName())).getWidgetType().getConstant());
            objectHtml.append("\" property=\"");
            objectHtml.append((RdfFactory.getProperty(importPropertyMapping.getRdfPropertyCurieName())).toString());
            if (rdfProperty.getDataType().equals(RDF.langString)) {
                //see the comments in blocks-fact-entry.js and RDF.LANGSTRING for why we remove the datatype in case of a rdf:langString
                addDataType = false;
            }
            if (addDataType) {
                objectHtml.append("\" datatype=\"").append(rdfProperty.getDataType().getCurie());
            }
            if (!StringUtils.isEmpty(contentString)) {
                objectHtml.append(contentString);
            }

            if (!StringUtils.isEmpty(resourceString)) {
                objectHtml.append(resourceString);
            }
            objectHtml.append("\">");

            objectHtml.append(value);
            objectHtml.append(" </div></div>");
        }
        return objectHtml.toString();
    }

    /**
     * Generate a RDFa-compatible HTML string from the supplied date and time
     */
    public static CharSequence serializeDateTimeHtml(ZoneId zone, Locale language, TemporalAccessor utcDateTime)
    {
        return new StringBuilder()
                        .append(DateTimeFormatter.ofPattern("cccc").withZone(zone).withLocale(language).format(utcDateTime))
                        .append(" ")
                        .append(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withZone(zone).withLocale(language).format(utcDateTime))
                        .append(" - ")
                        .append(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).withLocale(language).format(utcDateTime))
                        .append("<span class=\"").append(WIDGET_TYPE_TIME_TZONE_CLASS).append("\">(UTC")
                        .append(DateTimeFormatter.ofPattern("xxxxx").withZone(zone).withLocale(language).format(utcDateTime))
                        .append(")</span>");
    }

    /**
     * Generate a RDFa-compatible HTML string from the supplied enum.
     */
    public static CharSequence serializeEnumHtml(ResourceProxy enumValue)
    {
        // <p> is consistent with JS
        return new StringBuilder()
                        .append("<p>")
                        .append(enumValue.getLabel())
                        .append("</p>");
    }

    /**
     * Generate a RDFa-compatible HTML string from the supplied resource info
     */
    public static CharSequence serializeResourceHtml(RdfProperty rdfProperty, ResourceProxy resourceProxy)
    {
        CharSequence retVal = null;

        StringBuilder labelHtml = new StringBuilder();
        labelHtml.append(resourceProxy.getLabel());
        boolean disableImg = false;
        if (rdfProperty.getWidgetConfig() != null && rdfProperty.getWidgetConfig().containsKey(core.Entries.WIDGET_CONFIG_RESOURCE_ENABLE_IMG)) {
            disableImg = !Boolean.valueOf(rdfProperty.getWidgetConfig().get(core.Entries.WIDGET_CONFIG_RESOURCE_ENABLE_IMG));
        }
        if (resourceProxy.getImage() != null && !disableImg) {
            //Note: title is for a tooltip
            labelHtml.append("<img src=\"").append(resourceProxy.getImage()).append("\" alt=\"").append(resourceProxy.getLabel()).append("\" title=\"").append(resourceProxy.getLabel()).append("\">");
        }

        boolean disableHref = false;
        if (rdfProperty.getWidgetConfig() != null && rdfProperty.getWidgetConfig().containsKey(core.Entries.WIDGET_CONFIG_RESOURCE_ENABLE_HREF)) {
            disableHref = !Boolean.valueOf(rdfProperty.getWidgetConfig().get(core.Entries.WIDGET_CONFIG_RESOURCE_ENABLE_HREF));
        }
        if (resourceProxy.getUri() != null && !disableHref) {
            StringBuilder linkHtml = new StringBuilder();
            linkHtml.append("<a href=\"").append(resourceProxy.getUri()).append("\"");
            if (resourceProxy.isExternal() || resourceProxy.getUri().isAbsolute()) {
                linkHtml.append(" target=\"_blank\"");
            }
            linkHtml.append(">").append(labelHtml).append("</a>");

            retVal = linkHtml;
        }
        else {
            retVal = labelHtml;
        }

        return retVal;
    }

    /**
     * Converts an IRI to an URI
     */
    public static URI iriToUri(IRI iri)
    {
        return iri == null ? null : URI.create(iri.toString());
    }

    /**
     * Converts an URI to an IRI
     */
    public static IRI uriToIri(URI uri)
    {
        return uri == null ? null : SimpleValueFactory.getInstance().createIRI(uri.toString());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * turn Epoch to LocalDateTime
     *
     * @param value
     * @return
     */
    private static LocalDateTime epochToLocalDateTime(long value)
    {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneId.systemDefault());
    }
    /**
     * Small wrapper to make all absolute call pass through here
     */
    private static URI createAbsoluteResourceId(RdfResource entity, String id, boolean ontologyUniqueId)
    {
        return createResourceIdPath(UriBuilder.fromUri(R.configuration().getSiteDomain()), entity, id, ontologyUniqueId).build();
    }

    /**
     * Small wrapper to make all relative call pass through here
     */
    private static URI createRelativeResourceId(RdfResource entity, String id, boolean ontologyUniqueId)
    {
        return createResourceIdPath(UriBuilder.fromUri("/"), entity, id, ontologyUniqueId).build();
    }

    /**
     * Central method for uniform resource path creation.
     * Note: the ontologyUniqueId flag indicates the id is (always) unique to the entire ontology
     * if it is, we're can resolve a resource using only this id
     * if it's not, we need additional information to resolve the resource (eg. coming from a SQL primary key or linking to an external ontology)
     * see this discussion https://github.com/republic-of-reinvention/com.stralo.framework/issues/15
     */
    private static UriBuilder createResourceIdPath(UriBuilder uriBuilder, RdfResource entity, String id, boolean ontologyUniqueId)
    {
        //this is the constant factor for all resource ID's and needs to be synced with isResourceUrl() and extractResourceId()
        uriBuilder.path(Settings.RESOURCE_ENDPOINT);

        if (!ontologyUniqueId && entity != null) {
            uriBuilder.path(entity.getName());
        }

        uriBuilder.path(id);

        return uriBuilder;
    }

    //-----INNER CLASSES-----

    /**
     * This is a convenient wrapper class that wraps the parsing of RDF resource URIs and caches it's result
     * A resource URI has form /resource/<id> or /resource/<class>/<id>
     * For details, see https://github.com/republic-of-reinvention/com.stralo.framework/issues/15
     */
    public static class RdfResourceUri
    {
        private URI uri;
        private Path path;
        private RdfClass resourceClass;
        private String resourceId;
        private boolean prefixed = false;
        private boolean valid = false;

        public RdfResourceUri(URI uri)
        {
            this.uri = uri;

            if (this.uri != null && this.uri.getPath() != null) {

                //split the path into parts
                this.path = Paths.get(this.uri.getPath());

                if (this.path.startsWith(Settings.RESOURCE_ENDPOINT)) {

                    this.prefixed = true;

                    switch (this.path.getNameCount()) {
                        case 2:
                            this.resourceId = this.path.getName(1).toString();
                            this.valid = StringUtils.isNotEmpty(this.resourceId);

                            break;

                        case 3:
                            //this will add support for curies or just the name of the class in the default ontology
                            String className = this.path.getName(1).toString();

                            RdfResource resource = RdfFactory.lookup(className);
                            if (resource != null && resource instanceof RdfClass) {
                                this.resourceClass = (RdfClass) resource;
                            }
                            else {
                                Logger.error("Error while translating the class of a resource uri (" + className + ") to a valid RDF member." +
                                             " Could translate it to a valid resource, but it doesn't seem to be a RdfClass, but a " +
                                             " This shouldn't happen; " + resource);
                            }

                            this.resourceId = this.path.getName(2).toString();
                            this.valid = this.resourceClass != null && StringUtils.isNotEmpty(this.resourceId);

                            break;
                    }
                }
            }
        }

        /**
         * The raw URI as it was supplied in the first place
         */
        public URI getUri()
        {
            return uri;
        }
        /**
         * The parsed path of the raw URI (split into parts for parsing)
         */
        public Path getPath()
        {
            return path;
        }
        /**
         * The detected resource class in case of a typed resource URI or null in case of unsuccessful parsing or an ID-only URI
         */
        public RdfClass getResourceClass()
        {
            return resourceClass;
        }
        /**
         * The extracted ID or null in case of unsuccessful parsing
         */
        public String getResourceId()
        {
            return resourceId;
        }
        /**
         * True if the supplied URI starts with the correct /resource/ prefix, but may otherwise be invalid (see below)
         */
        public boolean isPrefixed()
        {
            return prefixed;
        }
        /**
         * True if the supplied URI was a lexicographical valid resource URI consisting of 2 or 3 parts (may not exist though)
         */
        public boolean isValid()
        {
            return valid;
        }
        /**
         * This only returns true in case the 2nd part in a 3-parts resource URI resolved to a type (see GitHub discussion)
         */
        public boolean isTyped()
        {
            return this.getResourceClass() != null;
        }

        //-----MGMT FUNCTIONS-----
        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof RdfResourceUri)) return false;

            RdfResourceUri that = (RdfResourceUri) o;

            return getUri() != null ? getUri().equals(that.getUri()) : that.getUri() == null;
        }
        @Override
        public int hashCode()
        {
            return getUri() != null ? getUri().hashCode() : 0;
        }
        @Override
        public String toString()
        {
            return "" + this.getUri();
        }
    }
}

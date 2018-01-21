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
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.filesystem.pages.PageModel;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;
import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.beligum.base.server.R.configuration;
import static gen.com.beligum.blocks.core.constants.blocks.core.INPUT_TYPE_TIME_TZONE_CLASS;

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
    public static URI createAbsoluteResourceId(RdfClass entity)
    {
        return createAbsoluteResourceId(entity, new Long(RdfTools.SIMPLE_FLAKE.generate()).toString(), true);
    }

    /**
     * Create a local, relative (to the current root) resource based on the resource endpoint and a type.
     * Generate a new id-value
     * e.g. /resource/877920329832560392
     */
    public static URI createRelativeResourceId(RdfClass entity)
    {
        return createRelativeResourceId(entity, new Long(RdfTools.SIMPLE_FLAKE.generate()).toString(), true);
    }

    /**
     * Create a absolute resource id, based on the type and an existing id-value
     * e.g. http://www.republic.be/v1/resource/address/big-street-in-antwerp
     */
    public static URI createAbsoluteResourceId(RdfClass entity, String id)
    {
        return createAbsoluteResourceId(entity, id, false);
    }

    /**
     * Create a locale resource id, based on the type and an existing id-value
     * e.g. /v1/resource/address/big-street-in-antwerp
     */
    public static URI createRelativeResourceId(RdfClass entity, String id)
    {
        return createRelativeResourceId(entity, id, false);
    }

    /**
     * Converts a URI to it's CURIE variant, using the locally known ontologies
     */
    public static URI fullToCurie(URI fullUri)
    {
        URI retVal = null;

        if (fullUri != null) {
            URI relative = Settings.instance().getRdfOntologyUri().relativize(fullUri);
            //if it's not absolute (eg. it doesn't start with http://..., this means the relativize 'succeeded' and the retVal starts with the RDF ontology URI)
            if (!relative.isAbsolute()) {
                retVal = URI.create(Settings.instance().getRdfOntologyPrefix() + ":" + relative.toString());
            }
        }

        return retVal;
    }

    /**
     * Converts a CURIE to a full URI
     */
    public static URI curieToFull(URI resourceTypeCurie)
    {
        //if we find nothing, we return null, which kind of makes sense to indicate an setRollbackOnly
        URI retVal = null;

        RdfVocabulary vocab = RdfFactory.getVocabularyForPrefix(resourceTypeCurie.getScheme());
        if (vocab != null) {
            retVal = vocab.resolve(resourceTypeCurie.getPath());
        }
        else {
            Logger.warn("Encountered unknown curie schema, returning null for; " + resourceTypeCurie);
        }

        return retVal;
    }

    /**
     * Make the URI relative to the locally configured domain if it's absolute (or just return it if it's not)
     */
    public static URI relativizeToLocalDomain(URI uri)
    {
        URI retVal = uri;

        if (uri.isAbsolute()) {
            //Note: if the url is not relative to the siteDomain url, the url is simply returned
            URI relative = configuration().getSiteDomain().relativize(retVal);
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
                        .append("<span class=\"").append(INPUT_TYPE_TIME_TZONE_CLASS).append("\">(UTC")
                        .append(DateTimeFormatter.ofPattern("xxxxx").withZone(zone).withLocale(language).format(utcDateTime))
                        .append(")</span>");
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
                        .append("<span class=\"").append(INPUT_TYPE_TIME_TZONE_CLASS).append("\">(UTC")
                        .append(DateTimeFormatter.ofPattern("xxxxx").withZone(zone).withLocale(language).format(utcDateTime))
                        .append(")</span>");
    }

    /**
     * Generate a RDFa-compatible HTML string from the supplied enum.
     */
    public static CharSequence serializeEnumHtml(AutocompleteSuggestion enumValue)
    {
        // <p> is consistent with JS
        return new StringBuilder()
                        .append("<p>")
                        .append(enumValue.getTitle())
                        .append("</p>");
    }

    /**
     * Generate a RDFa-compatible HTML string from the supplied resource info
     */
    public static CharSequence serializeResourceHtml(ResourceInfo resourceInfo)
    {
        CharSequence retVal = null;

        StringBuilder labelHtml = new StringBuilder();
        labelHtml.append(resourceInfo.getLabel());
        if (resourceInfo.getImage() != null) {
            //Note: title is for a tooltip
            labelHtml.append("<img src=\"").append(resourceInfo.getImage()).append("\" alt=\"").append(resourceInfo.getLabel()).append("\" title=\"").append(resourceInfo.getLabel()).append("\">");
        }

        if (resourceInfo.getLink() != null) {
            StringBuilder linkHtml = new StringBuilder();
            linkHtml.append("<a href=\"").append(resourceInfo.getLink()).append("\"");
            if (resourceInfo.isExternalLink() || resourceInfo.getLink().isAbsolute()) {
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
     * This analyzes the RDF model of the page, detects and splits all sub-resource models,
     * mapped by their subject IRI, meaning all returned resource models will have the same subject IRI.
     * The returned map is indexed by id (of the subresource), so it's easily fetched from/compared to existing index entries.
     * Note that since sub-object support was implemented for pages
     * (since January 2018), a page can contain multiple resources.
     * This method returns a sorted map, ready for indexation, meaning the sub-resources come before
     * the main page resource (so sub-resource lookups will resolve).
     */
    public static Map<String, PageModel> extractSubModels(Page page) throws IOException
    {
        //Note: instead of implementing a custom sorted TreeMap, we'll use a simple LinkedHashMap
        //that retain insertion order and postpone the insertion of the main resource (see below)
        Map<String, PageModel> retVal = new LinkedHashMap<>();

        Model pageRdfModel = page.readRdfModel();

        //Note that page resources are relative, so make sure it's absolute
        URI mainResource = URI.create(page.createAnalyzer().getHtmlAbout().value);
        if (!mainResource.isAbsolute()) {
            mainResource = R.configuration().getSiteDomain().resolve(mainResource);
        }

        PageModel mainModel = null;

        //we iterate all different subjects in this page and filter out the ones we're not interested in,
        //then "zoom-in" on the different sub-models
        for (Resource subject : pageRdfModel.subjects()) {

            //note that we need to filter out some general triples (like the "rdfa:usesVocabulary" statements)
            //by ignoring all statements about the page itself; we're only interested in the resources this page is talking about
            if (!subject.toString().equals(page.getPublicAbsoluteAddress().toString())) {

                //"zoom-in" on the specific subject
                Model subModel = pageRdfModel.filter(subject, null, null);
                URI subResource = RdfTools.iriToUri((IRI) subject);

                //while we're parsing the rdf graph, we might as well extract the type
                Optional<IRI> typeOfIRI = Models.objectIRI(subModel.filter(subject, RDF.TYPE, null));
                RdfClass subType = !typeOfIRI.isPresent() ? null : RdfFactory.getClassForResourceType(RdfTools.fullToCurie(RdfTools.iriToUri(typeOfIRI.get())));

                PageModel modelInfo = new PageModel(page, mainResource, subResource, subType, subModel);

                //if we encounter the main resource, save it for later and insert it last so sub-resources come first
                if (modelInfo.isMain()) {
                    mainModel = modelInfo;
                }
                else {
                    retVal.put(modelInfo.getId(), modelInfo);
                }
            }
        }

        if (mainModel != null) {
            retVal.put(mainModel.getId(), mainModel);
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

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Small wrapper to make all absolute call pass through here
     */
    private static URI createAbsoluteResourceId(RdfClass entity, String id, boolean ontologyUniqueId)
    {
        return createResourceIdPath(UriBuilder.fromUri(configuration().getSiteDomain()), entity, id, ontologyUniqueId).build();
    }

    /**
     * Small wrapper to make all relative call pass through here
     */
    private static URI createRelativeResourceId(RdfClass entity, String id, boolean ontologyUniqueId)
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
    private static UriBuilder createResourceIdPath(UriBuilder uriBuilder, RdfClass entity, String id, boolean ontologyUniqueId)
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
                            this.resourceClass = RdfFactory.getClassForResourceType(URI.create(Settings.instance().getRdfOntologyPrefix() + ":" + this.path.getName(1).toString()));
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

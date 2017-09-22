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

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.rdf.ifaces.RdfClass;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Locale;

import static com.beligum.base.server.R.configuration;
import static gen.com.beligum.blocks.core.constants.blocks.core.INPUT_TYPE_TIME_TZONE_CLASS;

/**
 * Created by wouter on 27/04/15.
 * <p/>
 * Simple functions to make the RDF life easier
 */
public class RdfTools
{
    // Simpleflake generates a Long id, based on timestamp
    public static final SimpleFlake SIMPLE_FLAKE = new SimpleFlake();
    public static HashMap<URI, HashMap<String, URI>> urlcache = new HashMap<URI, HashMap<String, URI>>();
    private static final URI ROOT = URI.create("/");

    /**
     * Create an absolute resource based on the resource endpoint and a type.
     * Generate a new id-value
     * e.g. http://www.republic.be/v1/resource/address/156465
     */
    public static URI createAbsoluteResourceId(RdfClass entity)
    {
        return createAbsoluteResourceId(entity, new Long(RdfTools.SIMPLE_FLAKE.generate()).toString());
    }

    /**
     * Create a local, relative (to the current root) resource based on the resource endpoint and a type.
     * Generate a new id-value
     * e.g. /v1/resource/address/156465
     */
    public static URI createRelativeResourceId(RdfClass entity)
    {
        return createRelativeResourceId(entity, new Long(RdfTools.SIMPLE_FLAKE.generate()).toString());
    }

    /**
     * Create a absolute resource id, based on the type and an existing id-value
     * e.g. http://www.republic.be/v1/resource/address/big-street-in-antwerp
     */
    public static URI createAbsoluteResourceId(RdfClass entity, String id)
    {
        return UriBuilder.fromUri(configuration().getSiteDomain()).path(Settings.RESOURCE_ENDPOINT).path(entity.getName()).path(id).build();
    }

    /**
     * Create a locale resource id, based on the type and an existing id-value
     * e.g. /v1/resource/address/big-street-in-antwerp
     */
    public static URI createRelativeResourceId(RdfClass entity, String id)
    {
        return UriBuilder.fromUri("/").path(Settings.RESOURCE_ENDPOINT).path(entity.getName()).path(id).build();
    }

    /**
     * Extracts the last part (the real ID) from a resource URI.
     * Only returns non-null if the supplied URI is a valid resource URI
     * Note that the resource itself may not exist though, this extraction is just lexicographical.
     */
    public static String extractResourceId(URI resourceUri)
    {
        String retVal = null;

        if (isResourceUrl(resourceUri)) {
            //a resource URI has form /resource/<type>/<id> so third part is the ID
            retVal = Paths.get(resourceUri.getPath()).getName(2).toString();
        }

        return retVal;
    }

    /**
     * Determines if the supplied URL is a valid resource URL (might not exist though)
     */
    public static boolean isResourceUrl(URI uri)
    {
        boolean retVal = false;

        if (uri != null && uri.getPath() != null) {
            Path path = Paths.get(uri.getPath());
            //A bit conservative: we need three segments: the word 'resource', the type of the resource and the ID
            if (path.getNameCount() == 3 && path.startsWith(Settings.RESOURCE_ENDPOINT)) {
                retVal = true;
            }
        }

        return retVal;
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
    public static CharSequence serializeDateHtml(ZoneId zone, Locale language, TemporalAccessor utcDateTime)
    {
        return new StringBuilder()
                        .append(DateTimeFormatter.ofPattern("cccc").withZone(zone).withLocale(language).format(utcDateTime))
                        .append(" ")
                        .append(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withZone(zone).withLocale(language).format(utcDateTime));
    }
    public static CharSequence serializeTimeHtml(ZoneId zone, Locale language, TemporalAccessor utcDateTime)
    {
        return new StringBuilder()
                        .append(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).withLocale(language).format(utcDateTime))
                        .append("<span class=\"").append(INPUT_TYPE_TIME_TZONE_CLASS).append("\">(UTC")
                        .append(DateTimeFormatter.ofPattern("xxxxx").withZone(zone).withLocale(language).format(utcDateTime))
                        .append(")</span>");
    }
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
    public static CharSequence serializeEnumHtml(AutocompleteSuggestion enumValue)
    {
        // <p> is consistent with JS
        return new StringBuilder()
                        .append("<p>")
                        .append(enumValue.getTitle())
                        .append("</p>");
    }
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
}

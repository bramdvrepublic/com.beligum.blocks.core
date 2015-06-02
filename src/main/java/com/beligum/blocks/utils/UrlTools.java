package com.beligum.blocks.utils;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import org.apache.http.client.utils.URIBuilder;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Created by wouter on 27/04/15.
 */
public class UrlTools
{

    public static SimpleFlake simpleFlake = new SimpleFlake();

    public static String createLocalType(String type) {
        return makeAbsolute(BlocksConfig.instance().getDefaultRdfSchema().toString(), type);
    }

    public static String createLocalResourceId(String type, String id) {
        return UriBuilder.fromUri(BlocksConfig.instance().getSiteDomain()).path(ParserConstants.RESOURCE_ENDPOINT).path(type.toLowerCase()).path(id).build().toString();
    }

    public static String createLocalResourceId(String type) {
        return createLocalResourceId(type, new Long(UrlTools.simpleFlake.generate()).toString());
    }


    public static String makeAbsolute(String host, String relativePath) {
        host = host.trim();
        if (!host.endsWith("/") && !host.endsWith("#") && !host.endsWith(":"))
            host += "/";
        if (relativePath.startsWith("/"))
            relativePath = relativePath.substring(1);
        return host + relativePath;
    }



}

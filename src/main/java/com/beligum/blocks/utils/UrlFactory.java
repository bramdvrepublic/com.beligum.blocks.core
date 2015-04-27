package com.beligum.blocks.utils;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;

/**
 * Created by wouter on 27/04/15.
 */
public class UrlFactory
{

    public static String createLocalType(String type) {
        return makeAbsolute(Blocks.config().getDefaultRdfSchema(), type);
    }

    public static String createLocalResourceId(String type, String id) {
        return makeAbsolute(Blocks.config().getSiteDomain(), ParserConstants.RESOURCE_ENDPOINT + "/" + type + "/" + id);
    }

    public static String createSiteUrl(String path) {
        return makeAbsolute(Blocks.config().getSiteDomain(), path);
    }

    public static String makeAbsoluteUrl(String relativePath) {
        return makeAbsolute(Blocks.config().getSiteDomain(), relativePath);
    }


    public static String makeAbsolute(String host, String relativePath) {
        host = host.trim();
        if (!host.endsWith("/") && !host.endsWith("#") && !host.endsWith(":")) host += "/";
        if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
        return host + relativePath;
    }

}

package com.beligum.blocks.utils;

import com.beligum.blocks.base.Blocks;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wouter on 2/04/15.
 */
public class URLFactory
{

    public static final Pattern absoluteUrl = Pattern.compile(".*://.*");

    public static URL createURL(String spec) throws MalformedURLException
    {
        URL retVal = null;
        Matcher m = URLFactory.absoluteUrl.matcher(spec);
        if (m.find()) {
            retVal = new URL(spec);
        } else {
            retVal = new URL(URLFactory.makeAbsoluteUrl(spec));
        }
        return retVal;
    }

    public static String makeAbsoluteUrl(String relativePath) {
        return makeAbsolute(Blocks.config().getSiteDomain(), relativePath);
    }

    public static String makeAbsoluteRdfValue(String relativePath) {
        return makeAbsolute(Blocks.config().getDefaultRdfSchema(), relativePath);
    }

    public static String makeAbsolute(String host, String relativePath) {
        if (host.lastIndexOf("/") != host.length()) host += "/";
        if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
        return host + relativePath;
    }
}

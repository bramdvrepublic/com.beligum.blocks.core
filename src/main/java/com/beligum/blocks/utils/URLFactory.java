package com.beligum.blocks.utils;

import com.beligum.blocks.base.Blocks;
import sun.jvm.hotspot.opto.Block;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by wouter on 2/04/15.
 */
public class URLFactory
{


    public static URL createURL(String spec) throws MalformedURLException
    {
        URL retVal = null;
        if (spec.contains("://")) {
            retVal = new URL(spec);
        } else {
            retVal = new URL(URLFactory.makeAbsoluteUrl(spec));
        }
        return retVal;
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

package com.beligum.blocks.utils;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import org.apache.http.client.utils.URIBuilder;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

/**
 * Created by wouter on 27/04/15.
 */
public class UrlTools
{

    public static SimpleFlake simpleFlake = new SimpleFlake();

    public static String createLocalType(String type) {
        return makeAbsolute(Blocks.config().getDefaultRdfSchema().toString(), type);
    }

    public static String createLocalResourceId(String type, String id) {
        return UriBuilder.fromUri(Blocks.config().getSiteDomain()).path(ParserConstants.RESOURCE_ENDPOINT).path(type.toLowerCase()).path(id).build().toString();
    }

    public static String createLocalResourceId(String type) {
        return createLocalResourceId(type, new Long(UrlTools.simpleFlake.generate()).toString());
    }

    public static String createSiteUrl(String path) {
        return makeAbsolute(Blocks.config().getSiteDomain().toString(), path);
    }

    public static String makeAbsoluteUrl(String relativePath) {
        return makeAbsolute(Blocks.config().getSiteDomain().toString(), relativePath);
    }


    public static String makeAbsolute(String host, String relativePath) {
        host = host.trim();
        if (!host.endsWith("/") && !host.endsWith("#") && !host.endsWith(":"))
            host += "/";
        if (relativePath.startsWith("/"))
            relativePath = relativePath.substring(1);
        return host + relativePath;
    }


    public static Locale getLanguage(URI url)
    {
        Path path = Paths.get(url.getPath());
        Locale retVal = Locale.ROOT;
        if (path.getNameCount() > 0) {
            String pathName = path.getName(0).toString();
            if (Blocks.config().getLocaleForLanguage(pathName) != null) {
                retVal = Blocks.config().getLocaleForLanguage(pathName);
            }
        }
        return retVal;
    }

    public static URI getUrlWithoutLanguage(URI url)
    {
        Locale language = UrlTools.getLanguage(url);
        Path path = Paths.get(url.getPath());
        URI retVal = url;
        if (language != null) {
            try {
                retVal = new URI(url.getSchemeSpecificPart());
            } catch (Exception e) {

            }
            retVal.resolve(path.subpath(1, path.getNameCount()).toString());
        }
        return retVal;
    }

    public static Path getPathWithoutLanguage(Path currentPath) {
        Path retVal = currentPath;
        if (currentPath.getNameCount() == 1) {
            retVal = Paths.get("/");
        } else {
            retVal = currentPath.subpath(1, currentPath.getNameCount());
        }
        return Paths.get("/").resolve(retVal);
    }

    public static URI createURI(String host, String path) throws URISyntaxException
    {
        URIBuilder builder = new URIBuilder();
        builder.setHost(host).setPath(path);
        return builder.build();

    }

}

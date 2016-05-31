package com.beligum.blocks.utils;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.exceptions.RdfException;
import com.beligum.blocks.rdf.ifaces.RdfClass;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static com.beligum.base.server.R.configuration;

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
        return UriBuilder.fromUri(configuration().getSiteDomain()).path(ParserConstants.RESOURCE_ENDPOINT).path(entity.getName()).path(id).build();
    }

    /**
     * Create a locale resource id, based on the type and an existing id-value
     * e.g. /v1/resource/address/big-street-in-antwerp
     */
    public static URI createRelativeResourceId(RdfClass entity, String id)
    {
        return UriBuilder.fromUri("/").path(ParserConstants.RESOURCE_ENDPOINT).path(entity.getName()).path(id).build();
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

        if (uri!=null && uri.getPath()!=null) {
            Path path = Paths.get(uri.getPath());
            //A bit conservative: we need three segments: the word 'resource', the type of the resource and the ID
            if (path.getNameCount() == 3 && path.startsWith(ParserConstants.RESOURCE_ENDPOINT)) {
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
            URI relative = configuration().getSiteDomain().relativize(retVal);
            //if it's not absolute (eg. it doesn't start with http://..., this means the relativize 'succeeded' and the retVal starts with the RDF ontology URI)
            if (!relative.isAbsolute()) {
                retVal = ROOT.resolve(relative);
            }
        }

        return retVal;
    }







    // ----- TO CHECK -----

    /*
    * create a local type based on the ontology in the config
    * e.g. http://www.republic.be/ontology/address
    * */
    public static URI createLocalType(String type)
    {
        return makeLocalAbsolute(type);
    }

    /*
    * Make a path absolute relative to the local ontology uri
    * */
    public static URI makeLocalAbsolute(String relativePath)
    {
        return addToUri(Settings.instance().getRdfOntologyUri(), relativePath);
    }

    /*
   * Make a path absolute relative to the local ontology uri
   * */
    public static URI addToUri(URI uri, String relativePath)
    {
        URI retVal = findInUrlCache(uri, relativePath);

        if (retVal != null)
            return retVal;

        if (relativePath.startsWith("/"))
            relativePath = relativePath.substring(1);
        String fragment = Settings.instance().getRdfOntologyUri().getFragment();
        if (fragment == null) {
            Path path = Paths.get(uri.getPath());
            path = path.resolve(relativePath).normalize();
            retVal = UriBuilder.fromUri(Settings.instance().getRdfOntologyUri()).replacePath(path.toString()).build();
        }
        // Add to fragment
        else {
            fragment = fragment.trim();
            if (fragment.equals("")) {
                retVal = UriBuilder.fromUri(Settings.instance().getRdfOntologyUri()).fragment(relativePath).build();
            }
            else {
                if (!fragment.endsWith("/"))
                    fragment = fragment + "/";
                retVal = UriBuilder.fromUri(Settings.instance().getRdfOntologyUri()).fragment(fragment + relativePath).build();
            }
        }

        putInURLCache(uri, relativePath, retVal);

        return retVal;
    }

    /*
    * Parse a prefix attribute in the from of:
    * "rep:http://www.republic.be be: http://www.belgium.be
    *
    * */
    public static HashMap<String, URI> parsePrefixes(String prefixString) throws RdfException
    {
        // TODO the parsing of prefixes is not correct yet. see specs

        HashMap<String, URI> retVal = new HashMap<>();

        prefixString = prefixString.trim();

        int index = 0;
        while (prefixString.length() > 0) {
            // find first prefix
            index = prefixString.indexOf(":");

            if (index == -1) {
                throw new RdfException("Invalid prefix attribute value: " + prefixString);
            }

            String prefix = prefixString.substring(0, index);
            index++;

            // remove prefix from attribute, trim remainig string and url (until next space)
            // if no space is found, rest of string is url
            prefixString = prefixString.substring(index).trim();
            index = prefixString.indexOf(" ");
            String stringUri = prefixString;
            if (index > 0) {
                stringUri = prefixString.substring(0, index);
                prefixString.trim();
            }

            // Check if valus is valid url
            URI uri = UriBuilder.fromUri(stringUri).build();
            if (!RdfTools.isValidAbsoluteURI(uri)) {
                throw new RdfException("Invalid prefix attribute value. Invalid url " + prefix + ": " + stringUri);
            }

            retVal.put(prefix, uri);
        }

        return retVal;

    }

    public static URI findInUrlCache(URI uri, String path)
    {
        URI retVal = null;
        if (urlcache.containsKey(uri)) {
            HashMap<String, URI> urls = urlcache.get(uri);
            if (urls.containsKey(path)) {
                retVal = urls.get(path);
            }
        }
        return retVal;
    }

    public static void putInURLCache(URI uri, String path, URI result)
    {
        if (!urlcache.containsKey(uri)) {
            urlcache.put(uri, new HashMap<String, URI>());
        }
        urlcache.get(uri).put(path, result);
    }

    /*
    * Checks if a URI is a valid absolute URI
    * */
    public static boolean isValidAbsoluteURI(URI uri)
    {
        return uri.getHost() != null && uri.getScheme() != null;
    }

    /*
    * Create a field name to be used in the db
    * Allow only a-z A-Z 0-9, replace them with underscore
    *
    * use host, path and fragment if available
    * http://www.example.com/test@address -> www_example_com_test_address
    *
    * */
    public static String makeDbFieldFromUri(URI field)
    {
        String retVal = field.getHost() + "_" + field.getPath();
        if (field.getFragment() != null)
            retVal = retVal + "_" + field.getFragment();
        retVal = retVal.trim().replaceAll("[^A-Za-z0-9]", "_");
        retVal = retVal.replaceAll("_+", "_");
        return retVal;
    }

}

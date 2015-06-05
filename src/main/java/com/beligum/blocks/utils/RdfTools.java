package com.beligum.blocks.utils;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.RdfException;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;

/**
 * Created by wouter on 27/04/15.
 *
 * Simple functions to make the RDF life easier
 *
 */
public class RdfTools
{
    // Simpleflake generates a Long id, based on timestamp
    public static SimpleFlake simpleFlake = new SimpleFlake();

    /*
    * create a local type based on the ontology in the config
    * e.g. http://www.republic.be/ontology/address
    * */
    public static URI createLocalType(String type) {
        return makeLocalAbsolute(type);
    }

    /*
    * create a locale resource id, based on the type and an existing id-value
    * e.g. http://www.republic.be/v1/resource/address/big-street-in-antwerp
    * */
    public static URI createLocalResourceId(String type, String id) {
        return UriBuilder.fromUri(BlocksConfig.instance().getSiteDomain()).path(ParserConstants.RESOURCE_ENDPOINT).path(type.toLowerCase()).path(id).build();
    }

    /*
    * create a local resource based on the resource endpoint and a type. Generate an id-value
    * e.g. http://www.republic.be/v1/resource/address/156465
    * */
    public static URI createLocalResourceId(String type) {
        return createLocalResourceId(type, new Long(RdfTools.simpleFlake.generate()).toString());
    }

    /*
    * Make a path absolute relative to the local ontology uri
    * */
    public static URI makeLocalAbsolute(String relativePath) {
        URI retVal;
        if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
        String fragment = BlocksConfig.instance().getDefaultRdfSchema().getFragment();
        if (fragment == null) {
            retVal = UriBuilder.fromUri(BlocksConfig.instance().getDefaultRdfSchema()).path(relativePath).build();
        }
        // Add to fragment
        else {
            fragment = fragment.trim();
            if (fragment.equals("")) {
                retVal = UriBuilder.fromUri(BlocksConfig.instance().getDefaultRdfSchema()).fragment(relativePath).build();
            } else {
                if (!fragment.endsWith("/")) fragment = fragment + "/";
                retVal = UriBuilder.fromUri(BlocksConfig.instance().getDefaultRdfSchema()).fragment(fragment + relativePath).build();
            }
        }
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

    /*
    * Checks if a URI is a valid absolute URI
    * */
    public static boolean isValidAbsoluteURI(URI uri) {
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
    public static String makeDbFieldFromUri(URI field) {
        String retVal = field.getHost()+ "_" + field.getPath();
        if (field.getFragment() != null) retVal = retVal + "_" + field.getFragment();
        retVal = retVal.trim().replaceAll("[^A-Za-z0-9]", "_");
        retVal = retVal.replaceAll("_+", "_");
        return retVal;
    }

}

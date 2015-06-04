package com.beligum.blocks.utils;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by wouter on 27/04/15.
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
        return makeAbsolute(type);
    }

    public static URI createLocalResourceId(String type, String id) {
        return UriBuilder.fromUri(BlocksConfig.instance().getSiteDomain()).path(ParserConstants.RESOURCE_ENDPOINT).path(type.toLowerCase()).path(id).build();
    }

    /*
    * create a local resource based on the ontology in the config
    * e.g. http://www.republic.be/v1/resource/address/156465
    * */
    public static URI createLocalResourceId(String type) {
        return createLocalResourceId(type, new Long(RdfTools.simpleFlake.generate()).toString());
    }

    public static URI makeAbsolute(String relativePath) {
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

package com.beligum.blocks.endpoints.utils;

import com.beligum.base.utils.toolkit.StringFunctions;
import org.apache.commons.lang.StringUtils;

import java.net.URI;

public class PageUrlValidator
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * This is modified version of StringFunctions.prepareSeoValue()
     * @param uri
     */
    public String createSafePagePath(URI uri)
    {
        String retVal = uri == null ? null : uri.getPath().trim();

        if (!StringUtils.isEmpty(retVal)) {
            //convert all special chars to ASCII
            retVal = StringFunctions.webNormalizeString(retVal);
            //this might be extended later on (note that we need to allow slashes because the path might have multiple segments)
            retVal = retVal.replaceAll("[^a-zA-Z0-9_ \\-/.]", "");
            //replace whitespace with dashes
            retVal = retVal.replaceAll("\\s+", "-");
            //replace double dashes with single dashes
            retVal = retVal.replaceAll("-+", "-");
            //make sure the path doesn't begin or end with a dash
            retVal = StringUtils.strip(retVal, "-");

            //Note: don't do a toLowerCase, it messes up a lot of resource-addresses, eg. /resource/SmithMark/360

            //see for inspiration: http://stackoverflow.com/questions/417142/what-is-the-maximum-length-of-a-url-in-different-browsers
            final int MAX_LENGTH = 2000;
            if (retVal.length() > MAX_LENGTH) {
                retVal = retVal.substring(0, MAX_LENGTH);
                retVal = retVal.substring(0, retVal.lastIndexOf("-"));
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

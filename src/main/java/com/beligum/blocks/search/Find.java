package com.beligum.blocks.search;

/**
 * Created by wouter on 3/09/15.
 */
public class Find
{

    public static AbstractSearch resources() {
        AbstractSearch retVal = new ResourceSearch();
        return retVal;
    }

    public static AbstractSearch webpages() {
        AbstractSearch retVal = new WebpageSearch();
        return retVal;
    }


}

package com.beligum.blocks.core.urlmapping.simple;

import com.beligum.blocks.core.identifiers.BlockId;

import java.net.URL;

/**
 * Created by wouter on 23/03/15.
 */
public interface BlocksUrlDispatcher
{
    public String findId(URL url);

    public void addId(URL url, BlockId id, String language);

    public void removeId(URL url, String language, boolean completely);

    public String getLanguage(URL url);

    public String getLanguageOrNull(URL url);

    public String findPreviousId(URL url);

}

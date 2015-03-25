package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.URLMapping.simple.UrlBranch;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.core.framework.utils.Logger;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

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

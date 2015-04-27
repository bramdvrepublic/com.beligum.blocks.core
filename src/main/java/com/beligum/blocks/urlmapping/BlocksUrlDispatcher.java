package com.beligum.blocks.urlmapping;

import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.SiteUrl;

import java.net.URL;

/**
 * Created by wouter on 23/03/15.
 */
public interface BlocksUrlDispatcher
{
    public SiteUrl findId(URL url);

    public BlockId findPreviousId(URL url);

    public void addId(URL url, URL view, URL resource,  String language);

    public void removeId(URL url) throws Exception;

    public String getLanguage(URL url);

    public String getLanguageOrNull(URL url);

    public String getUrlForId(String id);



}

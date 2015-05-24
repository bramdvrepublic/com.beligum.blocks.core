package com.beligum.blocks.caching;

import com.beligum.blocks.exceptions.CacheException;
import com.beligum.blocks.models.Blueprint;
import com.beligum.blocks.models.PageTemplate;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by wouter on 23/03/15.
 */
public interface BlocksTemplateCache
{

    public void reset() throws CacheException;

    public void addBlueprint(Blueprint blueprint);

    public void addPageTemplate(PageTemplate page);

    public Blueprint getBlueprint(String name);

    public PageTemplate getPageTemplate(String name);

    public List<Blueprint> getBlueprints();

    public List<PageTemplate> getPageTemplates();

    public List<Blueprint> getPageBlocks();

    public List<Blueprint> getAddableBlocks();

    public LinkedHashMap<String, String> getBlocksScripts();

    public LinkedHashMap<String, String> getBlocksLinks();

}

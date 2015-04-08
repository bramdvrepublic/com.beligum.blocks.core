package com.beligum.blocks.caching;

import com.beligum.blocks.exceptions.CacheException;
import com.beligum.blocks.models.Blueprint;
import com.beligum.blocks.models.PageTemplate;

import java.util.HashMap;
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

    public void addBlueprint(Blueprint blueprint, String language);

    public void addPageTemplate(PageTemplate page, String language);

    public void addPrefixes(HashMap<String, String> prefixes);
    public void addPrefix(String prefix, String namespace);
    public String getPrefixForSchema(String schema);
    public String getSchemaForPrefix(String prefix);
    public Blueprint getBlueprint(String name, String language);

    public PageTemplate getPagetemplate(String name, String language);
    public List<Blueprint> getBlueprints(String language);
    public List<PageTemplate> getPagetemplates(String language);
    public List<Blueprint> getPageBlocks();

    public List<Blueprint> getAddableBlocks();

    public LinkedHashSet<String> getBlocksScripts();
    public LinkedHashSet<String> getBlocksLinks();




}

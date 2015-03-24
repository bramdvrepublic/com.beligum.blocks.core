package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.nosql.Blueprint;
import com.beligum.blocks.core.models.nosql.Entity;
import com.beligum.blocks.core.models.nosql.PageTemplate;
import com.beligum.blocks.core.models.nosql.StoredTemplate;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.util.ArrayList;
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

    public Blueprint getBlueprint(String name, String language);

    public PageTemplate getPagetemplate(String name, String language);
    public List<Blueprint> getBlueprints(String language);
    public List<PageTemplate> getPagetemplates(String language);
    public List<Blueprint> getPageBlocks();

    public List<Blueprint> getAddableBlocks();

    public LinkedHashSet<String> getBlocksScripts();
    public LinkedHashSet<String> getBlocksLinks();




}

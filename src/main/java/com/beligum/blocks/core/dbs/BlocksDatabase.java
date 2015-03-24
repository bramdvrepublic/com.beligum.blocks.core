package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.URLMapping.simple.UrlDispatcher;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.models.nosql.Blueprint;
import com.beligum.blocks.core.models.nosql.Entity;
import com.beligum.blocks.core.models.nosql.PageTemplate;
import com.beligum.blocks.core.models.nosql.StoredTemplate;
import com.beligum.core.framework.utils.Logger;
import org.jongo.MongoCollection;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wouter on 23/03/15.
 */
public interface BlocksDatabase
{
    public void saveEntity(Entity entity);

    public Entity fetchEntity(BlockId id, String language);

    public void saveTemplate(StoredTemplate template) throws DatabaseException;

    public StoredTemplate fetchTemplate(BlockId id, String language);

    public void saveSiteMap(BlocksUrlDispatcher urlDispatcher) throws DatabaseException;

    public BlocksUrlDispatcher fetchSiteMap() throws DatabaseException;

    public StoredTemplate createStoredTemplate(Element element, String language) throws ParseException;
    public StoredTemplate createStoredTemplate(Element element, URL url) throws ParseException;
    public Blueprint createBlueprint(Element element, String language) throws ParseException;
    public PageTemplate createPageTemplate(Element element, String language)  throws ParseException;
    public Entity createEntity(String name);
    public Entity createEntity(String name, String language);
}

package com.beligum.blocks.core.mongo;

import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.identifiers.MongoID;
import com.beligum.blocks.core.models.*;
import com.beligum.blocks.core.models.factory.BlocksFactory;
import com.beligum.blocks.core.urlmapping.UrlDispatcher;
import org.jsoup.nodes.Element;

import java.net.URL;

/**
 * Created by wouter on 26/03/15.
 */
public class MongoBlocksFactory implements BlocksFactory
{

    @Override
    public UrlDispatcher createUrlDispatcher() {
        return new MongoUrlDispatcher();
    }


    @Override
    public Blueprint createBlueprint(Element element, String language) throws ParseException
    {
        return new MongoBlueprint(element, language);
    }

    @Override
    public StoredTemplate createStoredTemplate(Element element, String language) throws ParseException
    {
        return new MongoStoredTemplate(element, language);
    }

    @Override
    public StoredTemplate createStoredTemplate(Element element, URL url) throws ParseException
    {
        return new MongoStoredTemplate(element, url);
    }

    @Override
    public PageTemplate createPageTemplate(Element element, String language) throws ParseException
    {
        return new MongoPageTemplate(element, language);
    }



    @Override
    public Entity createEntity(String name)
    {
        return new MongoEntity(name);
    }

    @Override
    public Entity createEntity(String name, String language)
    {
        return new MongoEntity(name, language);
    }
    @Override
    public Class<? extends Entity> getEntityClass()
    {
        return Entity.class;
    }
    @Override
    public Class<? extends StoredTemplate> getStoredTemplateClass()
    {
        return MongoStoredTemplate.class;
    }
    @Override
    public Class<? extends Blueprint> getBlueprintClass()
    {
        return MongoBlueprint.class;
    }
    @Override
    public Class<? extends PageTemplate> getPageTemplateClass()
    {
        return MongoPageTemplate.class;
    }
    @Override
    public Class<? extends Singleton> getSingletonClass()
    {
        return Singleton.class;
    }
    @Override
    public Class<? extends UrlDispatcher> getUrlDispatcherClass()
    {
        return MongoUrlDispatcher.class;
    }

    public BlockId getIdForString(String s) {
        return new MongoID(s);
    }

}

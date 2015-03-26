package com.beligum.blocks.core.models.factory;

import com.beligum.blocks.core.dbs.BlocksUrlDispatcher;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.models.*;
import com.beligum.blocks.core.urlmapping.simple.UrlDispatcher;
import org.jsoup.nodes.Element;

import java.net.URL;

/**
 * Created by wouter on 26/03/15.
 */
public interface BlocksFactory
{
    public StoredTemplate createStoredTemplate(Element element, String language) throws ParseException;
    public StoredTemplate createStoredTemplate(Element element, URL url) throws ParseException;
    public Blueprint createBlueprint(Element element, String language) throws ParseException;
    public PageTemplate createPageTemplate(Element element, String language)  throws ParseException;
    public Entity createEntity(String name);
    public Entity createEntity(String name, String language);

    public Class<? extends Entity> getEntityClass();
    public Class<? extends StoredTemplate> getStoredTemplateClass();
    public Class<? extends Blueprint> getBlueprintClass();
    public Class<? extends PageTemplate> getPageTemplateClass();
    public Class<? extends Singleton> getSingletonClass();
    public Class<? extends UrlDispatcher> getUrlDispatcherClass();


    public BlocksUrlDispatcher createUrlDispatcher();
    public BlockId getIdForString(String s);
}

package com.beligum.blocks.models.factory;

import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.*;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import com.beligum.blocks.urlmapping.BlocksUrlDispatcher;
import com.beligum.blocks.urlmapping.UrlDispatcher;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.util.HashMap;

/**
 * Created by wouter on 26/03/15.
 */
public interface BlocksFactory
{
    public StoredTemplate createStoredTemplate(Element element, String language) throws ParseException;

    public StoredTemplate createStoredTemplate(Element element, URL url) throws ParseException;

    public Blueprint createBlueprint(Element element, String language) throws ParseException;

    public PageTemplate createPageTemplate(Element element, String language) throws ParseException;

    public Entity createEntity(String name);

    public Entity createEntity(HashMap<String, Object> properties);

    public Singleton createSingleton(Element element, String language) throws ParseException;

    public Class<? extends Entity> getEntityClass();

    public Class<? extends StoredTemplate> getStoredTemplateClass();

    public Class<? extends Blueprint> getBlueprintClass();

    public Class<? extends PageTemplate> getPageTemplateClass();

    public Class<? extends Singleton> getSingletonClass();

    public Class<? extends UrlDispatcher> getUrlDispatcherClass();

    public BlocksTemplateRenderer createTemplateRenderer();

    public BlocksUrlDispatcher createUrlDispatcher();

    public BlockId getIdForString(String s);
}

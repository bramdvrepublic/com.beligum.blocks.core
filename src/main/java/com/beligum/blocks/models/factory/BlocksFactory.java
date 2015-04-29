package com.beligum.blocks.models.factory;

import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.*;
import com.beligum.blocks.models.jsonld.ResourceNode;
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
    public PageTemplate createPageTemplate(Element element, String language)  throws ParseException;
    public ResourceNode createEntity(String name);
    public ResourceNode createEntity(HashMap<String, Object> properties);
    public Singleton createSingleton(Element element, String language) throws ParseException;

//    public StoredTemplate wrapStoredTemplate(HashMap<String, Object> map);
//    public BasicTemplate wrapBasicTemplate(HashMap<String, Object> map);
//    public Blueprint wrapBlueprint(HashMap<String, Object> map);
//    public PageTemplate wrapPageTemplate(HashMap<String, Object> map);
//    public Entity wrapResource(HashMap<String, Object> map);


    public Class<? extends ResourceNode> getEntityClass();
    public Class<? extends StoredTemplate> getStoredTemplateClass();

    public Class<? extends Blueprint> getBlueprintClass();

    public Class<? extends PageTemplate> getPageTemplateClass();

    public Class<? extends Singleton> getSingletonClass();
    public Class<? extends BlocksUrlDispatcher> getUrlDispatcherClass();

    public BlocksTemplateRenderer createTemplateRenderer();

    public BlocksUrlDispatcher createUrlDispatcher();

    public BlockId getIdForString(String s);
}

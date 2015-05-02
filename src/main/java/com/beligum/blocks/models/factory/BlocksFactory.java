package com.beligum.blocks.models.factory;

import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.*;
import com.beligum.blocks.models.jsonld.Resource;
import com.beligum.blocks.models.jsonld.ResourceImpl;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.util.HashMap;

/**
 * Created by wouter on 26/03/15.
 */
public interface BlocksFactory
{
    public StoredTemplate createStoredTemplate(Element element, String language) throws ParseException;
    public Blueprint createBlueprint(Element element, String language) throws ParseException;
    public PageTemplate createPageTemplate(Element element, String language)  throws ParseException;
    public Resource createEntity(String name);
    public Resource createEntity();
    public Singleton createSingleton(Element element, String language) throws ParseException;

    public Class<? extends ResourceImpl> getEntityClass();
    public Class<? extends StoredTemplate> getStoredTemplateClass();

    public Class<? extends Blueprint> getBlueprintClass();

    public Class<? extends PageTemplate> getPageTemplateClass();

    public Class<? extends Singleton> getSingletonClass();

    public BlocksTemplateRenderer createTemplateRenderer();

}

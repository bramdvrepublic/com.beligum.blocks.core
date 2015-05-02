package com.beligum.blocks.models.factory;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.Blueprint;
import com.beligum.blocks.models.PageTemplate;
import com.beligum.blocks.models.Singleton;
import com.beligum.blocks.models.StoredTemplate;
import com.beligum.blocks.models.jsonld.Node;
import com.beligum.blocks.models.jsonld.Resource;
import com.beligum.blocks.models.jsonld.ResourceImpl;
import com.beligum.blocks.models.jsonld.StringNode;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import com.beligum.blocks.renderer.VelocityBlocksRenderer;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.util.HashMap;

/**
 * Created by wouter on 2/05/15.
 */
public class DefaultBlocksFactory implements  BlocksFactory
{
    @Override
    public StoredTemplate createStoredTemplate(Element element, String language) throws ParseException
    {
        return new StoredTemplate(element, language);
    }

    @Override
    public Blueprint createBlueprint(Element element, String language) throws ParseException
    {
        return new Blueprint(element, language);
    }
    @Override
    public PageTemplate createPageTemplate(Element element, String language) throws ParseException
    {
        return new PageTemplate(element, language);
    }
    @Override
    public Resource createEntity(String type)
    {
        Resource retVal = new ResourceImpl();
        retVal.set(ParserConstants.JSONLD_TYPE, new StringNode(type));
        return retVal;
    }

    @Override
    public Resource createEntity()
    {
        Resource retVal = new ResourceImpl();
        return retVal;
    }
    @Override
    public Singleton createSingleton(Element element, String language) throws ParseException
    {
        return new Singleton(element, language);
    }
    @Override
    public Class<? extends ResourceImpl> getEntityClass()
    {
        return ResourceImpl.class;
    }
    @Override
    public Class<? extends StoredTemplate> getStoredTemplateClass()
    {
        return StoredTemplate.class;
    }
    @Override
    public Class<? extends Blueprint> getBlueprintClass()
    {
        return Blueprint.class;
    }
    @Override
    public Class<? extends PageTemplate> getPageTemplateClass()
    {
        return PageTemplate.class;
    }
    @Override
    public Class<? extends Singleton> getSingletonClass()
    {
        return Singleton.class;
    }
    @Override
    public BlocksTemplateRenderer createTemplateRenderer()
    {
        return new VelocityBlocksRenderer();
    }
}

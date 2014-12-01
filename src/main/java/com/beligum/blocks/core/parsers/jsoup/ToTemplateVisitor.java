package com.beligum.blocks.core.parsers.jsoup;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.core.framework.utils.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URL;
import java.nio.file.NotDirectoryException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

/**
 * Created by wouter on 22/11/14.
 */
public class ToTemplateVisitor extends AbstractVisitor
{
    Node template;
    Set<EntityTemplate> parsedEntities = new HashSet<EntityTemplate>();
//    private Stack<HashSet<EntityClass>> entityClassSet = new Stack<HashSet<EntityClass>>();
    private Stack<HashSet<EntityTemplate>> entitySet = new Stack<HashSet<EntityTemplate>>();
    private boolean doCache;


    public ToTemplateVisitor(boolean doCache) {
        this.doCache = doCache;
    }

    @Override
    public Node head(Node node, int depth) {
        node = super.head(node, depth);

        if(isEntity(node)) {
            this.pushChildren();
        }
        this.prepareTemplate(node);
        return node;
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        node = super.tail(node, depth);
        if (node instanceof Element && isEntity(node)) {
            Element element = (Element) node;
            Set<EntityTemplate> children = this.popChildren();
            EntityTemplateClass entityTemplateClass;
            if(doCache) {
                entityTemplateClass = getNewEntityClassFromElement(element);
            }
            else{
                entityTemplateClass = getEntityClassForElement(element);
            }
            EntityTemplate entityTemplate = new EntityTemplate(entityTemplateClass);
            node = replaceElementWithReference(element, entityTemplate);
            parsedEntities.add(entityTemplate);
            this.getChildren().add(entityTemplate);
        }
        else if(node instanceof Element) {
            this.createTemplate((Element) node);
        }
        return node;
    }

    private EntityTemplateClass getEntityClassForElement(Element element) throws ParseException
    {
        String entityClassName = "";
        try {
            entityClassName = this.getTypeOf(element);
            return EntityTemplateClassCache.getInstance().get(entityClassName);
        }
        catch (CacheException e){
            throw new ParseException("Couldn't get entity-class '" + entityClassName +"' from cache, while parsing: \n \n " + element.outerHtml(), e);
        }
    }

    private EntityTemplateClass getNewEntityClassFromElement(Element element) throws ParseException
    {
        String entityClassName = "";
        try {
            entityClassName = this.getTypeOf(element);
            EntityTemplateClass entityTemplateClass = new EntityTemplateClass(entityClassName, element.outerHtml(), null);
            EntityTemplateClassCache.getInstance().add(entityTemplateClass);
            return EntityTemplateClassCache.getInstance().get(entityClassName);
        }
        catch(Exception e){
            throw new ParseException("Error while creating new entity-class '" + entityClassName +"'.", e);
        }
    }

    protected void pushChildren() {
        if (this.entitySet.empty()) {
            // push one
            this.entitySet.push(new HashSet<EntityTemplate>());
        }
        this.entitySet.push(new HashSet<EntityTemplate>());
}

    protected Set<EntityTemplate> popChildren() {
        return this.entitySet.pop();
    }

    protected Set<EntityTemplate> getChildren() {
        if (!this.entitySet.empty()) {
            return this.entitySet.peek();
        }
        else {
            return new HashSet<EntityTemplate>();
        }
    }

    public Set<EntityTemplate> getAllParsedEntities() {
        return this.parsedEntities;
    }

    public EntityTemplate getParsedEntityTemplate() {
        Iterator<EntityTemplate> childIt = this.getChildren().iterator();
        if (childIt.hasNext()) {
            return childIt.next();
        }
        else{
            return null;
        }
    }

    protected void prepareTemplate(Node node) {
        if (isTemplate(node)) {
            this.template = node;
        }
    }

    protected void createTemplate(Element element)
    {
        if (element.hasAttr(ParserConstants.PAGE_TEMPLATE_CONTENT_ATTR) && doCache) {
            try {
                Node parent = element.parent();
                //initialize the page-template name by searching for the first template-attribute we find before the specified node and take the value of that attribute to be the name
                String templateName = "";
                while (parent.parent() != null) {
                    if (parent.nodeName().equals("html")) {
                        templateName = parent.attr(ParserConstants.PAGE_TEMPLATE_ATTR);
                    }
                    parent = parent.parent();
                }
                this.replaceElementWithReference(element, ParserConstants.PAGE_TEMPLATE_ENTITY_VARIABLE_NAME);
                PageTemplate pageTemplate = new PageTemplate(templateName, parent.outerHtml());
                PageTemplateCache.getInstance().add(pageTemplate);
            } catch (Exception e) {
                Logger.error("Something went wrong while creating template.", e);
                // TODO show error somewhere?
            }
        }
    }


    public static EntityTemplate cache(Element element, URL url) throws ParseException
    {
        ToTemplateVisitor visitor = new ToTemplateVisitor(true);
        Traversor traversor = new Traversor(visitor);
        traversor.traverse(element);
        return visitor.getParsedEntityTemplate();
    }

    public static EntityTemplate parse(Element element, URL url) throws ParseException
    {
        ToTemplateVisitor visitor = new ToTemplateVisitor(false);
        Traversor traversor = new Traversor(visitor);
        traversor.traverse(element);
        return visitor.getParsedEntityTemplate();
    }
}

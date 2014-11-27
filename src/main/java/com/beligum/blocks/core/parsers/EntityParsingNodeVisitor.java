package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.caching.EntityClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.models.PageTemplate;
import com.beligum.blocks.core.models.classes.EntityClass;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.core.framework.utils.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

/**
 * Created by wouter on 22/11/14.
 */
public class EntityParsingNodeVisitor extends AbstractEntityNodeVisitor
{
    Element template;
    Set<Entity> parsedEntities = new HashSet<Entity>();
//    private Stack<HashSet<EntityClass>> entityClassSet = new Stack<HashSet<EntityClass>>();
    private Stack<HashSet<Entity>> entitySet = new Stack<HashSet<Entity>>();
    private URL url;
    private boolean doCache;


    public EntityParsingNodeVisitor(URL url, boolean doCache) {
        this.url = url;
        this.doCache = doCache;
    }

    @Override
    public void head(Node node, int depth) {
        super.head(node, depth);

        if (node instanceof Element) {
            Element element = (Element) node;
            if(AbstractParser.isBlock(element)) {
                this.pushChildren();
            }
            this.prepareTemplate(element);
        }
    }

    @Override
    public void tail(Node node, int depth) {
        try{
            this.doTail(node, depth);
        }
        catch(ParserException e){
            Logger.error("Error while parsing node \n \n" + node.outerHtml(), e);

        }
    }

    public Node doTail(Node node, int depth) throws ParserException
    {
        super.tail(node, depth);
        // Here we create the new DefaultValue
        if (node instanceof Element) {
            Element element = (Element) node;
            if (AbstractParser.isBlock(element)) {

                Set<Entity> children = this.popChildren();
                EntityClass entityClass;
                if(doCache) {
                    entityClass = getNewEntityClassFromElement(element, children);
                }
                else{
                    entityClass = getEntityClassForElement(element);
                }
                Entity entity = new Entity(entityClass);
                node = replaceNodeWithReference(element, entity);
                parsedEntities.add(entity);
                this.getChildren().add(entity);
            }
            this.createTemplate(element);
        }
        return node;
    }

    private EntityClass getEntityClassForElement(Element element) throws ParserException
    {
        String entityClassName = "";
        try {
            entityClassName = AbstractParser.getType(element);
            return EntityClassCache.getInstance().get(entityClassName);
        }
        catch (CacheException e){
            throw new ParserException("Couldn't get entity-class '" + entityClassName +"' from cache, while parsing: \n \n " + element.outerHtml(), e);
        }
    }

    private EntityClass getNewEntityClassFromElement(Element element, Set<Entity> children) throws ParserException
    {
        String entityClassName = "";
        try {
            entityClassName = AbstractParser.getType(element);
            EntityClass entityClass = new EntityClass(entityClassName, children, element.outerHtml(), null);
            EntityClassCache.getInstance().add(entityClass);
            return EntityClassCache.getInstance().get(entityClassName);
        }
        catch(URISyntaxException | CacheException e){
            throw new ParserException("Error while creating new entity-class '" + entityClassName +"'.", e);
        }
    }

    protected void pushChildren() {
        if (this.entitySet.empty()) {
            // push one
            this.entitySet.push(new HashSet<Entity>());
        }
        this.entitySet.push(new HashSet<Entity>());
}

    protected Set<Entity> popChildren() {
        return this.entitySet.pop();
    }

    protected Set<Entity> getChildren() {
        if (!this.entitySet.empty()) {
            return this.entitySet.peek();
        }
        else {
            return new HashSet<Entity>();
        }
    }

    public Set<Entity> getAllParsedEntities() {
        return this.parsedEntities;
    }

    public Entity getParsedEntity() {
        Iterator<Entity> childIt = this.getChildren().iterator();
        if (childIt.hasNext()) {
            return childIt.next();
        }
        else{
            return null;
        }
    }

    protected void prepareTemplate(Element element) {
        if (element.tag().equals("html") && element.hasAttr(ParserConstants.TEMPLATE_ATTR)) {
            this.template = element;
        }
    }

    protected void createTemplate(Element element)
    {
        if (element.hasAttr(ParserConstants.TEMPLATE_CONTENT_ATTR) && doCache) {
            try {
                Element parent = element.parent();
                //initialize the page-template name by searching for the first template-attribute we find before the specified node and take the value of that attribute to be the name
                String templateName = "";
                while (parent.parent() != null) {
                    if (parent.tagName().equals("html")) {
                        templateName = parent.attr(ParserConstants.TEMPLATE_ATTR);
                    }
                    parent = parent.parent();
                }
                this.replaceNodeWithReference(element, ParserConstants.PAGE_TEMPLATE_ENTITY_VARIABLE_NAME);
                PageTemplate pageTemplate = new PageTemplate(templateName, parent.outerHtml());
                PageTemplateCache.getInstance().add(pageTemplate);
            } catch (Exception e) {
                Logger.error("Something went wrong while creating template.", e);
                // TODO show error somewhere?
            }
        }
    }


    public static Entity cache(URL url, Element element) throws ParserException
    {
        EntityParsingNodeVisitor visitor = new EntityParsingNodeVisitor(url, true);
        EntityNodeTraversor traversor = new EntityNodeTraversor(visitor);
        traversor.traverse(element);
        return visitor.getParsedEntity();
    }

    public static Entity parse(URL url, Element element) throws ParserException
    {
        EntityParsingNodeVisitor visitor = new EntityParsingNodeVisitor(url, false);
        EntityNodeTraversor traversor = new EntityNodeTraversor(visitor);
        traversor.traverse(element);
        return visitor.getParsedEntity();
    }
}

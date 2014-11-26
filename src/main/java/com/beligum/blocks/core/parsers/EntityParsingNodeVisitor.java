package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.caching.EntityClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CSSClasses;
import com.beligum.blocks.core.config.VelocityVariables;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.PageTemplate;
import com.beligum.blocks.core.models.classes.EntityClass;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.core.framework.utils.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.net.URI;
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
            this.pushChildren();
            Element element = (Element) node;
            this.prepareTemplate(element);
        }
    }

    @Override
    public void tail(Node node, int depth) {
        super.tail(node, depth);
        // Here we create the new DefaultValue
        if (node instanceof Element) {
            Element element = (Element) node;
            if (AbstractParser.isBlock(element)) {

                Set<Entity> children = this.popChildren();
                EntityClass entityClass = getEntityClassForElement(element, children);
                Entity entity = getEntityForElement(element, children);
                replaceNodeWithReference(element, entity);
                parsedEntities.add(entity);
                this.getChildren().add(entity);
            }
            this.createTemplate(element);

        }
    }

    private EntityClass getEntityClassForElement(Element element, Set<Entity> children) {
        EntityClass retVal = null;
        try {
            String entityName = AbstractParser.getType(element);

            if (doCache) {
                EntityClass entityClass = new EntityClass(entityName, children, element.outerHtml(), null);
                EntityClassCache.getInstance().add(entityClass);

            }
            retVal = EntityClassCache.getInstance().get(entityName);

        } catch (Exception e) {

        }
        return retVal;
    }

    private Entity getEntityForElement(Element element, Set<Entity> children)
    {
        try {
            return new Entity(new RedisID(this.url), this.getChildren(), AbstractParser.getType(element));
        }
        catch (RuntimeException e){
            throw new RuntimeException("Runtime error while getting entity for element \n \n" + element.outerHtml(), e);
        }
        catch (Exception e) {
            Logger.error("Error while getting entity for element \n \n" + element.outerHtml(), e);
            //TODO: throwing this runtimeexception should be changed to handling the exceptions and showing them to the end-user
            throw new RuntimeException("Something went wrong which should be handled in a catch-clause.", e);
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
        Set<Entity> retVal = null;
        if (!this.entitySet.empty()) {
            retVal = this.entitySet.peek();
        }
        return retVal;
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
        if (element.tag().equals("html") && element.hasAttr(CSSClasses.TEMPLATE_ATTR)) {
            this.template = element;
        }
    }

    protected void createTemplate(Element element)
    {
        if (element.hasAttr(CSSClasses.TEMPLATE_CONTENT_ATTR) && doCache) {
            try {
                Element parent = element.parent();
                //initialize the page-template name by searching for the first template-attribute we find before the specified node and take the value of that attribute to be the name
                String templateName = "";
                while (parent.parent() != null) {
                    if (parent.tagName().equals("html")) {
                        templateName = parent.attr(CSSClasses.TEMPLATE_ATTR);
                    }
                    parent = parent.parent();
                }
                Node newNode = new TextNode("${" + VelocityVariables.ENTITY_VARIABLE_NAME + "}", BlocksConfig.getSiteDomain());
                element.replaceWith(newNode);
                PageTemplate pageTemplate = new PageTemplate(templateName, parent.outerHtml());
                PageTemplateCache.getInstance().add(pageTemplate);
            } catch (Exception e) {
                Logger.error("Something went wrong while creating template.", e);
                // TODO show error somewhere?
            }
        }
    }


    public static Entity cache(URL url, Element element) {
        EntityParsingNodeVisitor visitor = new EntityParsingNodeVisitor(url, true);
        element.traverse(visitor);
        return visitor.getParsedEntity();
    }

    public static Entity parse(URL url, Element element) {
        EntityParsingNodeVisitor visitor = new EntityParsingNodeVisitor(url, false);
        element.traverse(visitor);
        return visitor.getParsedEntity();
    }
}

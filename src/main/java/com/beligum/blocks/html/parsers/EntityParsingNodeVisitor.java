package com.beligum.blocks.html.parsers;

import com.beligum.blocks.core.caching.EntityClassCache;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.classes.EntityClass;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.blocks.html.PageTemplate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URL;
import java.util.HashSet;
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

    private Entity getEntityForElement(Element element, Set<Entity> children) {
        Entity retVal = null;
        try {
            new Entity(new RedisID(this.url), this.getChildren(), AbstractParser.getType(element));
        } catch (Exception e) {

        }
        return retVal;
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
        Entity retVal = null;
        if (!this.entitySet.empty() && this.entitySet.size() > 0) {
            retVal = this.getChildren().iterator().next();
        }
        return retVal;
    }

    protected void prepareTemplate(Element element) {
        if (element.tag().equals("html") && element.hasAttr("template")) {
            this.template = element;
        }
    }

    protected void createTemplate(Element element) {
        if (element.hasAttr("content")) {
            PageTemplate pageTemplate = new PageTemplate(element);

            try {
                EntityClassCache.getInstance().addPageTemplate(pageTemplate);
            } catch (Exception e) {
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

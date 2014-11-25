package com.beligum.blocks.html.parsers;

import com.beligum.blocks.core.caching.EntityClassCache;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.classes.EntityClass;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.blocks.html.Cacher.TypeCacher;
import com.beligum.blocks.html.models.types.Template;
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
//    private Stack<HashSet<EntityClass>> entityClassSet = new Stack<HashSet<EntityClass>>();
    private Stack<HashSet<Entity>> entitySet = new Stack<HashSet<Entity>>();
    private URL url;


    public EntityParsingNodeVisitor(URL url) {
        this.url = url;
    }

    @Override
    public void head(Node node, int depth) {
        super.head(node, depth);

        if (node instanceof Element) {
            this.pushChildren();
            Element element = (Element) node;
            if (element.tag().equals("html") && element.hasAttr("template")) {
                this.template = (Element) node;
            }
        }
    }

    @Override
    public void tail(Node node, int depth) {
        super.tail(node, depth);
        // Here we create the new DefaultValue
        if (node instanceof Element) {
            Element element = (Element) node;
            if (AbstractParser.isBlock(element)) {
                Element reference = replaceNodeWithReference(element);
                Set<Entity> children = this.popChildren();
                EntityClass entityClass = getEntityClassForElement(element, children);
                Entity entity = getEntityForElement(element, children);
                this.getChildren().add(entity);
            }

            if (node.hasAttr("content")) {
                Template template = new Template(element);
                TypeCacher.instance().addTemplate(template, false);
            }
        }
    }

    private EntityClass getEntityClassForElement(Element element, Set<Entity> children) {
        EntityClass retVal = null;
        try {
            String entityName = AbstractParser.getType(element);

            EntityClass storedClass = EntityClassCache.getInstance().get(entityName);
            EntityClass entityClass = new EntityClass(entityName, children, element.outerHtml(), null);
            // TODO Wouter: check for blueprint to overwrite class in cache
            if (storedClass == null || storedClass.getTemplate() == null) {
                // Cache this entityClass
                retVal = entityClass;
            } else {
                retVal =  storedClass;
            }
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
    this.entitySet.push(new HashSet<Entity>());
}

    protected Set<Entity> popChildren() {
        return this.entitySet.pop();
    }

    protected Set<Entity> getChildren() {
        return this.entitySet.peek();
    }



    public static Entity PersistingNodeVisitor(URL url, Element element) {
        EntityParsingNodeVisitor visitor = new EntityParsingNodeVisitor(url);
        element.traverse(visitor);
        return visitor.result;
    }
}

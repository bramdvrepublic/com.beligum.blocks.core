package com.beligum.blocks.html.parsers;

import com.beligum.blocks.core.caching.EntityClassCache;
import com.beligum.blocks.core.config.CSSClasses;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.classes.EntityClass;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.blocks.html.db.BlocksDBFactory;
import com.beligum.blocks.html.models.Content;
import com.beligum.blocks.html.models.types.DefaultValue;
import com.beligum.blocks.html.models.types.Identifiable;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Created by wouter on 21/11/14.
 */
public class PersistingNodeVisitor extends AbstractEntityNodeVisitor
{

    private Entity result = null;
    private Stack<HashSet<Entity>> entitySet = new Stack<HashSet<Entity>>();

    Stack<Integer> typeDepth = new Stack<Integer>();
    Stack<Element> typeStack = new Stack<Element>();
    HashMap<String, Integer> propertyCount = new HashMap<String, Integer>();
    URL url = null;

    private PersistingNodeVisitor(URL url) {
        this.url = url;

    }

    public void head(Node node, int depth)
    {
        this.pushChildren();
    }

    public void tail(Node node, int depth) {
        super.tail(node, depth);
        // Here we create the new DefaultValue
        if (node instanceof Element) {
            Element element = (Element) node;
            if (AbstractParser.isBlock(element)) {
                Element reference = replaceNodeWithReference(element);
                String entityName = AbstractParser.getType(element);
                if (entityName == null) entityName = CSSClasses.DEFAULT_ENTITY_CLASS;
                try {
                    EntityClass entityClass = EntityClassCache.getInstance().get(entityName);
                    this.result = new Entity(new RedisID(url), entityClass, this.popChildren());
                    this.getChildren().add(this.result);
                } catch (Exception e) {

                }
            }
        }
    }

    protected void pushChildren() {
        this.entitySet.push(new HashSet<Entity>());
    }

    protected Set<Entity> popChildren() {
        this.entitySet.pop();
    }

    protected Set<Entity> getChildren() {
        return this.entitySet.peek();
    }


    private void saveContent(Content content) {
        BlocksDBFactory.instance().db().put(content.getId(), content.getParsedContent());
    }

    public Entity getVisitor() {
        return this.getVisitor();
    }

    public static Entity PersistingNodeVisitor(URL url, Element element) {
        PersistingNodeVisitor visitor = new PersistingNodeVisitor(url);
        element.traverse(visitor);
        return visitor.result;
    }
}

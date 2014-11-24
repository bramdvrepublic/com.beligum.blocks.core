package com.beligum.blocks.html.parsers;

import com.beligum.blocks.core.config.CSSClasses;
import com.beligum.blocks.core.models.classes.EntityClass;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.blocks.html.Cacher.TypeCacher;
import com.beligum.blocks.html.models.types.DefaultValue;
import com.beligum.blocks.html.models.types.Template;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Created by wouter on 22/11/14.
 */
public class CachingNodeVisitor extends AbstractEntityNodeVisitor
{
    Element template;
    private Stack<HashSet<EntityClass>> entitySet = new Stack<HashSet<EntityClass>>();


    @Override
    public void head(Node node, int depth) {
        super.head(node, depth);
        this.pushChildren();
        if (node instanceof Element) {
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
                try {
                    String entityName = AbstractParser.getType(element);
                    if (entityName == null) entityName = CSSClasses.DEFAULT_ENTITY_CLASS;
                    EntityClass entity = new EntityClass(entityName, this.popChildren(), reference.outerHtml(), null);
                } catch (Exception e) {
                    //cacheDefaultValue(content);
                }
            }

            if (node.hasAttr("content")) {
                Template template = new Template(element);
                TypeCacher.instance().addTemplate(template, false);
            }
        }
    }



    private void cacheDefaultValue(DefaultValue defaultValue) {
        TypeCacher.instance().addDefault(defaultValue, false);
    }

    protected void pushChildren() {
    this.entitySet.push(new HashSet<EntityClass>());
}

    protected Set<EntityClass> popChildren() {
        this.entitySet.pop();
    }

    protected Set<EntityClass> getChildren() {
        return this.entitySet.peek();
    }
}

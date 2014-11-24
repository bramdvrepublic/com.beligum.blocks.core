package com.beligum.blocks.html.parsers;

import com.beligum.blocks.html.Cacher.TypeCacher;
import com.beligum.blocks.html.models.types.DefaultValue;
import com.beligum.blocks.html.models.types.Template;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 * Created by wouter on 22/11/14.
 */
public class CachingNodeVisitor extends AbstractEntityNodeVisitor
{
    Element template;

    @Override
    public void head(Node node, int depth) {
        super.head(node, depth);
        if (node instanceof Element && ((Element)node).tag().equals("html") && node.hasAttr("template")) {
            this.template = (Element)node;
        }
    }

    @Override
    public void tail(Node node, int depth) {
        super.tail(node, depth);
        // Here we create the new DefaultValue
        if (node instanceof Element) {
            Element element = (Element) node;
            if (AbstractParser.isBlock(element)) {
                replaceNodeWithReference(element);
                DefaultValue content = new DefaultValue(element, getlastParent(), getPropertyCount(element));
                cacheDefaultValue(content);
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

}

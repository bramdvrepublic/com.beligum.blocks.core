package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.models.storables.Entity;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.util.HashMap;
import java.util.Stack;

/**
 * Created by wouter on 23/11/14.
 */
public class AbstractEntityNodeVisitor implements NodeVisitor
{

    private Stack<Integer> containerDepth = new Stack<Integer>();
    private Stack<Integer> typeDepth = new Stack<Integer>();
    private Stack<Element> typeStack = new Stack<Element>();
    private HashMap<String, Integer> propertyCount = new HashMap<String, Integer>();


    public void head(Node node, int depth) {
        if (node instanceof Element) {
            Element element = (Element) node;
            if (AbstractParser.isBlock(element)) {
                if (element.hasClass("container")) {
                    if (containerDepth.size() > 0 && containerDepth.peek() == null) {
                        containerDepth.push(depth);
                    }
                }
                typeDepth.push(depth);
                typeStack.push(element);
                this.resetPropertyCount();
            }
        }
    }

    public void tail(Node node, int depth) {
        // Here we create the new DefaultValue
        if (node instanceof Element) {
            Element element = (Element) node;
            if (AbstractParser.isBlock(element)) {
                typeDepth.pop();
                typeStack.pop();
            }
        }
    }


    protected String getPropertyFieldId(Element element) {
        return AbstractParser.getProperty(element) + typeDepth.peek();
    }

    protected int getPropertyCount(Element element) {
        int count = 1;
        String property = AbstractParser.getProperty(element);
        if (property != null && propertyCount.get(property) != null) {
            count = propertyCount.get(property) + 1;
            propertyCount.put(property, count);
        }
        return count;
    }

    private void resetPropertyCount() {
        this.propertyCount = new HashMap<String, Integer>();
    }

    protected Element getlastParent() {
        if (!this.typeStack.empty()) {
            return this.typeStack.peek();
        } else {
            return null;
        }
    }

    protected Node replaceNodeWithReference(Element element, Entity entity) {
        return replaceNodeWithReference(element, entity.getTemplateVariableName());
    }

    protected Node replaceNodeWithReference(Element element, String referenceTo)
    {
        //        Element replacementNode = new Element(element.tag(),"");
        //        replacementNode.attributes().addAll(element.attributes());
        //        replacementNode.attr("parsedContent", "");
        //        element.replaceWith(replacementNode);
        Element replacementNode = new Element(element.tag(), BlocksConfig.getSiteDomain());
        replacementNode.attributes().addAll(element.attributes());
        replacementNode.attr(ParserConstants.REFERENCE_TO, referenceTo);
        element.replaceWith(replacementNode);
        return replacementNode;
    }

    protected boolean inContainer(int depth) {
        boolean retVal = false;
        Integer cDepth = containerDepth.peek();
        if (cDepth != null && cDepth < depth) {
            retVal = true;
        }
        return retVal;
    }

    private void updateBootstrap(Element element) {
        if (AbstractParser.isBootstrapRow(element)) {
            if (AbstractParser.isBootstrapRow(element.parent())) {

            } else if (!AbstractParser.isBootstrapLayout(element)){

            }
        }
    }


}

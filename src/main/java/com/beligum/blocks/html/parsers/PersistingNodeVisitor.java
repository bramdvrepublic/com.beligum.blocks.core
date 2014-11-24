package com.beligum.blocks.html.parsers;

import com.beligum.blocks.html.db.BlocksDBFactory;
import com.beligum.blocks.html.models.Content;
import com.beligum.blocks.html.models.types.DefaultValue;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.HashMap;
import java.util.Stack;

/**
 * Created by wouter on 21/11/14.
 */
public class PersistingNodeVisitor extends AbstractEntityNodeVisitor
{
    Stack<Integer> typeDepth = new Stack<Integer>();
    Stack<Element> typeStack = new Stack<Element>();
    HashMap<String, Integer> propertyCount = new HashMap<String, Integer>();


    public void tail(Node node, int depth) {
        super.tail(node, depth);
        // Here we create the new DefaultValue
        if (node instanceof Element) {
            Element element = (Element) node;
            if (AbstractParser.isBlock(element)) {
                replaceNodeWithReference(element);
                Content content = new Content(element, typeStack.peek(), getPropertyCount(element));
                saveContent(content);
            }
        }
    }

    private void saveContent(Content content) {
        BlocksDBFactory.instance().db().put(content.getId(), content.getParsedContent());
    }

}

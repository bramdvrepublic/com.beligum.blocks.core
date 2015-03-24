package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.parsers.visitors.SuperVisitor;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 * Created by wouter on 16/03/15.
 */
public class SimpleTraversor
{
    public static SuperVisitor traverseDeep(Node root, SuperVisitor visitor) throws ParseException
    {
        Node node = root;
        int depth = 0;

        while (node != null) {
            if (node instanceof Element) {
                node = visitor.head(node, depth);
                if (node.childNodeSize() > 0) {
                    node = node.childNode(0);
                    depth++;
                }
                else {
                    while (node.nextSibling() == null && depth > 0) {
                        node = visitor.tail(node, depth);
                        node = node.parentNode();
                        depth--;
                    }
                    node = visitor.tail(node, depth);

                    node = node.nextSibling();
                }
            } else {
                while (node.nextSibling() == null && depth > 0) {
//                    node = visitor.tail(node, depth);
                    node = node.parentNode();
                    depth--;
                }

                node = node.nextSibling();
            }
        }
        return visitor;
    }

    public static SuperVisitor traverseProperties(Node root, SuperVisitor visitor) throws ParseException
    {
        Node node = root;
        int depth = 0;
        boolean foundProperty = false;

        while (node != null) {
                if (depth > 0 && node instanceof Element) {
                    node = visitor.head((Element) node, depth);
                    if (node.hasAttr(ParserConstants.PROPERTY) || node.hasAttr(ParserConstants.PAGE_TEMPLATE_CONTENT_ATTR)) foundProperty = true;
                }

                if (node.childNodeSize() > 0 && !foundProperty) {
                    node = node.childNode(0);
                    depth++;
                }

                else {
                    foundProperty = false;
                    while (node.nextSibling() == null && depth > 0) {
                        if (node instanceof Element) {
                            node = visitor.tail(node, depth);
                        }
                        node = node.parentNode();
                        depth--;
                    }
//                    node = visitor.tail((Element) node, depth);

                    node = node.nextSibling();
                }
            }

        return visitor;
    }

}

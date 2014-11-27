package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.models.storables.Entity;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * Created by bas on 26.11.14.
 * Based on org.jsoup.select.NodeTraversor.
 * Depth-first node traversor. Use to iterate through all nodes under and including the specified root node.
 * <p/>
 * This implementation does not use recursion, so a deep DOM does not risk blowing the stack.
 */
public class EntityNodeTraversor
{
    private EntityParsingNodeVisitor visitor;

    /**
     * Create a new traversor.
     * @param visitor a class implementing the {@link NodeVisitor} interface, to be called when visiting each node.
     */
    public EntityNodeTraversor(EntityParsingNodeVisitor visitor)
    {
        this.visitor = visitor;
    }

    /**
     * Start a depth-first traverse of the root and all of its descendants.
     * @param root the root node point to traverse.
     */
    public void traverse(Node root) throws ParserException
    {
        Node node = root;
        int depth = 0;

        while (node != null) {
            visitor.head(node, depth);
            if (node.childNodeSize() > 0) {
                node = node.childNode(0);
                depth++;
            } else {
                while (node.nextSibling() == null && depth > 0) {
                    visitor.doTail(node, depth);
                    node = node.parentNode();
                    depth--;
                }
                node = visitor.doTail(node, depth);

                if (node == root)
                    break;
                node = node.nextSibling();
            }
        }
    }
}

package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.models.storables.Entity;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 26.11.14.
 * Based on org.jsoup.select.NodeTraversor.
 * Depth-first node traversor. Use to iterate through all nodes under and including the specified root node.
 * <p/>
 * This implementation does not use recursion, so a deep DOM does not risk blowing the stack.
 */
public class ReplaceChildrenTraversor
{
    private ReplaceChildrenVisitor visitor;

    /**
     * Create a new traversor.
     * @param visitor a class implementing the {@link NodeVisitor} interface, to be called when visiting each node.
     */
    public ReplaceChildrenTraversor(ReplaceChildrenVisitor visitor)
    {
        this.visitor = visitor;
    }

    /**
     * Start a depth-first traverse of the root and all of its descendants, replacing all reference nodes with their corresponding entity's template
     * @param entityRoot the root node of the entity to traverse.
     * @param entityChildMap a map, mapping the template resource-names of the children of the entity to traverse, to entity-objects
     *
     */
    public void traverse(Node entityRoot, Map<String, Entity> entityChildMap) throws ParserException
    {
        Node node = entityRoot;
        int depth = 0;

        while (node != null) {
            node = visitor.doHead(node, depth, entityChildMap);
            if (node.childNodeSize() > 0) {
                node = node.childNode(0);
                depth++;
            } else {
                while (node.nextSibling() == null && depth > 0) {
                    visitor.tail(node, depth);
                    node = node.parentNode();
                    depth--;
                }
                visitor.tail(node, depth);

                if (node == entityRoot)
                    break;
                node = node.nextSibling();
            }
        }
    }
}

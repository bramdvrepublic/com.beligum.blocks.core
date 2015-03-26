package com.beligum.blocks.core.parsers.redis;

import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.parsers.redis.visitors.SuperVisitor;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.net.URL;

/**
 * Created by bas on 26.11.14.
 * Based on org.jsoup.select.NodeTraversor.
 * Depth-first node traversor. Use to iterate through all nodes under and including the specified root node.
 * <p/>
 * This implementation does not use recursion, so a deep DOM does not risk blowing the stack.
 */
public class Traversor
{
    private SuperVisitor visitor;

    private URL pageUrl = null;

    /**
     * Create a new traversor.
     * @param visitor a class implementing the {@link NodeVisitor} interface, to be called when visiting each node.
     */
    public Traversor(SuperVisitor visitor)
    {
        this.visitor = visitor;
    }

    /**
     *
     * @return the url of the first entity encountered in te html, null if none is found or no traverse has been done yet
     */
    public URL getPageUrl(){
        return pageUrl;
    }

    /**
     * Start a depth-first traverse of the root and all of its descendants.
     * @param root the root node point to traverse.
     */
    public void traverse(Node root) throws ParseException
    {
        Node node = root;
        int depth = 0;

        while (node != null) {
            node = visitor.head(node, depth);
            if (node.childNodeSize() > 0) {
                node = node.childNode(0);
                depth++;
            } else {
                while (node.nextSibling() == null && depth > 0) {
                    node = visitor.tail(node, depth);
                    node = node.parentNode();
                    depth--;
                }
                node = visitor.tail(node, depth);

                if (node == root) {
                    pageUrl = visitor.getParentUrl();
                    break;
                }
                node = node.nextSibling();
            }
        }
    }
}

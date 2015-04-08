package com.beligum.blocks.parsers.visitors.reset;

import com.beligum.blocks.parsers.visitors.BasicVisitor;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.LinkedHashSet;

/**
 * Created by wouter on 18/03/15.
 */
public class BlocksScriptVisitor extends BasicVisitor
{

    LinkedHashSet<String> scripts = new LinkedHashSet<>();
    LinkedHashSet<String> links = new LinkedHashSet<>();

    @Override
    public Node head(Node node, int depth)
    {

        if (node.nodeName().equals("link")) {
            //if an include has been found, import the wanted html-file

            this.links.add(node.outerHtml());
            Node emtpyNode = new TextNode("", null);
            node.replaceWith(emtpyNode);
            node = emtpyNode;

        } else if (node.nodeName().equals("script")) {
            //if a script has been found, add it to the scripts-stack
            this.scripts.add(node.outerHtml());
            Node emtpyNode = new TextNode("", null);
            node.replaceWith(emtpyNode);
            node = emtpyNode;
        }

        return node;
    }

    @Override
    public Node tail (Node node, int depth) {
        return node;
    }

    public LinkedHashSet<String> getScripts() {
        return this.scripts;
    }

    public LinkedHashSet<String> getLinks() {
        return this.links;
    }
}



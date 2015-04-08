package com.beligum.blocks.parsers.visitors.template;

import com.beligum.blocks.exceptions.ParseException;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.LinkedHashSet;

/**
 * Created by wouter on 16/03/15.
 */
public class BlueprintVisitor extends PropertyVisitor
{
    private LinkedHashSet<String> links = new LinkedHashSet<String>();
    /**the (javascript-)scripts that need to be injected*/
    private LinkedHashSet<String> scripts = new LinkedHashSet<String>();

    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        try {
            node = super.head(node,depth);
            if(node instanceof Element) {

                // if we find a property with use blueprint, add to properties with blueprint

                // if just a property add with typeof

                // add links and scripts to the stack and remove them from the html (to be re-injected later)
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
            }
            return node;
        }
        catch (Exception e){
            throw new ParseException("Could not parse tag-head while looking for blueprints and page-templates at \n\n" + node + "\n\n", e);
        }
    }

    public LinkedHashSet<String>  getLinks() {
        return this.links;
    }

    public LinkedHashSet<String>  getScripts() {
        return this.scripts;
    }
}

package com.beligum.blocks.core.parsing;
import com.beligum.blocks.core.models.AbstractIdentifiableElement;
import com.beligum.blocks.core.models.Block;
import com.beligum.blocks.core.models.Row;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.lang.String;import java.lang.StringBuilder;import java.lang.System;
import java.util.*;

/**
 * Created by bas on 30.09.14.
 * Parser class
 */
public class ElementParser
{
    /**
     * Parses a html-file to a string containing velocity-variables for each row- and block-element of the html-tree
     * @param htmlFile The file containing a html-tree
     * @param velocityTemplate A StringBuilder which will contain the velocityTemplate after invoking this method
     * @return a map holding key-value pairs of (id, blockString) and (id, rowString)
     * @throws IOException when
     */
    public Set<AbstractIdentifiableElement> toVelocity(File htmlFile, StringBuilder velocityTemplate) throws IOException, ElementParserException
    {
        Document htmlDOM = Jsoup.parse(htmlFile, null);
        Elements rows = htmlDOM.select(".row");
        Elements blocks = htmlDOM.select(".block");
        Set<AbstractIdentifiableElement> rowsAndBlocks = recursiveParse(htmlDOM);
        velocityTemplate.replace(0, velocityTemplate.length(), htmlDOM.outerHtml());
        return rowsAndBlocks;
    }

    /**
     * Turn xhtml into normal html using JSoup
     * @param xhtml the xhtml to parse
     * @return a string containing html
     */
    public String toHtml(String xhtml){
        //re-turn the xhtml to html using Jsoup
        Document returningXhtmlDOM = Jsoup.parse(xhtml, "", Parser.xmlParser());
        String html = returningXhtmlDOM.outerHtml();
        return html;
    }

    /**
     * Parses the tree starting with the node-element, looking for row- and block-elements and adding them to the map (
     * @param node root of the tree to be parsed
     * @return a set holding blocks and rows
     */
    private Set<AbstractIdentifiableElement> recursiveParse(Element node) throws ElementParserException
    {
        Set<AbstractIdentifiableElement> rowsAndBlocks = new HashSet<AbstractIdentifiableElement>();
        Elements children = node.children();
        for(Element child : children){
            //recursively iterate over the subtree starting with this child and add the found blocks an rows to the map
            rowsAndBlocks.addAll(recursiveParse(child));
            //TODO BAS: only can-edit and can-layout blocks and rows should be parsed
            boolean isRow = child.classNames().contains("row");
            boolean isBlock = child.classNames().contains("block");
            if(isRow || isBlock){
                if(child.id() != null && !child.id().isEmpty()) {
                    String childHtml = child.outerHtml();
                    String uid = child.id();
                    //TODO BAS: uid needs to be rendered here!!!
                    rowsAndBlocks.add(isRow ? new Row(childHtml, uid) : new Block(childHtml, uid));
                    //DEBUG feature
                    System.out.println("\n (" + child.id() + ", " + childHtml + ") \n");
                    //no baseUri specified
                    child.replaceWith(new TextNode("\n ${" + child.id() + "}\n", ""));
                }
                else{
                    //if no id i
                    throw new ElementParserException("A row- or block-element in the html-tree doesn't have an id, this shouldn't happen: \n" + child.outerHtml());
                }
            }
            else{
                //do nothing
            }
        }
        return rowsAndBlocks;
    }

}

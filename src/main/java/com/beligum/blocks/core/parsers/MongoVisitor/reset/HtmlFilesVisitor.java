package com.beligum.blocks.core.parsers.MongoVisitor.reset;

import com.beligum.blocks.core.caching.BlueprintsCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.nosql.PageTemplate;
import com.beligum.blocks.core.models.nosql.Blueprint;
import com.beligum.blocks.core.mongocache.TemplateCache;
import com.beligum.blocks.core.parsers.ElementParser;
import com.beligum.blocks.core.parsers.visitors.SuperVisitor;
import com.beligum.core.framework.utils.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;

/**
 * Created by wouter on 16/03/15.
 *
 * Used to create from html files blueprints
 */


public class HtmlFilesVisitor extends SuperVisitor
{
    private boolean parsingPageTemplate = false;
    private String language;


    public HtmlFilesVisitor(String language) {
        this.language = language;
    }

    @Override
    public Node head(Node node, int depth) throws ParseException {
        try {
            if(node instanceof Element) {

                if (ElementParser.isPageTemplateRootNode((Element)node)) {
                    this.parsingPageTemplate = true;
                }
                else if (parsingPageTemplate && ElementParser.isPageTemplateContentNode((Element)node)) {
                    this.parsingPageTemplate = false;
                    BlocksConfig.getInstance().getTemplateCache().addPageTemplate(BlocksConfig.getInstance().getDatabase().createPageTemplate((Element) node, this.language));
                }

                if(ElementParser.isTypeOf((Element) node)) {
                    if (!ElementParser.isBlueprint((Element)node)) {
                        node.attr(ParserConstants.USE_BLUEPRINT, ElementParser.getTypeOf((Element) node));
                    }
                    else {
                        node.attr(ParserConstants.USE_BLUEPRINT, node.attr(ParserConstants.BLUEPRINT));
                        node.removeAttr(ParserConstants.BLUEPRINT);
                    }
                    BlocksConfig.getInstance().getTemplateCache().addBlueprint(BlocksConfig.getInstance().getDatabase().createBlueprint((Element) node, this.language));
                } else if (ElementParser.isBlueprint((Element)node)) {
                    node.attr(ParserConstants.USE_BLUEPRINT, node.attr(ParserConstants.BLUEPRINT));
                    node.removeAttr(ParserConstants.BLUEPRINT);
                    BlocksConfig.getInstance().getTemplateCache().addBlueprint(BlocksConfig.getInstance().getDatabase().createBlueprint((Element) node, this.language));
                }

                boolean hasBlueprintType = ElementParser.isBlueprint((Element) node) || ElementParser.isUseBlueprint((Element)node);
                //if a blueprint node is not a property of it's parent, place a property on it using it's blueprint type
                if(hasBlueprintType) {
                    if (! ElementParser.isProperty((Element) node)) {
                        String type = ElementParser.getBlueprintName((Element) node);
                        node.attr(ParserConstants.PROPERTY, type);
                    }

                }



                if (node.nodeName().equals("link")) {
                    //if an include has been found, import the wanted html-file
                    if (node.hasAttr("href") && node.attr("rel").equals(ParserConstants.INCLUDE)) {
                        Element element = (Element) node;
                        String source = node.attr("href");
                        Document sourceDOM = getSource(source);
                        node = includeSource(element, sourceDOM);
                    }
                }


            }
            return node;
        }
        catch (Exception e){
            Logger.error(e);
            throw new ParseException("Could not parse tag-head while looking for blueprints and page-templates at \n\n" + node + "\n\n", e);
        }
    }

//    @Override
//    public Node tail(Node node, int depth) throws ParseException
//    {
//        node = super.tail(node, depth);
//        //if we reached the end of a new page-template, cache it
//        if(isPageTemplateRootNode(node)){
//            if(this.pageTemplateContentNode != null) {
//                this.cachePageTemplate(this.pageTemplateContentNode);
//            }
//            else{
//                throw new ParseException("Haven't found a content-node for page-template '" + getPageTemplateName(node) + "'.");
//            }
//        }
//        //if we reached an entity-node, determine it's entity-class and if needed, create a new entity-instance
//        if (node instanceof Element && isEntity(node)) {
//            try {
//                Element element = (Element) node;
//
//                Blueprint blueprint = null;
//                if(containsClassToBeCached(element)){
//                    blueprint = cacheEntityTemplateClassFromNode(element);
//                    /*
//                     * If we have cached a new blueprint which is a child of a parent entity,
//                     * we switch it by a use-blueprint-tag, to be filled in again when the defaults are made (in DefaultVisitor)
//                     */
//                    if(this.blueprintTypeStack.size()>0 && isBlueprint(element) && blueprint != null){
//                        node = replaceNodeWithUseBlueprintTag(element, blueprint.getName());
//                    }
//
//                }
//
//
//
//            }
//            catch (Exception e) {
//                throw new ParseException("Could not parse an " + Blueprint.class.getSimpleName() + " from " + Node.class.getSimpleName() + ": \n \n" + node + "\n \n", e);
//            }
//        }
//        return node;
//
//    }
}

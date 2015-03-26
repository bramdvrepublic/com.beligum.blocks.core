package com.beligum.blocks.core.parsers.visitors.reset;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.parsers.ElementParser;
import com.beligum.blocks.core.parsers.redis.visitors.SuperVisitor;
import com.beligum.core.framework.utils.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

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
                    Blocks.templateCache().addPageTemplate(Blocks.factory().createPageTemplate((Element) node, this.language));
                }

                if(ElementParser.isTypeOf((Element) node)) {
                    if (!ElementParser.isBlueprint((Element)node)) {
                        node.attr(ParserConstants.USE_BLUEPRINT, ElementParser.getTypeOf((Element) node));
                    }
                    else {
                        node.attr(ParserConstants.USE_BLUEPRINT, node.attr(ParserConstants.BLUEPRINT));
                        node.removeAttr(ParserConstants.BLUEPRINT);
                    }
                    Blocks.templateCache().addBlueprint(Blocks.factory().createBlueprint((Element) node, this.language));
                } else if (ElementParser.isBlueprint((Element)node)) {
                    node.attr(ParserConstants.USE_BLUEPRINT, node.attr(ParserConstants.BLUEPRINT));
                    node.removeAttr(ParserConstants.BLUEPRINT);
                    Blocks.templateCache().addBlueprint(Blocks.factory().createBlueprint((Element) node, this.language));
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

}

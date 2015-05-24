package com.beligum.blocks.parsers.visitors.reset;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.parsers.ElementParser;
import com.beligum.blocks.parsers.visitors.BasicVisitor;
import com.beligum.blocks.utils.UrlTools;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Created by wouter on 16/03/15.
 * <p/>
 * Used to create from html files blueprints
 */

public class HtmlFilesVisitor extends BasicVisitor
{
    private boolean parsingPageTemplate = false;
    private Locale language;

    public HtmlFilesVisitor(Locale language)
    {
        this.language = language;
    }

    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        try {
            if (node instanceof Element) {
                // parse the prefixes
                if (ElementParser.isPrefix((Element) node)) {
                    this.parsePrefixes(ElementParser.getPrefix((Element) node));
                    node.removeAttr(ParserConstants.PREFIX);
                }

                if (ElementParser.isPageTemplateRoot((Element) node)) {
                    this.parsingPageTemplate = true;
                    Element base = new Element(Tag.valueOf("base"), Blocks.config().getSiteDomain().toString());
                    base.attr("href", Blocks.config().getSiteDomain().toString());
                    ((Element) node).prependChild(base);
                    node = base;
                }
                else if (parsingPageTemplate && ElementParser.isPageTemplateContentNode((Element) node)) {
                    this.parsingPageTemplate = false;
                    Blocks.templateCache().addPageTemplate(Blocks.factory().createPageTemplate((Element) node, this.language));
                }

                // TypeOf has to be a blueprint
                if(ElementParser.isTypeOf((Element) node)) {
                    node.attr(ParserConstants.TYPE_OF, UrlTools.createLocalType(ElementParser.getTypeOf((Element) node)));
                    if (!ElementParser.isBlueprint((Element)node)) {
                        String typeOf = ElementParser.getTypeOf((Element)node);
                        Path typeofPath = Paths.get(typeOf);
                        typeOf = typeofPath.getName(typeofPath.getNameCount()-1).toString();
                        node.attr(ParserConstants.BLUEPRINT, typeOf);
                    }
                }

                if (ElementParser.isBlueprint((Element) node)) {
                    node.attr(ParserConstants.USE_BLUEPRINT, node.attr(ParserConstants.BLUEPRINT));
                }

                //if a blueprint node is not a property of it's parent, place a property on it using it's blueprint type
                if (ElementParser.isUseBlueprint((Element) node) && !(ElementParser.isProperty((Element) node))) {
                    String type = ElementParser.getBlueprintName((Element) node);
                    node.attr(ParserConstants.PROPERTY, UrlTools.createLocalType(type));
                }


                if (ElementParser.isBlueprint((Element) node)) {
                    Blocks.templateCache().addBlueprint(Blocks.factory().createBlueprint((Element) node, this.language));
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
        catch (Exception e) {
            Logger.error(e);
            throw new ParseException("Could not parse tag-head while looking for blueprints and page-templates at \n\n" + node + "\n\n", e);
        }
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        return node;
    }

}

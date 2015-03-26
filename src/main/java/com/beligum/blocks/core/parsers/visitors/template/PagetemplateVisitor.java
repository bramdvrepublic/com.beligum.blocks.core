package com.beligum.blocks.core.parsers.visitors.template;

import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.parsers.ElementParser;
import com.beligum.core.framework.utils.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Created by wouter on 19/03/15.
 */
public class PagetemplateVisitor extends BlueprintVisitor
{

    private String pageTemplateName = ParserConstants.DEFAULT_PAGE_TEMPLATE;

    public PagetemplateVisitor(String name) {
        if (name != null) this.pageTemplateName = name;
    }

    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        try {
            if (node instanceof Element && ElementParser.isProperty((Element)node)) {
                // check if singleton and if not add
                if (ElementParser.isUseBlueprint((Element) node) && !ElementParser.isSingleton((Element) node)) {
                    node.attr(ParserConstants.SINGLETON, ElementParser.getBlueprintName((Element) node) + "/" + this.pageTemplateName);
                }
            }

            node = super.head(node,depth);
            Node retVal = node;
            if(node instanceof Element) {

                if (((Element) node).tagName().toUpperCase().equals("HEAD")) {
                    ((Element) node).append(ParserConstants.TEMPLATE_HEAD);
                } else if (ElementParser.isPageTemplateContentNode((Element) node)) {
                    retVal = new TextNode(ParserConstants.TEMPLATE_CONTENT, null);
                    node.replaceWith(retVal);
                }

            }
            return retVal;
        }
        catch (Exception e){
            Logger.error(e);
            throw new ParseException("Could not parse tag-head while looking for blueprints and page-templates at \n\n" + node + "\n\n", e);
        }
    }


}

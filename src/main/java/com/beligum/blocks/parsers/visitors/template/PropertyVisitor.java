package com.beligum.blocks.parsers.visitors.template;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.BasicTemplate;
import com.beligum.blocks.parsers.ElementParser;
import com.beligum.blocks.parsers.visitors.BasicVisitor;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;

/**
* Created by wouter on 16/03/15.
*/
public class PropertyVisitor extends BasicVisitor
{

    private BasicTemplate template;

    public PropertyVisitor(BasicTemplate template) {
        this.template = template;
    }

    public BasicTemplate getTemplate() {
        return this.template;
    }

    @Override
    public Node head(Node node, int depth) throws ParseException
    {

        // if property do something
        Node retVal = node;
        if (ElementParser.isProperty((Element) node)) {
            String propertyName = ElementParser.getProperty((Element) node);

            retVal = this.replacePropertyWithID((Element) node, propertyName);
            BasicTemplate property = null;
            if (ElementParser.isSingleton((Element) node)) {
                property = Blocks.factory().createSingleton((Element) node.clone(), ElementParser.getLanguage((Element) node));
                this.getTemplate().add(propertyName, property);
            } else {
                if (!StringUtils.isEmpty(propertyName)) {
                    this.getTemplate().addProperty(propertyName, (Element) node.clone());
                }
                else if (this.getTemplate().isWrapper()) {
                    throw new ParseException("Wrapper blueprint can only have 1 empty property");
                }
                else {
                    this.getTemplate().setWrapper(true);
                }
            }
        }

        return retVal;
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        return node;
    }


//    public ArrayList<BasicTemplate> getProperties() {
//        return this.properties;
//    }

    protected Node replacePropertyWithID(Element element, String key)
    {
        if (key == null)
            key = "";
        String templateKey = ParserConstants.TEMPLATE_PROPERTY_START + key + ParserConstants.TEMPLATE_PROPERTY_END;
        Node textNode = new TextNode(templateKey, null);
        element.replaceWith(textNode);
        return textNode;
    }
}

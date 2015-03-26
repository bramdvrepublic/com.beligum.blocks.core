package com.beligum.blocks.core.parsers.visitors.template;

import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.BasicTemplate;
import com.beligum.blocks.core.parsers.ElementParser;
import com.beligum.blocks.core.parsers.redis.visitors.SuperVisitor;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.LinkedHashMap;

/**
 * Created by wouter on 16/03/15.
 */
public class PropertyVisitor extends SuperVisitor
{
    LinkedHashMap<String, BasicTemplate> properties = new LinkedHashMap<>();

    @Override
    public Node head(Node node, int depth) throws ParseException
    {

        // if property do something
        Node retVal = node;
        if (ElementParser.isProperty((Element) node)) {
            String propertyName = ElementParser.getProperty((Element)node);
            retVal = this.replacePropertyWithID((Element) node, ElementParser.getPropertyKey(propertyName, properties.keySet()));

            BasicTemplate property = new BasicTemplate((Element)node.clone());


            this.properties.put(ElementParser.getPropertyKey(propertyName, properties.keySet()), property);
        }

        return retVal;
    }


    public LinkedHashMap<String, BasicTemplate> getProperties() {
        return this.properties;
    }

    protected Node replacePropertyWithID(Element element, String key) {
        String templateKey = ParserConstants.TEMPLATE_PROPERTY_START + key + ParserConstants.TEMPLATE_PROPERTY_END;
        Node textNode = new TextNode(templateKey, null);
        element.replaceWith(textNode);
        return textNode;
    }
}

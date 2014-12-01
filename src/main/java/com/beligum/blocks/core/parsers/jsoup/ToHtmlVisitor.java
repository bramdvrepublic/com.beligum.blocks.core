package com.beligum.blocks.core.parsers.jsoup;

import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
* Created by wouter on 23/11/14.
*/
public class ToHtmlVisitor extends AbstractVisitor
{

    public Node doHead(Node node, int depth) throws ParseException
    {
        if (node instanceof Element) {
            Element element = (Element)node;
            String resourceId = getReferencedId(element);
            if (!StringUtils.isEmpty(resourceId)) {
                try {
                    RedisID id = new RedisID(resourceId);
                    EntityTemplate resource = Redis.getInstance().fetchEntityTemplate(id);
                    if (resource != null) {
                        Document resourceDOM = Jsoup.parse(resource.getTemplate());
                        node.replaceWith(this.doHead(resourceDOM, depth + 1));
                    }
                    else {
                        throw new ParseException("Could not replace reference to'" + resourceId + "'. It is not a child of the current entity being parsed.");
                    }
                }
                catch(ParseException e){
                    throw e;
                }
                catch(Exception e){
                    throw new ParseException("Error while parsing node '" + node.nodeName() + "' at tree depth '" + depth + "'.", e);
                }
            }
        }
        super.head(node, depth);
        return node;
    }



//    public void mergeElement(Element element) {
//        // check inheritance of mutability
//        // mutability of entity prevails??
//        Element content = this.getContent(element); // DB data
//        Element defaultValue = getDefaultValue(element);
//        if (content != null) {
//            if (AbstractParser.hasTypeOf(element)) {
//                // if element not editable, keep layout
//                // but(!!) we will keep the content of the saved block
//                // DON't CARE ABOUT MUTABILITY property
//                // LOOK AT ENTITY BLUEPRINT
//                // IF Entity is mutable, get from DB
//                // if ENTITY not completely mutable, copy only properties from DB
//                if (!AbstractParser.isMutable(defaultValue)) {
//                   copyContent(content, defaultValue);
//                   content = defaultValue;
//                }
//
//            } else if (AbstractParser.isProperty(element)) {
//                // Is property but not type
//                // TODO: discussion
//                // For now we look nu further
//                // (you can not have properties inside untyped properties
//                if (!AbstractParser.isMutable(element)) {
//                    content = defaultValue;
//                }
//            }
//        } else {
//            content = defaultValue;
//        }
//
//        // try to merge content
//        // If not bootstrap layout , then rip layout from new
//        // if bootstrap layout and new is different:
//        // then match row-row, column - container/column,
//        // row/column or column/row
//        // altijd er in steken ipv vervangen
////        if (AbstractParser.isBootstrapRow(content) && AbstractParser.isBootstrapRow(element)) {
////
////        } else if (AbstractParser.isBootstrapRow(element) && AbstractParser.isBootstrapColumn(content)) {
////
////        }
//        if (content != null) {
//            element.replaceWith(content);
//        }
//    }
}

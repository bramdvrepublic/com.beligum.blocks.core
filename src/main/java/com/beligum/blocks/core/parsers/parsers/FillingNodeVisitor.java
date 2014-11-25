package com.beligum.blocks.core.parsers.parsers;

/**
 * Created by wouter on 23/11/14.
 */
public class FillingNodeVisitor extends AbstractEntityNodeVisitor
{
//    Element filledNode;
//
//    public void head(Node node, int depth) {
//        if (node instanceof Element) {
//            Element element = (Element)node;
//            if (filledNode == null) {
//                this.filledNode = element;
//            }
//            if (AbstractParser.isReference((Element)node)) {
//                this.mergeElement(element);
//            }
//            super.head(node, depth);
//        }
//    }
//
//
//    /*
//    *
//    * */
//    private Element getContent(Element node) {
//        Element retVal = null;
//        EntityID id = EntityID.parse(AbstractParser.getResource(node));
//        if (id.hasImplementationID()) {
//            retVal = BlocksDBFactory.instance().db().get(id);
//        }
//
//        return retVal;
//    }
//
//    private Element getDefaultValue(Element node) {
//        Content content = new Content(node, getlastParent(), getPropertyCount(node));
//        return TypeCacher.instance().getContent(content);
//
//    }
//
//    public void mergeElement(Element element) {
//        // check inheritance of mutability
//        // mutability of entity prevails??
//        Element content = this.getContent(element); // DB data
//        Element defaultValue = getDefaultValue(element);
//        if (content != null) {
//            if (AbstractParser.isType(element)) {
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
//
//
//    private void copyContent(Element sourceElement, Element targetElement) {
//        HashMap<String, Element> source = mapChildren(sourceElement.select("div[reference]"));
//        HashMap<String, Element> target = mapChildren(targetElement.select("div[reference]"));
//        for (String key: source.keySet()) {
//            Element srcEL = source.get(key);
//            Element trgEL = target.get(key);
//            if (srcEL != null & trgEL != null) {
//                trgEL.attr("resource", srcEL.attr("resource"));
//                trgEL.attr("about", srcEL.attr("about"));
//            }
//        }
//    }
//
//
//    // TODO WOUTER:  MAP FOR MULTIPLE PROPERTIES IN ONE ENTITY
//    private HashMap<String, Element> mapChildren(List<Element> source) {
//        HashMap<String, Element> retVal = new HashMap<String, Element>();
//
//        for (Element child: source) {
//            if (AbstractParser.isProperty(child)) {
//                retVal.put(AbstractParser.getProperty(child), child);
//            }
//        }
//        return retVal;
//    }
}

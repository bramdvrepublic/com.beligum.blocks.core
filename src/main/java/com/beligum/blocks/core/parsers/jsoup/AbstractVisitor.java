package com.beligum.blocks.core.parsers.jsoup;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;

import java.util.List;
import java.util.Stack;

/**
 * Created by wouter on 23/11/14.
 */
public class AbstractVisitor
{
    private Stack<Node> typeOfStack = new Stack<>();


    public Node head(Node node, int depth) throws ParseException
    {
        if (isEntity(node)) {
            typeOfStack.push(node);
        }
        return node;
    }

    public Node tail(Node node, int depth) throws ParseException
    {
        if (isEntity(node)) {
            typeOfStack.pop();
        }
        return node;
    }


    protected Node getlastParent() {
        if (!this.typeOfStack.empty()) {
            return this.typeOfStack.peek();
        } else {
            return null;
        }
    }

    protected Element replaceElementWithReference(Element element, EntityTemplate entityTemplate) {
        return replaceElementWithReference(element, entityTemplate.getTemplateVariableName());
    }

    protected Element replaceElementWithReference(Element element, String referenceTo)
    {
        Element replacementNode = new Element(element.tag(), BlocksConfig.getSiteDomain());
        replacementNode.attributes().addAll(element.attributes());
        replacementNode.attr(ParserConstants.REFERENCE_TO, referenceTo);
        element.replaceWith(replacementNode);
        return replacementNode;
    }






    //_______________________Methods to analyze html elements______________________

    /**
     *
     * @param node
     * @return the id of the entity this node is a reference to, an empty string if their is no id present or null if it not a reference-node
     */
    protected String getReferencedId(Node node){
        if(isReference(node)){
            return node.attr(ParserConstants.REFERENCE_TO);
        }
        else{
            return null;
        }
    }

    public boolean isReference(Node node) {
        return node.hasAttr(ParserConstants.REFERENCE_TO);
    }

    /**
     * Checks if the specified node is an entity, which is true if it has an rdf-attribute "typeof" or "property".
     * @param node
     * @return true if this node is the root node of an entity, false otherwise
     */
    public boolean isEntity(Node node)
    {
        return hasTypeOf(node) || isProperty(node);
    }

    /**
     *
     * @param node
     * @return true if the specified element has a rdf-"typeof" attribute, false otherwise
     */
    public boolean hasTypeOf(Node node) {
        boolean retVal = false;
        if (node.hasAttr("typeof")) {
            retVal = true;
        }
        return retVal;
    }

    /**
     *
     * @param node
     * @return true if the specified element has a rdf-"property" attribute, false otherwise
     */
    public boolean isProperty(Node node) {
        boolean retVal = false;
        if (node.hasAttr("property")) {
            retVal = true;
        }
        return retVal;
    }

    /**
     *
     * @param entityNode the root node of an entity
     * @return return the rdf "typeof" value of a node, the default "typeof" if it is a property-node or null if it is not an entity
     */
    public String getTypeOf(Node entityNode) {
        if (hasTypeOf(entityNode)) {
            return entityNode.attr("typeof");
        }
        else if(isProperty(entityNode)){
            return ParserConstants.DEFAULT_ENTITY_TEMPLATE_CLASS;
        }
        else{
            return null;
        }
    }


    public boolean isPageTemplateRoot(Node root){
        return root.hasAttr(ParserConstants.PAGE_TEMPLATE_ATTR);
    }

    /**
     *
     * @param root
     * @return true if the specified element has a rdf-"typeof" attribute, false otherwise
     */
    public boolean isEntityTemplateRoot(Node root){
        return  isEntity(root);
    }

    public boolean hasResource(Element element) {
        boolean retVal = false;
        if (element.hasAttr("resource")) {
            retVal = true;
        }
        return retVal;
    }

    /**
     *
     * @param node
     * @return true if the specified element is "html" and has an attribute "template", false otherwise
     */
    public boolean isTemplate(Node node) {
        return node.nodeName().equals("html") && node.hasAttr(ParserConstants.PAGE_TEMPLATE_ATTR);
    }

    public boolean isLayoutable(Element node) {
        boolean retVal = false;
        if (node.hasAttr("can-layout")) {
            retVal = true;
        }
        return retVal;
    }

    public boolean isReplaceable(Element node) {
        boolean retVal = false;
        if (node.hasAttr("can-replace")) {
            retVal = true;
        }
        return retVal;
    }

    public boolean isBootstrapContainer(Element node) {
        return node.hasClass("container");
    }

    public boolean isBootstrapRow(Element node) {
        return node.hasClass("row");
    }

    public boolean isBootstrapColumn(Element node) {
        boolean retVal = false;
        for (String cName: node.classNames()) {
            if (cName.startsWith("col-")) {
                retVal = true;
                break;
            }
        }
        return retVal;
    }

    public boolean isBootstrapLayout(Element node) {
        return isBootstrapRow(node) || isBootstrapContainer(node) || isBootstrapColumn(node);
    }
    public boolean isBootstrapLayout(List<Element> elements) {
        boolean retVal = true;
        for (Element el: elements) {
            if (!isBootstrapLayout(el)) {
                retVal = false;
                break;
            }
        }
        return retVal;
    }





    public String getProperty(Element node) {
        return node.attr("property");
    }



    public boolean isBlueprint(Element node) {
        boolean retVal = false;
        if (node.hasAttr("blueprint")) {
            retVal = true;
        }
        return retVal;
    }

    public boolean isEditable(Element node) {
        boolean retVal = false;
        if (node.hasAttr("can-edit")) {
            retVal = true;
        }
        return retVal;
    }

    public boolean isInlineEditable(Element node) {
        boolean retVal = false;
        if (node.hasAttr("can-edit-inline")) {
            retVal = true;
        }
        return retVal;
    }



    public boolean isMutable(Element node) {
        return isLayoutable(node) || isReplaceable(node) || isEditable(node) || isInlineEditable(node);
    }


}

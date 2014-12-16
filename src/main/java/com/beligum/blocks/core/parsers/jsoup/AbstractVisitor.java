package com.beligum.blocks.core.parsers.jsoup;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Stack;

/**
 * Created by wouter on 23/11/14.
 */
public class AbstractVisitor
{
    protected Stack<Node> typeOfStack = new Stack<>();
    protected URL parentUrl = null;
    protected URL pageUrl = null;


    public Node head(Node node, int depth) throws ParseException
    {
        if (isEntity(node)) {
            typeOfStack.push(node);
        }
        return node;
    }

    public Node tail(Node node, int depth) throws ParseException
    {
        try {
            if (isEntity(node)) {
                typeOfStack.pop();
                if (parentUrl == null && typeOfStack.isEmpty() && hasResource(node)) {
                    parentUrl = new URL(getResource(node));
                }
            }
            return node;
        }catch(MalformedURLException e){
            throw new ParseException("Bad resource found: " + getResource(node), e);
        }
    }

    /**
     *
     * @return the resource-url of the first entity-parent which has a resource found in the html currently being parsed
     */
    public URL getParentUrl(){
        return parentUrl;
    }

    /**
     *
     * @return the node containing the type of the last typed parent visited
     */
    protected String getParentType() {
        if (!this.typeOfStack.empty()) {
            return getTypeOf(this.typeOfStack.peek());
        } else {
            return null;
        }
    }

    /**
     *
     * @param element
     * @return an entity-template wich is the property that was found in the specified element
     * @throws ParseException
     */
    protected Element replaceElementWithPropertyReference(Element element) throws ParseException
    {
        return replaceElementWithReference(element, getPropertyId(element));
    }

    protected Element replaceElementWithEntityReference(Element element, EntityTemplate entity){
        if(isEntity(element) && StringUtils.isEmpty(getResource(element))){
            element.attr(ParserConstants.RESOURCE, entity.getUrl().toString());
        }
        //TODO: sometimes here an absolute (versioned) template should be saved, when we don't always want to show the last version of a referenced entity
        return replaceElementWithReference(element, entity.getUnversionedId());
    }

    /**
     *
     * @param element
     * @param referenceTo
     * @return the replacement node, or the specified element if it could not be replaced
     */
    protected Element replaceElementWithReference(Element element, String referenceTo)
    {
        if(referenceTo != null && (referenceTo.contentEquals(ParserConstants.PAGE_TEMPLATE_ENTITY_VARIABLE_NAME) || RedisID.isRedisId(referenceTo))) {
            Element replacementNode = new Element(element.tag(), BlocksConfig.getSiteDomain());
            replacementNode.attributes().addAll(element.attributes());
            replacementNode.attr(ParserConstants.REFERENCE_TO, referenceTo);
            element.replaceWith(replacementNode);
            return replacementNode;
        }
        else{
            return element;
        }
    }

    /**
     * Replace the specified node with the root-node of an entity-template
     * @param node
     * @param replacement
     * @return the root-node of the replacement template, or the specified node itself when a null-replacement was specified
     */
    protected Node replaceReferenceWithEntity(Node node, EntityTemplate replacement)
    {
        if (replacement != null) {
            Document templateDOM = Jsoup.parse(replacement.getTemplate(), BlocksConfig.getSiteDomain(), Parser.xmlParser());
            Node replacementHtmlRoot = templateDOM.child(0);
            node.replaceWith(replacementHtmlRoot);
            return replacementHtmlRoot;
        }
        else{
            return node;
        }
    }







    //_______________________Methods to analyze html elements______________________

    /**
     *
     * @param node
     * @return the id of the entity this node is a reference to, an empty string if their is no id present or null if it not a reference-node
     */
    public String getReferencedId(Node node){
        if(isReference(node)){
            return node.attr(ParserConstants.REFERENCE_TO);
        }
        else{
            return null;
        }
    }

    public boolean isReference(Node node) {
        if(node == null){
            return false;
        }
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
        if(node == null){
            return false;
        }
        boolean retVal = false;
        if (node.hasAttr(ParserConstants.TYPE_OF)) {
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
        if(node == null){
            return false;
        }
        boolean retVal = false;
        if (node.hasAttr(ParserConstants.PROPERTY)) {
            retVal = true;
        }
        return retVal;
    }

    /**
     *
     * @param node
     * @return true if the specified node ha a rdf-"resource" attribute, false otherwise
     */
    public boolean hasResource(Node node){
        if(node == null){
            return false;
        }
        else{
            return node.hasAttr(ParserConstants.RESOURCE);
        }
    }

    /**
     *
     * @param node
     * @return the value of the rdf-"resource" attribute, which can be empty, or null if no such attribute can be found
     */
    public String getResource(Node node){
        if(hasResource(node)){
            return node.attr(ParserConstants.RESOURCE);
        }
        else{
            return null;
        }
    }

    /**
     *
     * @param entityNode the root node of an entity
     * @return return the rdf "typeof" value of a node, the default "typeof" if it is a property-node or null if it is not an entity
     */
    public String getTypeOf(Node entityNode) {
        if (hasTypeOf(entityNode)) {
            return entityNode.attr(ParserConstants.TYPE_OF);
        }
        else if(isProperty(entityNode)){
            return ParserConstants.DEFAULT_ENTITY_TEMPLATE_CLASS;
        }
        else{
            return null;
        }
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
        if(element == null){
            return false;
        }
        boolean retVal = false;
        if (element.hasAttr(ParserConstants.RESOURCE)) {
            retVal = true;
        }
        return retVal;
    }

    /**
     *
     * @param node
     * @return true if the specified element is "html" and has an attribute "template", false otherwise
     */
    public boolean isPageTemplateRootNode(Node node) {
        if(node == null){
            return false;
        }
        else {
            return node.nodeName().equals("html") && node.hasAttr(ParserConstants.PAGE_TEMPLATE_ATTR);
        }
    }

    /**
     *
     * @param node
     * @return the page-template name specified in the node, an empty string if no name has been specified, or null if no "template"-attribute can be found
     */
    public String getPageTemplateName(Node node){
        if(node == null){
            return null;
        }
        else{
            return node.attr(ParserConstants.PAGE_TEMPLATE_ATTR);
        }
    }

    /**
     *
     * @param node
     * @return true if the specified node has the attribute "template-content"
     */
    public boolean isPageTemplateContentNode(Node node){
        if(node == null){
            return false;
        }
        else{
            return node.hasAttr(ParserConstants.PAGE_TEMPLATE_CONTENT_ATTR);
        }
    }

    /**
     *
     * @param node
     * @return the value of the property-attribute of the given node, or empty string if no such value can be found
     */
    public String getProperty(Node node) {
        if(isProperty(node)) {
            return node.attr(ParserConstants.PROPERTY);
        }
        else{
            return null;
        }
    }


    public boolean isBlueprint(Node node) {
        if(node == null){
            return false;
        }
        boolean retVal = false;
        if (node.hasAttr(ParserConstants.BLEUPRINT)) {
            retVal = true;
        }
        return retVal;
    }

    public boolean isLayoutable(Node node) {
        if(node == null){
            return false;
        }
        boolean retVal = false;
        if (node.hasAttr(ParserConstants.CAN_LAYOUT)) {
            retVal = true;
        }
        return retVal;
    }

    public boolean isEditable(Node node) {
        if(node == null){
            return false;
        }
        boolean retVal = false;
        if (node.hasAttr(ParserConstants.CAN_EDIT)) {
            retVal = true;
        }
        return retVal;
    }


    /**
     * @return the property-id using the property-name of this entity-node and the last class-node visited (of the form "blocks://[db-alias]/[parent-typeof]#[property-name]_[optional-html-id-value]:[version]"), or null if no property-name can be found
     */
    public String getPropertyId(Node node) throws ParseException
    {
        if(isEntity(node)) {
            String parentEntityClassName = this.getParentType();
            if(parentEntityClassName == null){
                return null;
            }
            else {
                String propertyName = getProperty(node);
                if(StringUtils.isEmpty(propertyName)){
                    return null;
                }
                try {
                    return RedisID.renderNewPropertyId(parentEntityClassName, propertyName).getUnversionedId();

                }catch(IDException e){
                    throw new ParseException("Could not render new property-id.", e);
                }
            }
        }
        else{
            return null;
        }

    }


//    public boolean isBootstrapContainer(Element node) {
//        if(node == null){
//            return false;
//        }
//        return node.hasClass("container");
//    }
//
//    public boolean isBootstrapRow(Element node) {
//        if(node == null){
//            return false;
//        }
//        return node.hasClass("row");
//    }
//
//    public boolean isBootstrapColumn(Element node) {
//        if(node == null){
//            return false;
//        }
//        boolean retVal = false;
//        for (String cName: node.classNames()) {
//            if (cName.startsWith("col-")) {
//                retVal = true;
//                break;
//            }
//        }
//        return retVal;
//    }
//
//    public boolean isBootstrapLayout(Element node) {
//        return isBootstrapRow(node) || isBootstrapContainer(node) || isBootstrapColumn(node);
//    }
//    public boolean isBootstrapLayout(List<Element> elements) {
//        boolean retVal = true;
//        for (Element el: elements) {
//            if (!isBootstrapLayout(el)) {
//                retVal = false;
//                break;
//            }
//        }
//        return retVal;
//    }

}

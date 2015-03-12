package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.Blueprint;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by wouter on 23/11/14.
 */
public class SuperVisitor
{
    protected Stack<Node> blueprintTypeStack = new Stack<>();
    protected URL parentUrl = null;
    protected URL entityUrl = null;


    public Node head(Node node, int depth) throws ParseException
    {
        if (hasBlueprintType(node)) {
            blueprintTypeStack.push(node);
        }
        return node;
    }

    public Node tail(Node node, int depth) throws ParseException
    {
        try {
            if (hasBlueprintType(node)) {
                blueprintTypeStack.pop();
                if (parentUrl == null && blueprintTypeStack.isEmpty() && hasResource(node)) {
                    parentUrl = new URL(getResource(node));
                }
            }
            return node;
        }catch(MalformedURLException e){
            throw new ParseException("Bad resource found: " + getResource(node), e, node);
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
    protected String getParentType() throws ParseException
    {
        if (!this.blueprintTypeStack.empty()) {
            return getBlueprintType(this.blueprintTypeStack.peek());
        } else {
            return null;
        }
    }

    /**
     *
     * @return the last typed parent-node visited
     */
    protected Node getParent(){
        if(!this.blueprintTypeStack.empty()){
            return this.blueprintTypeStack.peek();
        }
        else{
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

    protected Element replaceElementWithEntityReference(Element element, EntityTemplate entity) throws ParseException
    {
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
        if(referenceTo != null && (referenceTo.contentEquals(ParserConstants.PAGE_TEMPLATE_ENTITY_VARIABLE_NAME) || BlocksID.isBlocksId(referenceTo))) {
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
     * Replace an element with that same element having an attribute "use-blueprint" and with no children.
     * @param element
     * @return the replacement-element
     */
    protected Element replaceNodeWithUseBlueprintTag(Element element, String entityClassName){
        if(element!=null){
            Element replacement = new Element(element.tag(), BlocksConfig.getSiteDomain());
            replacement.attributes().addAll(element.attributes());
            replacement.removeAttr(ParserConstants.BLUEPRINT);
            replacement.attr(ParserConstants.USE_BLUEPRINT, entityClassName);
            element.replaceWith(replacement);
            return replacement;
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
    protected Node replaceNodeWithEntity(Node node, EntityTemplate replacement)
    {
        if (node!= null && replacement != null) {
            Element replacementHtmlRoot= TemplateParser.parse(replacement.getTemplate()).child(0);
            if(StringUtils.isEmpty(replacementHtmlRoot.attr(ParserConstants.RESOURCE))){
                replacementHtmlRoot.attr(ParserConstants.RESOURCE, replacement.getUrl().toString());
            }
            node.replaceWith(replacementHtmlRoot);
            return replacementHtmlRoot;
        }
        else{
            return node;
        }
    }


    /**
     * Include the html found in the file found at {@param sourcePath} and replace the element {@param at} by the found tags.
     * @param at
     * @param source
     * @throws java.io.IOException
     * @throws ParseException
     */
    public Node includeSource(Element at, Document source) throws IOException, ParseException
    {
        if(source.childNodes().isEmpty()){
            throw new ParseException("Cannot include an empty file.", at);
        }
        Node firstChild = source.childNode(0);
        Element parent = at.parent();
        if(parent == null){
            throw new ParseException("Cannot use an include as a root-node.", at);
        }
        int siblingIndex = at.siblingIndex();
        parent.insertChildren(siblingIndex, source.childNodes());
        at.remove();
        return firstChild;
    }

    public Document getSource(String sourcePath) throws IOException
    {
        try(InputStream input = this.getClass().getResourceAsStream(sourcePath)) {
            String content = "";
            List<String> lines = IOUtils.readLines(input);
            for (String line : lines) {
                content += line + "\n";
            }
            Document source = TemplateParser.parse(content);
            if(source.childNodes().isEmpty()){
                Logger.warn("Found empty file at '" + sourcePath + "'.");
            }
            return source;
        }
    }

    public Node setUseBlueprintType(Node node){
        String type = node.attr(ParserConstants.BLUEPRINT);
        node.removeAttr(ParserConstants.BLUEPRINT);
        if(!StringUtils.isEmpty(type) && !node.hasAttr(ParserConstants.USE_BLUEPRINT)) {
            node.attr(ParserConstants.USE_BLUEPRINT, type);
        }
        return node;
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
    public boolean isEntity(Node node) throws ParseException
    {
        return hasBlueprintType(node) || isProperty(node);
    }

    /**
     *
     * @param node
     * @return true if the specified element has a rdf-"typeof" attribute, false otherwise
     * @throws com.beligum.blocks.core.exceptions.ParseException if an empty blueprint or use-blueprint attribute was found
     */
    public boolean hasBlueprintType(Node node) throws ParseException
    {
        if (node == null) {
            return false;
        }
        else if(node.hasAttr(ParserConstants.BLUEPRINT)){
            if (StringUtils.isEmpty(node.attr(ParserConstants.BLUEPRINT))) {
                throw new ParseException("Found empty blueprint type.", node);
            }
            else{
                return true;
            }
        }
        else if(node.hasAttr(ParserConstants.USE_BLUEPRINT)){
            if (StringUtils.isEmpty(node.attr(ParserConstants.USE_BLUEPRINT))) {
                throw new ParseException("Found empty use-blueprint type.", node);
            }
            else{
                return true;
            }
        }
        else{
            return false;
        }
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
    public String getBlueprintType(Node entityNode) throws ParseException
    {
        if (hasBlueprintType(entityNode)) {
            String retVal =  entityNode.attr(ParserConstants.BLUEPRINT);
            if(StringUtils.isEmpty(retVal)){
                retVal = entityNode.attr(ParserConstants.USE_BLUEPRINT);
            }
            return retVal;
        }
        else if(isProperty(entityNode)){
            return ParserConstants.DEFAULT_BLUEPRINT;
        }
        else{
            return null;
        }
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

    public boolean hasTypeOf(Node node) throws ParseException
    {
        if(node == null){
            return false;
        }
        else if(node.hasAttr(ParserConstants.TYPE_OF)){
            if(StringUtils.isEmpty(node.attr(ParserConstants.TYPE_OF))){
                throw new ParseException("Found empty typeof attribute at node", node);
            }
            else{
                return true;
            }
        }
        else{
            return false;
        }
    }

    public String getTypeOf(Node node){
        if(node == null){
            return null;
        }
        else{
            return node.attr(ParserConstants.TYPE_OF);
        }
    }

    public String getBlueprintCssClass(Node node) throws ParseException
    {
        String type = getBlueprintType(node);
        if(StringUtils.isEmpty(type)){
            return null;
        }
        else{
            return ParserConstants.CSS_CLASS_PREFIX + type;
        }
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

    public String getPropertyKey(Node node){
        if(isProperty(node)){
            String propertyKey = getProperty(node);
            if(node.hasAttr(ParserConstants.PROPERTY_NAME)) {
                propertyKey = propertyKey + "/" + node.attr(ParserConstants.PROPERTY_NAME);
            }
            return propertyKey;
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
        if (node.hasAttr(ParserConstants.BLUEPRINT)) {
            retVal = true;
        }
        return retVal;
    }

    public boolean isLayoutable(Element element) {
        if(element == null){
            return false;
        }
        boolean retVal = false;
        if (element.hasAttr(ParserConstants.CAN_LAYOUT)) {
            retVal = true;
        }
        return retVal;
    }

    public boolean isEditable(Element element) {
        if(element == null){
            return false;
        }
        boolean retVal = false;
        if (element.hasAttr(ParserConstants.CAN_EDIT_PROPERTY)) {
            retVal = true;
        }
        return retVal;
    }

    public boolean isNotEditable(Element element) {
        if(element == null){
            return false;
        }
        boolean retVal = false;
        if (element.hasAttr(ParserConstants.CAN_NOT_EDIT_PROPERTY)) {
            retVal = true;
        }
        return retVal;
    }

    public boolean isChangable(Element element) {
        if(element == null){
            return false;
        }
        boolean retVal = false;
        if (element.hasAttr(ParserConstants.CAN_CHANGE)) {
            retVal = true;
        }
        return retVal;
    }

    public boolean isModifiable(Element element) {
        return isEditable(element) || isLayoutable(element) || isChangable(element);
    }

    public ParserConstants.ModificationLevel getModificationLevel(Element element){
        if(isEditable(element)){
            return ParserConstants.ModificationLevel.CAN_EDIT;
        }
        else if(isLayoutable(element)){
            return ParserConstants.ModificationLevel.CAN_LAYOUT;
        }
        else{
            return ParserConstants.ModificationLevel.NONE;
        }
    }


    /**
     * @return the property-id using the property-name of this entity-node and the last class-node visited (of the form "blocks://[db-alias]/[parent-typeof]#[property-name]:[version]"), or null if no property-name can be found
     */
    public String getPropertyId(Node node) throws ParseException
    {
        if(isEntity(node)) {
            String parentEntityClassName = this.getParentType();
            if(parentEntityClassName == null){
                return null;
            }
            else {
                String propertyValue = getProperty(node);
                if(StringUtils.isEmpty(propertyValue)){
                    return null;
                }
                try {
                    return BlocksID.renderClassPropertyId(parentEntityClassName, propertyValue, getPropertyName(node), BlocksID.NO_LANGUAGE).getUnversionedId();

                }catch(Exception e){
                    throw new ParseException("Could not render new property-id.", e, node);
                }
            }
        }
        else{
            return null;
        }

    }

    /**
     * Get the "name" attribute of a property-tag. Used for guaranteeing the uniqueness of a class-property-id
     * @param node
     */
    public String getPropertyName(Node node){
        if(isProperty(node)){
            return node.attr(ParserConstants.PROPERTY_NAME);
        }
        else{
            return null;
        }
    }

    public Node copyModificationLevel(Element from, Element to){
        ParserConstants.ModificationLevel modificationLevel = getModificationLevel(from);
        String previousLevel = getModificationLevel(to).toString();
        if(!StringUtils.isEmpty(previousLevel)) {
            to.removeClass(previousLevel);
        }
        if(!StringUtils.isEmpty(modificationLevel.toString())) {
            to.addClass(modificationLevel.toString());
        }
        return to;
    }

    public boolean needsBlueprintCopy(Node node){
        if(node == null){
            return false;
        }
        else if(!node.hasAttr(ParserConstants.USE_BLUEPRINT)){
            return false;
        }
        else{
            //empty tags need a blueprint copy, when other elements are already inside the node, no blueprint copy is needed
            List<Node> childNodes = node.childNodes();
            if(childNodes.isEmpty()){
                return true;
            }
            else {
                boolean needsBlueprint = true;
                int i = 0;
                while (needsBlueprint && i < childNodes.size()) {
                    needsBlueprint = !(childNodes.get(i) instanceof Element);
                    i++;
                }
                return needsBlueprint;
            }
        }
    }

    /**
     *
     * @param node
     * @return the value of the lang-attribute of the node, or the language of the specified entityTemplateClass if no lang-attribute is present,
     * or the default-language if the blueprint doesn't have a language
     */
    public String getLanguage(Node node, Blueprint blueprint){
        String language = node.attr(ParserConstants.LANGUAGE);
        if(StringUtils.isEmpty(language)){
            language = blueprint != null ? blueprint.getLanguage() : BlocksConfig.getDefaultLanguage();
        }
        if(StringUtils.isEmpty(language)){
            language = BlocksConfig.getDefaultLanguage();
        }
        language = Languages.getStandardizedLanguage(language);
        return language;
    }

    public boolean isPageBlock(Node node){
        return node.hasAttr(ParserConstants.PAGE_BLOCK);
    }

    public boolean isAddableBlock(Node node){
        return !node.hasAttr(ParserConstants.NOT_ADDABLE_BLOCK);
    }

}

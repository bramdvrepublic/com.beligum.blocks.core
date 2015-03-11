package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.Blueprint;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Created by wouter on 22/11/14.
 * Visitor holding all functionalities to parse a html-file to entity-classes stored in cache
 */
public class FindBlueprintsVisitor extends SuperVisitor
{

    //TODO BAS SH: je hebt net deze klasse hernoemt van FindTemplatesVisitor naar CheckBlueprintsVisitor, en dat is wat deze klasse nu ook zou moeten doen

    private String pageTemplateName = null;
    /**flag for indicating if the current traverse has encountered a tag indicating a page-template is being parsed*/
    private boolean parsingPageTemplate = false;
    /**the node of the page-template currently being parsed which has to be replaced with an entity*/
    private Element pageTemplateContentNode = null;
    /**the (css-)linked files that need to be injected*/
    private Stack<List<String>> linksStack = new Stack<>();
    /**the (javascript-)scripts that need to be injected*/
    private Stack<List<String>> scriptsStack = new Stack<>();

    private List<AbstractTemplate> foundTemplates;

    private Set<String> foundEntityClassNames;

    /**
     *
     * @param foundTemplates the list to be filled up with blueprintes and page-templates
     * @throws NullPointerException if no cache is specified
     */
    public FindBlueprintsVisitor(List<AbstractTemplate> foundTemplates, Set<String> foundEntityClassNames)
    {
        if(foundTemplates == null){
            throw new NullPointerException("Cannot cache to null-collection.");
        }
        this.foundTemplates = foundTemplates;
        if(foundEntityClassNames == null){
            throw new NullPointerException("Cannot fill up null-collection.");
        }
        this.foundEntityClassNames = foundEntityClassNames;
        linksStack.push(new ArrayList<String>());
        scriptsStack.push(new ArrayList<String>());
    }

    @Override
    public Node head(Node node, int depth) throws ParseException {
        try {
            node = super.head(node, depth);
            if(node instanceof Element) {
                if (isPageTemplateRootNode(node)) {
                    this.pageTemplateName = getPageTemplateName(node);
                    this.parsingPageTemplate = true;
                }
                else if (parsingPageTemplate && isPageTemplateContentNode(node) && node instanceof Element) {
                    pageTemplateContentNode = (Element) node;
                }

                boolean hasBlueprintType = hasBlueprintType(node);
                if(hasTypeOf(node)) {
                    if (!hasBlueprintType) {
                        node.attr(ParserConstants.BLUEPRINT, getTypeOf(node));
                        hasBlueprintType = true;
                        this.blueprintTypeStack.push(node);
                    }
                    else if(!getBlueprintType(node).equals(getTypeOf(node))){
                        throw new ParseException("Cannot deal with entity of type '" + getTypeOf(node) + "' and blueprint type '" + getBlueprintType(node) + "'. For now those types should be equal. Found at node \n\n" +node + "\n\n");
                    }
                }

                //TODO BAS!: css class blocks-entityClassName er bij zetten indien nodig
                //TODO BAS!: fill children (properties: propertyValue/propertyName of propertyValue/number-> EtntityTemplate) vb building en building/1 en building/2...

                if (hasBlueprintType) {
                    //if a blueprint should be cached, set a links- and a scripts-list ready to be filled during next steps
                    boolean containsClassToBeCached = containsClassToBeCached(node);
                    if (containsClassToBeCached) {
                        linksStack.push(new ArrayList<String>());
                        scriptsStack.push(new ArrayList<String>());
                    }
                }

                //if we find a use-blueprint attribute which does not have an entity parent, warn user for data loss, since that html will not be picked up (since no blueprint is defined)
                if(node.hasAttr(ParserConstants.USE_BLUEPRINT) && this.blueprintTypeStack.size() < 2 && !parsingPageTemplate){
                    Logger.warn("Found a " + ParserConstants.USE_BLUEPRINT + " attribute which is not a child of an entity. The layer will be ignored: \n\n" + node + "\n\n");
                }

                //add links and scripts to the stack and remove them from the html (to be re-injected later)
                if (node.nodeName().equals("link")) {
                    //if an include has been found, import the wanted html-file
                    if (node.hasAttr("href") && node.attr("rel").equals(ParserConstants.INCLUDE)) {
                        Element element = (Element) node;
                        String source = node.attr("href");
                        Document sourceDOM = getSource(source);
                        node = includeSource(element, sourceDOM);
                    }
                    //if not, add the link to the links-stack
                    else {
                        this.linksStack.peek().add(node.outerHtml());
                        Node emtpyNode = new TextNode("", null);
                        node.replaceWith(emtpyNode);
                        node = emtpyNode;
                    }
                }
                //if a script has been found, add it to the scripts-stack
                if (node.nodeName().equals("script")) {
                    this.scriptsStack.peek().add(node.outerHtml());
                    Node emtpyNode = new TextNode("", null);
                    node.replaceWith(emtpyNode);
                    node = emtpyNode;
                }
            }
            return node;
        }
        catch (Exception e){
            throw new ParseException("Could not parse tag-head while looking for blueprints and page-templates at \n\n" + node + "\n\n", e);
        }
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        node = super.tail(node, depth);
        //if we reached the end of a new page-template, cache it
        if(isPageTemplateRootNode(node)){
            if(this.pageTemplateContentNode != null) {
                this.cachePageTemplate(this.pageTemplateContentNode);
            }
            else{
                throw new ParseException("Haven't found a content-node for page-template '" + getPageTemplateName(node) + "'.");
            }
        }
        //if we reached an entity-node, determine it's entity-class and if needed, create a new entity-instance
        if (node instanceof Element && isEntity(node)) {
            try {
                Element element = (Element) node;

                Blueprint blueprint = null;
                if(containsClassToBeCached(element)){
                    blueprint = cacheEntityTemplateClassFromNode(element);
                    /*
                     * If we have cached a new blueprint which is a child of a parent entity,
                     * we switch it by a use-blueprint-tag, to be filled in again when the defaults are made (in DefaultVisitor)
                     */
                    if(this.blueprintTypeStack.size()>0 && isBlueprint(element) && blueprint != null){
                        node = replaceNodeWithUseBlueprintTag(element, blueprint.getName());
                    }

                }

                //if a blueprint node is not a property of it's parent, place a property on it using it's blueprint type
                if(hasBlueprintType(element) && !isProperty(element) && this.blueprintTypeStack.size()>0){
                    String type = getBlueprintType(element);
                    node.attr(ParserConstants.PROPERTY, type);
                }

            }
            catch (Exception e) {
                throw new ParseException("Could not parse an " + Blueprint.class.getSimpleName() + " from " + Node.class.getSimpleName() + ": \n \n" + node + "\n \n", e);
            }
        }
        return node;

    }



    /**
     *
     * @param node
     * @return true if the node is the root-node of a class that should be cached (that is, when it is a blueprint), false otherwise
     */
    private boolean containsClassToBeCached(Node node) throws CacheException, IDException, ParseException
    {
        if(!isEntity(node)){
            return false;
        }
        else{
            return isBlueprint(node);
        }
    }

    /**
     * Caches the entity-template parsed from the root-element specified. If a certain (non-blueprint) implementation of this entity-class is already present in cache, it is replaced.
     * If a blueprint was already present, an exception is thrown.
     * @param classRoot node defining an blueprint
     * @return the blueprint defined by the node
     * @throws ParseException
     */
    private Blueprint cacheEntityTemplateClassFromNode(Element classRoot) throws ParseException
    {
        String entityClassName = "";
        try {
            entityClassName = this.getBlueprintType(classRoot);
            if(!StringUtils.isEmpty(entityClassName)) {
                //if a template is explicitly mentioned, that one is used, otherwise we use the page-template from the file we are parsing (specified at <html tempalte="name">), or the default if no page-template is currently being parsed
                String pageTemplateName = getPageTemplateName(classRoot);
                if(StringUtils.isEmpty(pageTemplateName)){
                    pageTemplateName = this.pageTemplateName;
                }

                String language = getLanguage(classRoot, null);
                List<String> links = this.linksStack.pop();
                List<String> scripts = this.scriptsStack.pop();
                Blueprint blueprint = new Blueprint(entityClassName, language, classRoot.outerHtml(), pageTemplateName, links, scripts);
                this.foundTemplates.add(blueprint);
                this.foundEntityClassNames.add(entityClassName);
                return blueprint;
            }
            else{
                throw new ParseException("Found + " + Node.class.getSimpleName() + " which doesn't define an entity.", classRoot);
            }
        }
        catch(ParseException e){
            throw e;
        }
        catch(Exception e){
            throw new ParseException("Error while creating new entity-class '" + entityClassName +"'.", e);
        }
    }

    private Node cachePageTemplate(Element contentNode) throws ParseException
    {
        try {
            if (isPageTemplateContentNode(contentNode)) {
                Node parent = contentNode.parent();
                //initialize the page-template name by searching for the first template-attribute we find before the specified node and take the value of that attribute to be the name
                String templateName = "";
                while (parent.parent() != null) {
                    if (parent.nodeName().equals("html")) {
                        templateName = parent.attr(ParserConstants.PAGE_TEMPLATE_ATTR);
                    }
                    Node nextParent = parent.parent();
                    if(nextParent != null) {
                        parent = nextParent;
                    }
                }
                /**
                 * Replace the content of the template temporarily to a reference-block, so we can distill the page-template in the tag-head.
                 * Afterwards we want to return the html-tree to it's original form, without the reference-block.
                 * All of this is done so we can give a page-template to the first entity encountered.
                 */
                Node replacement = this.replaceElementWithReference(contentNode, ParserConstants.PAGE_TEMPLATE_ENTITY_VARIABLE_NAME);
                String language = getLanguage(parent, null);
                List<String> links = linksStack.pop();
                List<String> scripts = scriptsStack.pop();
                PageTemplate pageTemplate = new PageTemplate(templateName, language, parent.outerHtml(), links, scripts);
                replacement.replaceWith(contentNode);
                this.foundTemplates.add(pageTemplate);
            }
            else{
                throw new ParseException("Found node which is not a page-template-content-node, this should be impossible. At: \n \n " + contentNode + "\n \n");
            }
            return contentNode;
        }
        catch (Exception e) {
            throw new ParseException("Something went wrong while creating page-template.", e);
        }
    }
}

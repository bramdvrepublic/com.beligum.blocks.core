package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Created by wouter on 22/11/14.
 * Visitor holding all functionalities to parse a html-file to entity-classes stored in cache
 */
public class BlueprintVisitor extends AbstractVisitor
{

    private String pageTemplateName = null;
    /**flag for indicating if the current traverse has encountered a tag indicating a page-template is being parsed*/
    private boolean parsingPageTemplate = false;
    /**the node of the page-template currently being parsed which has to be replaced with an entity*/
    private Element pageTemplateContentNode = null;
    /**the (css-)linked files that need to be injected*/
    private Stack<List<String>> linksStack = new Stack<>();
    /**the (javascript-)scripts that need to be injected*/
    private Stack<List<String>> scriptsStack = new Stack<>();

    private List<AbstractTemplate> cache;

    /**
     *
     * @param cache the list to be filled up with entity-template-classes and page-templates
     * @throws NullPointerException if no cache is specified
     */
    public BlueprintVisitor(List<AbstractTemplate> cache)
    {
        if(cache == null){
            throw new NullPointerException("Cannot cache to null-collection.");
        }
        this.cache = cache;
        linksStack.push(new ArrayList<String>());
        scriptsStack.push(new ArrayList<String>());
    }

    @Override
    public Node head(Node node, int depth) throws ParseException {
        try {
            node = super.head(node, depth);
            if (isPageTemplateRootNode(node)) {
                this.pageTemplateName = getPageTemplateName(node);
                this.parsingPageTemplate = true;
            }
            else if (parsingPageTemplate && isPageTemplateContentNode(node) && node instanceof Element) {
                pageTemplateContentNode = (Element) node;
            }
            if(hasTypeOf(node)) {
                boolean containsClassToBeCached = containsClassToBeCached(node);
                if(containsClassToBeCached) {
                    linksStack.push(new ArrayList<String>());
                    scriptsStack.push(new ArrayList<String>());
                }
            }
            //add links and scripts to the stack and remove them from the html (to be re-injected later)
            if(node.nodeName().equals("link")){
                this.linksStack.peek().add(node.outerHtml());
                Node emtpyNode = new TextNode("", null);
                node.replaceWith(emtpyNode);
                node = emtpyNode;
            }
            if(node.nodeName().equals("script")){
                this.scriptsStack.peek().add(node.outerHtml());
                Node emtpyNode = new TextNode("", null);
                node.replaceWith(emtpyNode);
                node = emtpyNode;
            }
            return node;
        }
        catch (Exception e){
            throw new ParseException("Could not parse tag-head while caching at " + node, e);
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
                EntityTemplateClass entityTemplateClass = null;
                //if this element is a class-bleuprint, it must be added to the cache (even if a class with this name was cached before)
                if(containsClassToBeCached(element)){
                    entityTemplateClass = cacheEntityTemplateClassFromNode(element);
                    this.linksStack.pop();
                    this.scriptsStack.pop();
                    /*
                     * If we have cached an new entity-template-class which is a property of a parent entity,
                     * we switch it by a use-blueprint-tag, to be filled in again when the defaults are made (in DefaultVisitor)
                     */
                    if(isProperty(element) && isBlueprint(element) && entityTemplateClass != null){
                        node = replaceNodeWithUseBlueprintTag(element);
                    }
                }

            }
            catch (Exception e) {
                throw new ParseException("Could not parse an " + EntityTemplateClass.class.getSimpleName() + " from " + Node.class.getSimpleName() + ": \n \n" + node + "\n \n", e);
            }
        }
        return node;

    }

    /**
     *
     * @param node
     * @return true if the node is the root-node of a class that should be cached (that is, when it is a blueprint, or when no class with that name is present in cache yet), false otherwise
     */
    private boolean containsClassToBeCached(Node node) throws CacheException, IDException
    {
        if(!isEntity(node)){
            return false;
        }
        if(isBlueprint(node)) {
            return true;
        }
        else{
            String typeOf = getTypeOf(node);
            //if no class of this type can be found, we use the found html as blueprint
            if(!typeOf.equals(ParserConstants.DEFAULT_ENTITY_TEMPLATE_CLASS) && !EntityTemplateClassCache.getInstance().contains(typeOf)){
                return true;
            }
            else{
                return false;
            }
        }
    }

    /**
     * Caches the entity-template parsed from the root-element specified. If a certain (non-blueprint) implementation of this entity-class is already present in cache, it is replaced.
     * If a bleuprint was already present, an exception is thrown.
     * @param classRoot node defining an entity-template-class
     * @return the entity-template-class defined by the node
     * @throws ParseException
     */
    private EntityTemplateClass cacheEntityTemplateClassFromNode(Element classRoot) throws ParseException
    {
        String entityClassName = "";
        try {
            entityClassName = this.getTypeOf(classRoot);
            if(!StringUtils.isEmpty(entityClassName)) {
                //if a template is explicitly mentioned, that one is used, otherwise we use the page-template from the file we are parsing (specified at <html tempalte="name">), or the default if no page-template is currently being parsed
                String pageTemplateName = getPageTemplateName(classRoot);
                if(StringUtils.isEmpty(pageTemplateName)){
                    pageTemplateName = this.pageTemplateName;
                }
                Elements classProperties = classRoot.select("[" + ParserConstants.PROPERTY + "]");
                //the class-root is not a property of this class, so if it contains the "property"-attribute, it is removed from the list
                classProperties.remove(classRoot);
                //since we are sure to be working with class-properties, we now all of them will hold an attribute "property", so we can use this in a comparator to sort all elements according to the property-value
                Collections.sort(classProperties, new Comparator<Element>() {
                    @Override
                    public int compare(Element classProperty1, Element classProperty2) {
                        return getProperty(classProperty1).compareTo(getProperty(classProperty2));
                    }
                });
                for(int i = 1; i<classProperties.size(); i++){
                    Element previousClassProperty = classProperties.get(i-1);
                    String previousClassPropertyValue = getProperty(previousClassProperty);
                    Element classProperty = classProperties.get(i);
                    String classPropertyValue = getProperty(classProperty);
                    if(previousClassPropertyValue.equals(classPropertyValue)){
                        //check if properties with the same attribute-value, have a different name (<div property="something" name="some_thing"></div> and <div property="something"  name="so_me_th_ing"></div> is a correct situation)
                        String previousClassPropertyName = getPropertyName(previousClassProperty);
                        String classPropertyName = getPropertyName(classProperty);
                        if(StringUtils.isEmpty(previousClassPropertyName) || StringUtils.isEmpty(classPropertyName)){
                            throw new ParseException("Found two class-properties with same property-value '" + previousClassPropertyValue + "' and no name-attribute to distinguish them at \n \n" + classRoot + "\n \n");
                        }
                    }
                }
                String language = getLanguage(classRoot, null);
                List<String> links = this.linksStack.peek();
                List<String> scripts = this.scriptsStack.peek();
                EntityTemplateClass entityTemplateClass = new EntityTemplateClass(entityClassName, language, classRoot.outerHtml(), pageTemplateName, links, scripts);
                this.cache.add(entityTemplateClass);
                return entityTemplateClass;
            }
            else{
                throw new ParseException(Node.class.getSimpleName() + " '" + classRoot + "' does not define an entity.");
            }
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
                this.cache.add(pageTemplate);
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

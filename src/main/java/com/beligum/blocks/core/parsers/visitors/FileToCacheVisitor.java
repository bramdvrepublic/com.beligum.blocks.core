package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplate;
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
public class FileToCacheVisitor extends AbstractVisitor
{

    //TODO BAS: split file-to-cache visitor into two visitors, one for extracting the blueprints and cache corresponding classes to cache, a second to instantiate the defaults afterwards

    private String pageTemplateName = null;
    /**flag for indicating if the current traverse has encountered a tag indicating a page-template is being parsed*/
    private boolean parsingPageTemplate = false;
    /**flag for indicating if the current traverse has encountered a tag holding a new class to be cached*/
    private Stack<Boolean> parsingClassToBeCached = new Stack<>();
    /**the node of the page-template currently being parsed which has to be replaced with an entity*/
    private Element pageTemplateContentNode = null;
    /**boolean indicating if we are parsing default entities for a certain page-template*/
    private boolean enteredPageTemplateContentNode = false;
    /**the language of this file, if present*/
    private String pageTemplateLanguage = null;
    /**the (css-)linked files that need to be injected*/
    private Stack<List<String>> linksStack = new Stack<>();
    /**the (javascript-)scripts that need to be injected*/
    private Stack<List<String>> scriptsStack = new Stack<>();

    public FileToCacheVisitor()
    {
        parsingClassToBeCached.push(false);
        linksStack.push(new ArrayList<String>());
        scriptsStack.push(new ArrayList<String>());
    }

    public boolean isParsingClassToBeCached(){
        if(!parsingClassToBeCached.empty()){
            return parsingClassToBeCached.peek();
        }
        else{
            return false;
        }
    }

    @Override
    public Node head(Node node, int depth) throws ParseException {
        try {
            node = super.head(node, depth);
            if (isPageTemplateRootNode(node)) {
                this.pageTemplateName = getPageTemplateName(node);
                this.parsingPageTemplate = true;
                this.pageTemplateLanguage = getLanguage(node, null);
            }
            else if (parsingPageTemplate && isPageTemplateContentNode(node) && node instanceof Element) {
                pageTemplateContentNode = (Element) node;
                this.enteredPageTemplateContentNode = true;
            }
            if(hasTypeOf(node)) {
                boolean containsClassToBeCached = containsClassToBeCached(node);
                parsingClassToBeCached.push(containsClassToBeCached);
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
        if(node == this.pageTemplateContentNode){
            this.enteredPageTemplateContentNode = false;
        }
        //if we reached an entity-node, determine it's entity-class and if needed, create a new entity-instance
        if (node instanceof Element && isEntity(node)) {
            try {
                Element element = (Element) node;
                EntityTemplateClass entityTemplateClass;
                //if this element is a class-bleuprint, it must be added to the cache (even if a class with this name was cached before)
                if(containsClassToBeCached(element)){
                    entityTemplateClass = cacheEntityTemplateClassFromNode(element);
                    this.linksStack.pop();
                    this.scriptsStack.pop();
                }
                else{
                    entityTemplateClass = EntityTemplateClassCache.getInstance().get(getTypeOf(element));
                }
                //if we reached the tail of a node that has been pushed on the parsingClassToBeCached-stack (all typed entities), we remove it
                if(hasTypeOf(node)){
                    this.parsingClassToBeCached.pop();
                }
                if(isProperty(element)) {
                    //if we are parsing a new class to be cached, we should also make new default-properties
                    if(this.isParsingClassToBeCached()) {
                        element.removeAttr(ParserConstants.BLUEPRINT);
                        EntityTemplate propertyInstance;
                        String language = getLanguage(element, entityTemplateClass);
                        if(needsBlueprint(element)) {
                            RedisID propertyId = RedisID.renderNewPropertyId(this.getParentType(), getProperty(element), getPropertyName(element), language);
                            propertyInstance = this.getNewEntityClassCopy(element, propertyId, entityTemplateClass);
                        }
                        else{
                            propertyInstance = new EntityTemplate(RedisID.renderNewPropertyId(this.getParentType(), getProperty(element), getPropertyName(element), language), entityTemplateClass, element.outerHtml());
                        }
                        RedisID lastVersion = new RedisID(propertyInstance.getUnversionedId(), RedisID.LAST_VERSION, language);
                        EntityTemplate storedInstance = Redis.getInstance().fetchEntityTemplate(lastVersion);
                        //if no version is present in db, or this version is different, save to db
                        if (storedInstance == null || !storedInstance.equals(propertyInstance)) {
                            Redis.getInstance().save(propertyInstance);
                        }
                        node = replaceElementWithPropertyReference(element);
                    }
                    //if we're parsing entities belonging to a page-template, we want to create a reproducable id, so we can permanently save changes in db
                    else if(this.parsingPageTemplate && !this.enteredPageTemplateContentNode){
                        RedisID defaultPageTemplateEntityId = RedisID.renderNewPageTemplateDefaultEntity(this.pageTemplateName, getProperty(element), this.pageTemplateLanguage);
                        EntityTemplate lastVersion = (EntityTemplate) Redis.getInstance().fetchLastVersion(defaultPageTemplateEntityId, EntityTemplate.class);
                        //if no version of this entity exists yet, make a new one
                        if(lastVersion == null) {
                            EntityTemplate newDefaultEntity;
                            if(needsBlueprint(element)){
                                defaultPageTemplateEntityId = RedisID.renderNewPageTemplateDefaultEntity(this.pageTemplateName, getProperty(element), this.pageTemplateLanguage);
                                newDefaultEntity = this.getNewEntityClassCopy(element, defaultPageTemplateEntityId, entityTemplateClass);
                            }
                            else{
                                defaultPageTemplateEntityId = RedisID.renderNewPageTemplateDefaultEntity(this.pageTemplateName, getProperty(element), entityTemplateClass.getLanguage());
                                newDefaultEntity = new EntityTemplate(defaultPageTemplateEntityId, entityTemplateClass, element.outerHtml());
                            }
                            Redis.getInstance().save(newDefaultEntity);
                            node = replaceElementWithEntityReference(element, newDefaultEntity);
                        }
                        //if a version has been stored in db before, use that version as page-template-entity (f.i. a menu that has been changed by the user, should stay changed after server-start-up)
                        else{
                            node = replaceElementWithEntityReference(element, lastVersion);
                        }
                    }
                    else if(needsBlueprint(element)){
                        RedisID defaultEntityId = RedisID.renderNewEntityTemplateID(entityTemplateClass, entityTemplateClass.getLanguage());
                        EntityTemplate defaultEntity = this.getNewEntityClassCopy(element, defaultEntityId, entityTemplateClass);
                        Redis.getInstance().save(defaultEntity);
                        node = replaceElementWithEntityReference(element, defaultEntity);
                    }
                    //if no new class is being parsed, we are parsing a default-instance of a certain type
                    else{
                        EntityTemplate defaultEntity = new EntityTemplate(RedisID.renderNewEntityTemplateID(entityTemplateClass, entityTemplateClass.getLanguage()), entityTemplateClass, element.outerHtml());
                        Redis.getInstance().save(defaultEntity);
                        node = replaceElementWithEntityReference(element, defaultEntity);
                    }
                }
                else if(this.typeOfStack.size()>0) {
                    /*
                     * If we find an entity which is not a property, we throw an exception, since this makes no rdf-sense.
                     * However, if the node is the head-node of a class-blueprint, no property-attribute is expected, and so then no error is thrown
                     */
                    if (!(this.typeOfStack.size() == 1 && isBlueprint(this.typeOfStack.peek()))) {
                        throw new ParseException("Found entity-child with typeof-attribute, but no property-attribute at \n \n " + element + "\n \n");
                    }
                }
                else{
                    //do nothing, since we have found the ending of the outer-most typeof-tag
                }
            }
            catch (Exception e) {
                throw new ParseException("Could not parse an " + EntityTemplateClass.class.getSimpleName() + " from " + Node.class.getSimpleName() + ": \n \n" + node + "\n \n", e);
            }
        }
        return node;

    }


    /**
     * Make a new copy of the class-template, using all node-attributes of the node specified.
     * @param node
     * @param id
     * @param entityClass
     * @return
     * @throws IDException
     */
    private EntityTemplate getNewEntityClassCopy(Node node, RedisID id, EntityTemplateClass entityClass) throws IDException, CacheException
    {
        Map<RedisID, String> classTemplates = entityClass.getTemplates();
        Map<RedisID, String> copiedTemplates = new HashMap<>();
        for(RedisID languageId : classTemplates.keySet()){
            Element classRoot = TemplateParser.parse(classTemplates.get(languageId)).child(0);
            classRoot.attributes().addAll(node.attributes());
            classRoot.removeAttr(ParserConstants.USE_BLUEPRINT);
            copiedTemplates.put(languageId, classRoot.outerHtml());
        }
        return new EntityTemplate(id, entityClass, copiedTemplates);
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
     * Caches the entity-template parsed from the root-element specified. If a certain implementation of this entity-class is already present in cache, it is replaced.
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
                EntityTemplateClassCache.getInstance().replace(entityTemplateClass);
                return EntityTemplateClassCache.getInstance().get(entityClassName);
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
                //we need to instanciate the cache first, so a default-template surely will be cached with an older version than the page-template we're about to make
                PageTemplateCache cache = PageTemplateCache.getInstance();
                String language = getLanguage(parent, null);
                List<String> links = linksStack.pop();
                List<String> scripts = scriptsStack.pop();
                PageTemplate pageTemplate = new PageTemplate(templateName, language, parent.outerHtml(), links, scripts);
                replacement.replaceWith(contentNode);
                boolean added = cache.add(pageTemplate);
                //default page-templates should be added to the cache no matter what, so the last one encountered is kept
                if (!added && this.pageTemplateName.contentEquals(ParserConstants.DEFAULT_PAGE_TEMPLATE)) {
                    PageTemplateCache.getInstance().replace(pageTemplate);
                }
                else if(!added){
                    Logger.warn(PageTemplate.class.getName() + " '" + pageTemplate.getName() + "' was not added to the application-cache, since an other " + PageTemplate.class.getName() +
                                " with the same name was already present.");
                }
            }
            return contentNode;
        }
        catch (Exception e) {
            throw new ParseException("Something went wrong while creating page-template.", e);
        }
    }
}

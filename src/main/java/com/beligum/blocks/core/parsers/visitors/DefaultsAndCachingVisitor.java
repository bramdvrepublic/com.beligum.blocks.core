package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.blocks.core.parsers.Traversor;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Created by bas on 22/01/15.
 */
public class DefaultsAndCachingVisitor extends SuperVisitor
{
    //the language we're parsing
    private String language;

    private final Document root;
    //the type of template we're parsing
    private AbstractTemplate parsingTemplate;

    private Map<String, EntityTemplateClass> allEntityClasses;
    private Map<String, PageTemplate> allPageTemplates;

    /**
     *
     * @param language The language the defaults will have.
     * @param root the root-node of the template being parsed
     * @param parsingTemplate The template this visitor will be visiting for the creation of default values. Only EntityTemplateClass and PageTemplate are supported.
     * @param allEntityClasses A map holding all class-templates to be used to render default-values with.
     * @param allPageTemplates A map holding all page-templates.
     * @throws ParseException If an unknown or empty language is specified and if te type of the parsingTemplate is not supported.
     */
    public DefaultsAndCachingVisitor(String language, Document root, AbstractTemplate parsingTemplate, Map<String, EntityTemplateClass> allEntityClasses, Map<String, PageTemplate> allPageTemplates) throws ParseException
    {
        if(Languages.isNonEmptyLanguageCode(language)) {
            this.language = language;
        }
        else{
            throw new ParseException("Found unknown or empty language '" + language + "'.");
        }
        if(root == null){
            throw new ParseException("Found null-root while initializing visitor.");
        }
        this.root = root;
        if(parsingTemplate instanceof EntityTemplateClass) {
            if(!allEntityClasses.containsKey(parsingTemplate.getName())){
                throw new ParseException("Found unknown entity-class '" + parsingTemplate.getName() + "'.");
            }
            this.parsingTemplate = parsingTemplate;
        }
        else if(parsingTemplate instanceof PageTemplate) {
            if(!allPageTemplates.containsKey(parsingTemplate.getName())){
                throw new ParseException("Found unknown page-template-name '" + parsingTemplate.getName() + "'.");
            }
            this.parsingTemplate = parsingTemplate;
        }
        else{
            throw new ParseException("Cannot visit a template of type " + parsingTemplate.getClass().getSimpleName() + ". Only " + EntityTemplateClass.class.getSimpleName() + " and " + PageTemplate.class + " are supported.");
        }
        this.allEntityClasses = allEntityClasses;
        this.allPageTemplates = allPageTemplates;
    }

    @Override
    public Node head(Node node, int depth) throws ParseException {
        try {
            node = super.head(node, depth);
            return node;
        }
        catch (Exception e){
            throw new ParseException("Could not parse tag-head while setting defaults at \n \n" + node + "\n \n", e);
        }
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        try {
            node = super.tail(node, depth);
            if (node instanceof Element) {
                //if we reached an entity-node, determine it's entity-class and if needed, create a new entity-instance
                if (isEntity(node)) {
                    Element element = (Element) node;
                    /*
                     * For properties of the document being parsed it needs to be checked if they need to be saved to db.
                     * However if this is the root-node (of the document being parsed), the presence of a property-attribute only means the blueprint of a certain class was defined inside another one.
                     */
                    if (isProperty(element) && !root.equals(element.parent())) {
                        String typeOf = getTypeOf(element);
                        EntityTemplateClass entityTemplateClass = allEntityClasses.get(typeOf);
                        if (entityTemplateClass == null) {
                            throw new ParseException("Found unknown entity-class '" + typeOf + "' at node \n \n " + element + "\n \n");
                        }
                        //we found an uncached entity-template-class, so we cache it
                        if (!EntityTemplateClassCache.getInstance().contains(typeOf) && !parsingTemplate.getName().equals(typeOf)) {
                            Document entityTemplateClassDOM = TemplateParser.parse(entityTemplateClass.getTemplate());
                            Traversor
                                            traversor =
                                            new Traversor(new DefaultsAndCachingVisitor(entityTemplateClass.getLanguage(), entityTemplateClassDOM, entityTemplateClass, allEntityClasses,
                                                                                        allPageTemplates));
                            traversor.traverse(entityTemplateClassDOM);
                        }
                        //we want to use the cached entity-template-class
                        entityTemplateClass = EntityTemplateClassCache.getInstance().get(typeOf);
                        //for entity-template-classes, new default-properties should be constructed
                        if (this.parsingTemplate instanceof EntityTemplateClass) {
                            if (this.parsingTemplate.getName().equals(this.getParentType())) {
                                String language = getLanguage(element, entityTemplateClass);
                                if (needsBlueprint(element)) {
                                    RedisID propertyId = RedisID.renderNewPropertyId(this.getParentType(), getProperty(element), getPropertyName(element), language);
                                    node = this.saveNewEntityClassCopy(element, propertyId, entityTemplateClass);
                                }
                                else {
                                    RedisID propertyId = RedisID.renderNewPropertyId(this.getParentType(), getProperty(element), getPropertyName(element), language);
                                    node = this.saveNewEntity(element, propertyId);
                                }
                            }
                            else {
                                //do nothing, since we have found an entity that is not a direct child of the template being parsed, this sort of entity will be taken care of while saving a new entity-default
                            }
                        }
                        //if we're parsing entities belonging to a page-template, we want to create a reproducable id, so we can permanently save changes in db
                        else if (this.parsingTemplate instanceof PageTemplate) {
                            //only entities at entity-depth 1 should be given a page-template-default id and saved to db
                            if (getParentType() == null) {
                                RedisID defaultPageTemplateEntityId = RedisID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), this.language);
                                EntityTemplate lastVersion = (EntityTemplate) Redis.getInstance().fetchLastVersion(defaultPageTemplateEntityId, EntityTemplate.class);
                                //if no version of this entity exists yet, make a new one
                                if (lastVersion == null) {
                                    if (needsBlueprint(element)) {
                                        defaultPageTemplateEntityId = RedisID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), this.language);
                                        node = this.saveNewEntityClassCopy(element, defaultPageTemplateEntityId, entityTemplateClass);
                                    }
                                    else {
                                        defaultPageTemplateEntityId =
                                                        RedisID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), entityTemplateClass.getLanguage());
                                        node = this.saveNewEntity(element, defaultPageTemplateEntityId);
                                    }
                                }
                                //if a version has been stored in db before, use that version as page-template-entity (f.i. a menu that has been changed by the user, should stay changed after server-start-up)
                                else {
                                    node = replaceElementWithEntityReference(element, lastVersion);
                                }
                            }
                            else {
                                //do nothing, since we have found an entity that is not a direct child of the template being parsed, this sort of entity will be taken care of while saving a new entity-default
                            }
                        }
                    }
                    else if (this.typeOfStack.size() > 0) {
                    /*
                     * If we find an entity which is not a property, we throw an exception, since this makes no rdf-sense.
                     * However, if the node is the head-node of a class-blueprint, no property-attribute is expected, and so then no error is thrown
                     */
                        if (!(this.typeOfStack.size() == 1 && isBlueprint(this.typeOfStack.peek()))) {
                            throw new ParseException("Found entity-child with typeof-attribute, but no property-attribute at \n \n " + element + "\n \n");
                        }
                    }
                    //we reached the tail of the outer-most tag of an entity-template-class, so we cache it to the application-cache
                    else if (parsingTemplate instanceof EntityTemplateClass) {
                        this.cacheEntityTemplateClass(element);
                    }
                    else {
                        throw new ParseException("Unexpected behaviour at node \n \n" + node + "\n \n");
                    }
                }
                //if we reached the end of a page-template, cache it
                else if("html".equals(node.nodeName()) && parsingTemplate instanceof PageTemplate){
                    this.cachePageTemplate((Element) node);
                }
            }
            return node;
        }
        catch (Exception e) {
            throw new ParseException("Could not parse an " + EntityTemplateClass.class.getSimpleName() + " from " + Node.class.getSimpleName() + ": \n \n" + node + "\n \n", e);
        }
    }

    private void cacheEntityTemplateClass(Element root) throws ParseException, CacheException, IDException
    {
        checkPropertyUniqueness(root);
        root.removeAttr(ParserConstants.BLUEPRINT);
        EntityTemplateClass parsingTemplate = (EntityTemplateClass) this.parsingTemplate;
        /*
         * Use all info from the template we're parsing to make a real entity-template-class to be cached.
         * The correct template of this class to be cached has just been created in this defaults-visitor and can thus be found at element.outerHtml().
         */
        EntityTemplateClass entityTemplateClass = new EntityTemplateClass(parsingTemplate.getName(), this.language, root.outerHtml(), parsingTemplate.getPageTemplateName(), parsingTemplate.getLinks(), parsingTemplate.getScripts());
        boolean added = EntityTemplateClassCache.getInstance().add(entityTemplateClass);
        if(!added) {
            if (entityTemplateClass.getName().equals(ParserConstants.DEFAULT_ENTITY_TEMPLATE_CLASS)) {
                if(!EntityTemplateClassCache.getInstance().get(entityTemplateClass.getName()).equals(entityTemplateClass)) {
                    EntityTemplateClassCache.getInstance().replace(entityTemplateClass);
                    Logger.warn("Replaced default-" + EntityTemplateClass.class.getSimpleName() + ".");
                }
            }
            else {
                throw new ParseException("Could not add " + EntityTemplateClass.class.getSimpleName() + " '" + entityTemplateClass.getName() + "' to application cache. This shouldn't happen.");
            }
        }
    }

    private void cachePageTemplate(Element root) throws ParseException, CacheException, IDException
    {
        checkPropertyUniqueness(root);
        PageTemplate pageTemplate = new PageTemplate(parsingTemplate.getName(), this.language, root.outerHtml(), parsingTemplate.getLinks(), parsingTemplate.getScripts());
        boolean added = PageTemplateCache.getInstance().add(pageTemplate);
        if(!added){
            if(pageTemplate.getName().equals(ParserConstants.DEFAULT_PAGE_TEMPLATE)){
                if(!PageTemplateCache.getInstance().get(pageTemplate.getName()).equals(pageTemplate)){
                    PageTemplateCache.getInstance().replace(pageTemplate);
                    Logger.warn("Replaced default-" + PageTemplate.class.getSimpleName() + ".");
                }
            }
            else {
                throw new ParseException("Could not add " + PageTemplate.class.getSimpleName() + " '" + pageTemplate.getName() + "' to application cache. This shouldn't happen.");
            }
        }
    }


    /**
     * Make a new copy of the class-template, using all node-attributes of the node specified and return a node referencing to that template
     * @param element
     * @param id
     * @param entityClass
     * @throws com.beligum.blocks.core.exceptions.IDException
     */
    private Element saveNewEntityClassCopy(Element element, RedisID id, EntityTemplateClass entityClass) throws IDException, CacheException, ParseException
    {
        Map<RedisID, String> classTemplates = entityClass.getTemplates();
        Map<RedisID, String> copiedTemplates = new HashMap<>();
        for(RedisID languageId : classTemplates.keySet()){
            Element classRoot = TemplateParser.parse(classTemplates.get(languageId)).child(0);
            classRoot.attributes().addAll(element.attributes());
            classRoot.removeAttr(ParserConstants.USE_BLUEPRINT);
            //a copy of an entity-class, also means a copy of all of it's children, so we need to traverse all templates to create entity-copies of all it's children
            Traversor traversor = new Traversor(new ClassToStoredInstanceVisitor(id.getUrl(), id.getLanguage()));
            traversor.traverse(classRoot);
            copiedTemplates.put(languageId, classRoot.outerHtml());
        }
        if(entityClass.getTemplate(id.getLanguage())==null){
            //a copy of an entity-class, also means a copy of all of it's children, so we need to traverse all templates to create entity-copies of all it's children
            Element classRoot = TemplateParser.parse(entityClass.getTemplate()).child(0);
            classRoot.attributes().addAll(element.attributes());
            classRoot.removeAttr(ParserConstants.USE_BLUEPRINT);
            Traversor traversor = new Traversor(new ClassToStoredInstanceVisitor(id.getUrl(), id.getLanguage()));
            traversor.traverse(classRoot);
            copiedTemplates.put(id, entityClass.getTemplate());
        }
        element.removeAttr(ParserConstants.USE_BLUEPRINT);
        element = replaceElementWithEntityReference(element, new EntityTemplate(id, entityClass, copiedTemplates));
        return element;
    }

    /**
     * Save the specified node as a new (default) instance to db. The typeof-attribute of the node will determine it's entity-class
     * @param node
     * @param id
     * @return a referencing node to the freshly stored entity
     * @throws IDException
     * @throws CacheException
     * @throws RedisException
     * @throws ParseException
     */
    private Node saveNewEntity(Node node, RedisID id) throws IDException, CacheException, RedisException, ParseException
    {
        /*
         * HtmlToStoredInstance needs a html-document to traverse correctly.
         * We put the root of the entity to be a default into the body and let the visitor save new instances we're needed.
         */
        Document entityRoot = new Document(BlocksConfig.getSiteDomain());
        entityRoot.appendChild(node.clone());
        //traverse the entity-root and save new instances to db
        Traversor traversor = new Traversor(new HtmlToStoreVisitor(id.getLanguagedUrl(), entityRoot));
        traversor.traverse(entityRoot);
        Node entityReference = entityRoot.child(0);
        node.replaceWith(entityReference);
        return entityReference;
    }

    /**
     * Checks if the properties af a template are unique (or have a unique name if multiple equal properties are present).
     * @param templateRoot root-node of a template
     * @return true if all properties are unique, throws {@link ParseException} otherwise.
     * @throws ParseException
     */
    private boolean checkPropertyUniqueness(Element templateRoot) throws ParseException
    {
        Elements properties = templateRoot.select("[" + ParserConstants.PROPERTY + "]");
        //the class-root is not a property of this class, so if it contains the "property"-attribute, it is removed from the list
        properties.remove(templateRoot);
        //since we are sure to be working with class-properties, we now all of them will hold an attribute "property", so we can use this in a comparator to sort all elements according to the property-value
        Collections.sort(properties, new Comparator<Element>() {
            @Override
            public int compare(Element classProperty1, Element classProperty2) {
                return getProperty(classProperty1).compareTo(getProperty(classProperty2));
            }
        });
        for(int i = 1; i<properties.size(); i++){
            Element previousClassProperty = properties.get(i-1);
            String previousClassPropertyValue = getProperty(previousClassProperty);
            Element classProperty = properties.get(i);
            String classPropertyValue = getProperty(classProperty);
            if(previousClassPropertyValue.equals(classPropertyValue)){
                //check if properties with the same attribute-value, have a different name (<div property="something" name="some_thing"></div> and <div property="something"  name="so_me_th_ing"></div> is a correct situation)
                String previousClassPropertyName = getPropertyName(previousClassProperty);
                String classPropertyName = getPropertyName(classProperty);
                if(StringUtils.isEmpty(previousClassPropertyName) || StringUtils.isEmpty(classPropertyName)){
                    throw new ParseException("Found two properties with same property-value '" + previousClassPropertyValue + "' and no name-attribute to distinguish them at \n \n" + templateRoot + "\n \n");
                }
            }
        }
        return true;
    }


}
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
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.blocks.core.parsers.Traversor;
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
    //the type of template we're parsing
    private AbstractTemplate parsingTemplate;

    private Map<String, EntityTemplateClass> allEntityClasses;
    private Map<String, PageTemplate> allPageTemplates;

    /**
     *
     * @param language The language the defaults will have.
     * @param parsingTemplate The template this visitor will be visiting for the creation of default values. Only EntityTemplateClass and PageTemplate are supported.
     * @param allEntityClasses A map holding all class-templates to be used to render default-values with.
     * @param allPageTemplates A map holding all page-templates.
     * @throws ParseException If an unknown or empty language is specified and if te type of the parsingTemplate is not supported.
     */
    public DefaultsAndCachingVisitor(String language, AbstractTemplate parsingTemplate, Map<String, EntityTemplateClass> allEntityClasses, Map<String, PageTemplate> allPageTemplates) throws ParseException
    {
        if(Languages.isNonEmptyLanguageCode(language)) {
            this.language = language;
        }
        else{
            throw new ParseException("Found unknown or empty language '" + language + "'.");
        }
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
        node = super.tail(node, depth);
        //if we reached an entity-node, determine it's entity-class and if needed, create a new entity-instance
        if (node instanceof Element && isEntity(node)) {
            try {
                Element element = (Element) node;
                if(isProperty(element)) {
                    String typeOf = getTypeOf(element);
                    EntityTemplateClass entityTemplateClass = allEntityClasses.get(typeOf);
                    if (entityTemplateClass == null) {
                        throw new ParseException("Found unknown entity-class '" + typeOf + "' at node \n \n " + element + "\n \n");
                    }
                    //we found an uncached entity-template-class, so we cache it
                    if(!EntityTemplateClassCache.getInstance().contains(typeOf)){
                        //TODO BAS SH: you're debugging the new visitor (making default instances). The entity-template-class-part seems already quite all right, only here about a infite loop seems to amerge.
                        Traversor traversor = new Traversor(new DefaultsAndCachingVisitor(entityTemplateClass.getLanguage(), entityTemplateClass, allEntityClasses, allPageTemplates));
                        traversor.traverse(TemplateParser.parse(entityTemplateClass.getTemplate()));
                    }
                    //we want to use the cached entity-template-class
                    entityTemplateClass = EntityTemplateClassCache.getInstance().get(typeOf);
                    //for entity-template-classes, new default-properties should be constructed
                    if(this.parsingTemplate instanceof EntityTemplateClass){
                        if(this.parsingTemplate.getName().equals(this.getParentType())) {
//                            element.removeAttr(ParserConstants.BLUEPRINT);
                            EntityTemplate propertyInstance;
                            String language = getLanguage(element, entityTemplateClass);
                            if (needsBlueprint(element)) {
                                RedisID propertyId = RedisID.renderNewPropertyId(this.getParentType(), getProperty(element), getPropertyName(element), language);
                                node = this.saveNewEntityClassCopy(element, propertyId, entityTemplateClass);
                            }
                            else {
                                RedisID propertyId = RedisID.renderNewPropertyId(this.getParentType(), getProperty(element), getPropertyName(element), language);
                                node = this.saveNewEntityDefault(element, propertyId, entityTemplateClass);
                            }
                        }
                        else{
                            //do nothing, since we have found an entity that is not a direct child of the template being parsed, this sort of entity will be taken care of while saving a new entity-default
                        }
                    }
                    //if we're parsing entities belonging to a page-template, we want to create a reproducable id, so we can permanently save changes in db
                    else if(this.parsingTemplate instanceof PageTemplate && /*TODO BAS: only do this step if in depth '1' of the pagetemplate*/true){
                        RedisID defaultPageTemplateEntityId = RedisID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), this.language);
                        EntityTemplate lastVersion = (EntityTemplate) Redis.getInstance().fetchLastVersion(defaultPageTemplateEntityId, EntityTemplate.class);
                        //if no version of this entity exists yet, make a new one
                        if(lastVersion == null) {
                            if(needsBlueprint(element)){
                                defaultPageTemplateEntityId = RedisID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), this.language);
                                node = this.saveNewEntityClassCopy(element, defaultPageTemplateEntityId, entityTemplateClass);
                            }
                            else{
                                defaultPageTemplateEntityId = RedisID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), entityTemplateClass.getLanguage());
                                EntityTemplate newDefaultEntity = new EntityTemplate(defaultPageTemplateEntityId, entityTemplateClass, element.outerHtml());
                                Redis.getInstance().save(newDefaultEntity);
                                node = replaceElementWithEntityReference(element, newDefaultEntity);
                            }
                        }
                        //if a version has been stored in db before, use that version as page-template-entity (f.i. a menu that has been changed by the user, should stay changed after server-start-up)
                        else{
                            node = replaceElementWithEntityReference(element, lastVersion);
                        }
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
                //we reached the tail of the outer-most tag of an entity-template-class, so we cache it to the application-cache
                else if(parsingTemplate instanceof EntityTemplateClass){
                    this.cacheEntityTemplateClass(element);
                }
                //we reached the tail of the outer-most tag of a page-template, so we cache it to the application-cache
                else if(parsingTemplate instanceof PageTemplate) {
                    this.cachePageTemplate(element);
                }
                else{
                    throw new ParseException("Unexpected behaviour at node \n \n" + node + "\n \n");
                }
            }
            catch (Exception e) {
                throw new ParseException("Could not parse an " + EntityTemplateClass.class.getSimpleName() + " from " + Node.class.getSimpleName() + ": \n \n" + node + "\n \n", e);
            }
        }
        return node;

    }

    private void cacheEntityTemplateClass(Element root) throws ParseException, CacheException, IDException
    {
        checkPropertyUniqueness(root);
        EntityTemplateClass parsingTemplate = (EntityTemplateClass) this.parsingTemplate;
        /*
         * Use all info from the template we're parsing to make a real entity-template-class to be cached.
         * The correct template of this class to be cached has just been created in this defaults-visitor and can thus be found at element.outerHtml().
         */
        EntityTemplateClass entityTemplateClass = new EntityTemplateClass(parsingTemplate.getName(), this.language, root.outerHtml(), parsingTemplate.getPageTemplateName(), parsingTemplate.getLinks(), parsingTemplate.getScripts());
        boolean added = EntityTemplateClassCache.getInstance().add(entityTemplateClass);
        if(!added){
            throw new ParseException("Could not add " + EntityTemplateClass.class.getSimpleName() + " '" + entityTemplateClass.getName() + "' to application cache. This shouldn't happen.");
        }
    }

    private void cachePageTemplate(Element root) throws ParseException, CacheException, IDException
    {
        checkPropertyUniqueness(root);
        PageTemplate pageTemplate = new PageTemplate(parsingTemplate.getName(), this.language, root.outerHtml(), parsingTemplate.getLinks(), parsingTemplate.getScripts());
        boolean added = PageTemplateCache.getInstance().add(pageTemplate);
        if(!added){
            throw new ParseException("Could not add " + PageTemplate.class.getSimpleName() + " '" + pageTemplate.getName() + "' to application cache. This shouldn't happen.");
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
            Traversor traversor = new Traversor(new ClassToStoredInstanceVisitor(id.getUrl(), id.getLanguage()));
            traversor.traverse(classRoot);
            copiedTemplates.put(id, entityClass.getTemplate());
        }
        element = replaceElementWithEntityReference(element, new EntityTemplate(id, entityClass, copiedTemplates));
        return element;
    }

    private Node saveNewEntityDefault(Node node, RedisID id, EntityTemplateClass entityClass) throws IDException, CacheException, RedisException, ParseException
    {
        //traverse the entity-root and save new instances to db
        Traversor traversor = new Traversor(new HtmlToStoreVisitor(id.getLanguagedUrl()));
        /*
         * HtmlToStoredInstance needs a html-document to traverse correctly.
         * We put the root of the entity to be a default into the body and let the visitor save new instances we're needed.
         */
        Document entityRoot = new Document(BlocksConfig.getSiteDomain());
        entityRoot.appendChild(node.clone());
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

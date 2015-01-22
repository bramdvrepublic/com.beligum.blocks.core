package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.blocks.core.parsers.Traversor;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import sun.security.pkcs.ParsingException;

import java.util.*;

/**
 * Created by bas on 22/01/15.
 */
public class DefaultsVisitor extends SuperVisitor
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
    public DefaultsVisitor(String language, AbstractTemplate parsingTemplate, Map<String, EntityTemplateClass> allEntityClasses, Map<String, PageTemplate> allPageTemplates) throws ParseException
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
                    EntityTemplateClass entityTemplateClass = allEntityClasses.get(getTypeOf(element));
                    if(entityTemplateClass == null){
                        throw new ParseException("Found unknown entity-class '" + getTypeOf(element) + "' at node \n \n " + element + "\n \n");
                    }
                    //for entity-template-classes, new default-properties should be constructed
                    if(this.parsingTemplate instanceof EntityTemplateClass) {
                        element.removeAttr(ParserConstants.BLUEPRINT);
                        EntityTemplate propertyInstance;
                        String language = getLanguage(element, entityTemplateClass);
                        if(needsBlueprint(element)) {
                            RedisID propertyId = RedisID.renderNewPropertyId(this.getParentType(), getProperty(element), getPropertyName(element), language);
                            propertyInstance = this.saveNewEntityClassCopy(element, propertyId, entityTemplateClass);
                        }
                        else{
                            propertyInstance = new EntityTemplate(RedisID.renderNewPropertyId(this.getParentType(), getProperty(element), getPropertyName(element), language), entityTemplateClass, element.outerHtml());
                            RedisID lastVersion = new RedisID(propertyInstance.getUnversionedId(), RedisID.LAST_VERSION, language);
                            EntityTemplate storedInstance = Redis.getInstance().fetchEntityTemplate(lastVersion);
                            //if no version is present in db, or this version is different, save to db
                            if (storedInstance == null || !storedInstance.equals(propertyInstance)) {
                                Redis.getInstance().save(propertyInstance);
                            }
                        }
                        node = replaceElementWithEntityReference(element, propertyInstance);
                    }
                    //if we're parsing entities belonging to a page-template, we want to create a reproducable id, so we can permanently save changes in db
                    else if(this.parsingTemplate instanceof PageTemplate){
                        RedisID defaultPageTemplateEntityId = RedisID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), this.language);
                        EntityTemplate lastVersion = (EntityTemplate) Redis.getInstance().fetchLastVersion(defaultPageTemplateEntityId, EntityTemplate.class);
                        //if no version of this entity exists yet, make a new one
                        if(lastVersion == null) {
                            EntityTemplate newDefaultEntity;
                            if(needsBlueprint(element)){
                                defaultPageTemplateEntityId = RedisID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), this.language);
                                newDefaultEntity = this.saveNewEntityClassCopy(element, defaultPageTemplateEntityId, entityTemplateClass);
                            }
                            else{
                                defaultPageTemplateEntityId = RedisID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), entityTemplateClass.getLanguage());
                                newDefaultEntity = new EntityTemplate(defaultPageTemplateEntityId, entityTemplateClass, element.outerHtml());
                                Redis.getInstance().save(newDefaultEntity);
                            }
                            node = replaceElementWithEntityReference(element, newDefaultEntity);
                        }
                        //if a version has been stored in db before, use that version as page-template-entity (f.i. a menu that has been changed by the user, should stay changed after server-start-up)
                        else{
                            node = replaceElementWithEntityReference(element, lastVersion);
                        }
                    }
                    //                    else if(needsBlueprint(element)){
                    //                        RedisID defaultEntityId = RedisID.renderNewEntityTemplateID(entityTemplateClass, entityTemplateClass.getLanguage());
                    //                        EntityTemplate defaultEntity = this.saveNewEntityClassCopy(element, defaultEntityId, entityTemplateClass);
                    //                        node = replaceElementWithEntityReference(element, defaultEntity);
                    //                    }
                    //                    //if no new class is being parsed, we are parsing a default-instance of a certain type
                    //                    else{
                    //                        EntityTemplate defaultEntity = new EntityTemplate(RedisID.renderNewEntityTemplateID(entityTemplateClass, entityTemplateClass.getLanguage()), entityTemplateClass, element.outerHtml());
                    //                        Redis.getInstance().save(defaultEntity);
                    //                        node = replaceElementWithEntityReference(element, defaultEntity);
                    //                    }
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
                    checkPropertyUniqueness(element);
                    EntityTemplateClass parsingTemplate = (EntityTemplateClass) this.parsingTemplate;
                    /*
                     * Use all info from the template we're parsing to make a real entity-template-class to be cached.
                     * The correct template of this class to be cached has just been created in this defaults-visitor and can thus be found at element.outerHtml().
                     */
                    EntityTemplateClass entityTemplateClass = new EntityTemplateClass(parsingTemplate.getName(), this.language, element.outerHtml(), parsingTemplate.getPageTemplateName(), parsingTemplate.getLinks(), parsingTemplate.getScripts());
                    boolean added = EntityTemplateClassCache.getInstance().add(entityTemplateClass);
                    if(!added){
                        throw new ParseException("Could not add " + EntityTemplateClass.class.getSimpleName() + " '" + entityTemplateClass.getName() + "' to application cache. This shouldn't happen.");
                    }
                }
                //we reached the tail of the outer-most tag of a page-template, so we cache it to the application-cache
                else if(parsingTemplate instanceof PageTemplate) {
                    checkPropertyUniqueness(element);
                    PageTemplate pageTemplate = new PageTemplate(parsingTemplate.getName(), this.language, element.outerHtml(), parsingTemplate.getLinks(), parsingTemplate.getScripts());
                    boolean added = PageTemplateCache.getInstance().add(pageTemplate);
                    if(!added){
                        throw new ParseException("Could not add " + PageTemplate.class.getSimpleName() + " '" + pageTemplate.getName() + "' to application cache. This shouldn't happen.");
                    }
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


    /**
     * Make a new copy of the class-template, using all node-attributes of the node specified.
     * @param node
     * @param id
     * @param entityClass
     * @throws com.beligum.blocks.core.exceptions.IDException
     */
    private EntityTemplate saveNewEntityClassCopy(Node node, RedisID id, EntityTemplateClass entityClass) throws IDException, CacheException, ParseException
    {
        Map<RedisID, String> classTemplates = entityClass.getTemplates();
        Map<RedisID, String> copiedTemplates = new HashMap<>();
        for(RedisID languageId : classTemplates.keySet()){
            Element classRoot = TemplateParser.parse(classTemplates.get(languageId)).child(0);
            classRoot.attributes().addAll(node.attributes());
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
        return new EntityTemplate(id, entityClass, copiedTemplates);
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

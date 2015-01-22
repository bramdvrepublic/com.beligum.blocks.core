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
public class DefaultsVisitor extends AbstractVisitor
{
    //the language we're parsing
    private String language;
    //the type of template we're parsing
    private AbstractTemplate parsingTemplate;

    /**
     *
     * @param language
     * @param parsingTemplate The template this visitor will be visiting for the creation of default values. Only EntityTemplateClass and PageTemplate are supported.
     * @throws ParseException If an unknown or empty language is specified and if te type of the parsingTemplate is not supported.
     */
    public DefaultsVisitor(String language, AbstractTemplate parsingTemplate) throws ParseException
    {
        if(Languages.isNonEmptyLanguageCode(language)) {
            this.language = language;
        }
        else{
            throw new ParseException("Found unknown or empty language '" + language + "'.");
        }
        if(parsingTemplate instanceof EntityTemplateClass || parsingTemplate instanceof PageTemplate) {
            this.parsingTemplate = parsingTemplate;
        }
        else{
            throw new ParseException("Cannot visit a template of type " + parsingTemplate.getClass().getSimpleName() + ". Only " + EntityTemplateClass.class.getSimpleName() + " and " + PageTemplate.class + " are supported.");
        }
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
                    EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(getTypeOf(element));
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
                    AbstractTemplate replacedTemplate = EntityTemplateClassCache.getInstance().replace((EntityTemplateClass) parsingTemplate);
                    if(replacedTemplate != null){
                        Map<RedisID, String> templates = replacedTemplate.getTemplates();
                        boolean isBlueprint = false;
                        for(RedisID languageId : templates.keySet()){
                            isBlueprint = this.isBlueprint(TemplateParser.parse(templates.get(languageId)).child(0));
                            if(isBlueprint){
                                throw new ParseException("An "+ EntityTemplateClass.class.getSimpleName() + " of type '" + replacedTemplate.getName() + "' was already present in cache. Cannot have two blueprints for the same type. Found at \n \n " + node);
                            }
                        }
                    }
                }
                //we reached the tail of the outer-most tag of a page-template, so we cache it to the application-cache
                else if(parsingTemplate instanceof PageTemplate) {
                    boolean added = PageTemplateCache.getInstance().add((PageTemplate) parsingTemplate);
                    //default page-templates should be added to the cache no matter what, so the last one encountered is kept
                    if (!added && parsingTemplate.getName().contentEquals(ParserConstants.DEFAULT_PAGE_TEMPLATE)) {
                        PageTemplateCache.getInstance().replace((PageTemplate) parsingTemplate);
                        Logger.warn("Replaced default-" + PageTemplate.class.getSimpleName() + ". This should only happen once!");
                    }
                    else if(!added){
                        throw new ParsingException(PageTemplate.class.getSimpleName() + " '" + parsingTemplate.getName() + "' was not added to the application-cache, since another " + PageTemplate.class.getSimpleName() +
                                    " with the same name was already present. Cannot add two ");
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


}

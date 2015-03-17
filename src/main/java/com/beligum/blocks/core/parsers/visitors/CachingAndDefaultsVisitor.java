package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.caching.BlueprintsCache;
import com.beligum.blocks.core.caching.PageTemplatesCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.dynamic.DynamicBlockHandler;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.Blueprint;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.blocks.core.parsers.Traversor;
import com.beligum.core.framework.utils.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.*;

/**
 * Created by bas on 22/01/15.
 */
public class CachingAndDefaultsVisitor extends SuperVisitor
{
    //the language we're parsing
    private String language;

    private final Document root;
    //the type of template we're parsing
    private AbstractTemplate parsingTemplate;

    private Map<String, Blueprint> allEntityClasses;
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
    public CachingAndDefaultsVisitor(String language, Document root, AbstractTemplate parsingTemplate, Map<String, Blueprint> allEntityClasses, Map<String, PageTemplate> allPageTemplates) throws ParseException
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
        if(parsingTemplate instanceof Blueprint) {
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
            throw new ParseException("Cannot visit a template of type " + parsingTemplate.getClass().getSimpleName() + ". Only " + Blueprint.class.getSimpleName() + " and " + PageTemplate.class + " are supported.");
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
                        String blueprintType = getBlueprintType(element);
                        Blueprint blueprint = allEntityClasses.get(blueprintType);
                        if (blueprint == null) {
                            if(!DynamicBlockHandler.getInstance().isDynamicBlock(blueprintType)) {
                                throw new ParseException("Found unknown entity-class '" + blueprintType + "' at node \n \n " + element + "\n \n");
                            }
                            else{
                                //do nothing with dynamic block type, since it will be handled on show
                            }
                        }
                        else {
                            //we found an uncached blueprint, so we cache it
                            if (!BlueprintsCache.getInstance().contains(blueprintType) && !parsingTemplate.getName().equals(blueprintType)) {
                                Document entityTemplateClassDOM = TemplateParser.parse(blueprint.getTemplate());
                                Traversor
                                                traversor =
                                                new Traversor(new CachingAndDefaultsVisitor(blueprint.getLanguage(), entityTemplateClassDOM, blueprint, allEntityClasses,
                                                                                            allPageTemplates));
                                traversor.traverse(entityTemplateClassDOM);
                            }
                            //we want to use the cached blueprint
                            blueprint = BlueprintsCache.getInstance().get(blueprintType);
                            //for blueprints, new default-properties should be constructed
                            if (this.parsingTemplate instanceof Blueprint) {
                                if (this.parsingTemplate.getName().equals(this.getParentType())) {
                                    String language = getLanguage(element, blueprint);
                                    if (needsBlueprintCopy(element)) {
                                        BlocksID propertyId = BlocksID.renderClassPropertyId(this.getParentType(), getProperty(element), getPropertyName(element), language);
                                        node = this.saveNewEntityClassCopy(element, propertyId, blueprint);
                                    }
                                    else {
                                        BlocksID propertyId = BlocksID.renderClassPropertyId(this.getParentType(), getProperty(element), getPropertyName(element), language);
                                        node = this.saveNewEntity(element, propertyId);
                                    }
                                }
                                else {
                                    //TODO: make sure default grandchildren of a blueprint are not instantiated over again on every server startup (use properties field)
                                    //do nothing, since we have found an entity that is not a direct child of the template being parsed, this sort of entity will be taken care of while saving a new entity-default
                                }
                            }
                            //if we're parsing entities belonging to a page-template, we want to create a reproducable id, so we can permanently save changes in db
                            else if (this.parsingTemplate instanceof PageTemplate) {
                                //only entities at entity-depth 1 should be given a page-template-default id and saved to db
                                if (getParentType() == null) {
                                    BlocksID defaultPageTemplateEntityId = BlocksID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), this.language);
                                    EntityTemplate lastVersion = (EntityTemplate) RedisDatabase.getInstance().fetchLastVersion(defaultPageTemplateEntityId, EntityTemplate.class);
                                    //if no version of this entity exists yet, make a new one
                                    if (lastVersion == null) {
                                        if (needsBlueprintCopy(element)) {
                                            defaultPageTemplateEntityId = BlocksID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), this.language);
                                            node = this.saveNewEntityClassCopy(element, defaultPageTemplateEntityId, blueprint);
                                        }
                                        else {
                                            defaultPageTemplateEntityId =
                                                            BlocksID.renderNewPageTemplateDefaultEntity(this.parsingTemplate.getName(), getProperty(element), blueprint.getLanguage());
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
                    }
                    //if we find an entity which is not a property inside a parent, we throw an exception.
                    else if (this.blueprintTypeStack.size() > 0) {
                        throw new ParseException("Found entity-child with " + ParserConstants.BLUEPRINT + "-attribute, but no property-attribute at \n \n " + element + "\n \n");
                    }
                    //we reached the tail of the outer-most tag of an blueprint, so we cache it to the application-cache
                    else if (parsingTemplate instanceof Blueprint) {
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
        catch(ParseException e){
            throw e;
        }
        catch (Exception e) {
            throw new ParseException("Could not parse an " + Blueprint.class.getSimpleName() + " from " + Node.class.getSimpleName() + ": \n \n" + node + "\n \n", e);
        }
    }

    private void cacheEntityTemplateClass(Element root) throws Exception
    {
//        checkPropertyUniqueness(root);
        Blueprint parsingTemplate = (Blueprint) this.parsingTemplate;
        /*
         * Use all info from the template we're parsing to make a real blueprint to be cached.
         * The correct template of this class to be cached has just been created in this defaults-visitor and can thus be found at root.outerHtml().
         * Add addability information (as a block or as a page) to the class.*/
        boolean isAddableBlock = isAddableBlock(root);
        boolean isPageBlock = isPageBlock(root);
        root.removeAttr(ParserConstants.NOT_ADDABLE_BLOCK);
        root.removeAttr(ParserConstants.PAGE_BLOCK);
        Blueprint blueprint = new Blueprint(parsingTemplate.getName(), this.language, root.outerHtml(), parsingTemplate.getPageTemplateName(), parsingTemplate.getLinks(), parsingTemplate.getScripts());
        blueprint.setAddableBlock(isAddableBlock);
        blueprint.setPageBlock(isPageBlock);
        blueprint.setProperties(parsingTemplate.getProperties());

        boolean added = BlueprintsCache.getInstance().add(blueprint);
        if(!added) {
            if (blueprint.getName().equals(ParserConstants.DEFAULT_BLUEPRINT)) {
                if(!BlueprintsCache.getInstance().get(blueprint.getName()).equals(blueprint)) {
                    BlueprintsCache.getInstance().replace(blueprint);
                    Logger.warn("Replaced default-" + Blueprint.class.getSimpleName() + ".");
                }
            }
            else {
                throw new ParseException("Could not add " + Blueprint.class.getSimpleName() + " '" + blueprint.getName() + "' to application cache. This shouldn't happen.");
            }
        }
    }

    private void cachePageTemplate(Element root) throws Exception
    {
//        checkPropertyUniqueness(root);
        PageTemplate pageTemplate = new PageTemplate(parsingTemplate.getName(), this.language, root.outerHtml(), parsingTemplate.getLinks(), parsingTemplate.getScripts());
        pageTemplate.setProperties(parsingTemplate.getProperties());

        boolean added = PageTemplatesCache.getInstance().add(pageTemplate);
        if(!added){
            if(pageTemplate.getName().equals(ParserConstants.DEFAULT_PAGE_TEMPLATE)){
                if(!PageTemplatesCache.getInstance().get(pageTemplate.getName()).equals(pageTemplate)){
                    PageTemplatesCache.getInstance().replace(pageTemplate);
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
    private Element saveNewEntityClassCopy(Element element, BlocksID id, Blueprint entityClass) throws IDException, CacheException, ParseException
    {
        //        Map<BlocksID, String> classTemplates = entityClass.getTemplates();
        Map<BlocksID, String> copiedTemplates = new HashMap<>();
        //        for(BlocksID languageId : classTemplates.keySet()){
        //            Element classRoot = TemplateParser.parse(classTemplates.get(languageId)).child(0);
        //            classRoot.attributes().addAll(element.attributes());
        //            classRoot = (Element) setUseBlueprintType(classRoot);
        //            //a copy of an entity-class, also means a copy of all of it's children, so we need to traverse all templates to create entity-copies of all it's children
        //            BlueprintToStoredInstanceVisitor visitor = new BlueprintToStoredInstanceVisitor(id.getUrl(), id.getLanguage());
        //            Traversor traversor = new Traversor(visitor);
        //            traversor.traverse(classRoot);
        //            copiedTemplates.put(languageId, classRoot.outerHtml());
        //        }
        String template = entityClass.getTemplate(id.getLanguage());
        if(template==null) {
            template = entityClass.getTemplate();
        }
        //a copy of an entity-class, also means a copy of all of it's children, so we need to traverse all templates to create entity-copies of all it's children
        Element classRoot = TemplateParser.parse(entityClass.getTemplate()).child(0);
        classRoot.attributes().addAll(element.attributes());
        BlueprintToStoredInstanceVisitor visitor = new BlueprintToStoredInstanceVisitor(id.getUrl(), id.getLanguage());
        Traversor traversor = new Traversor(visitor);
        traversor.traverse(classRoot);
//        copiedTemplates.put(id, entityClass.getTemplate());
        EntityTemplate newEntity = visitor.getFoundEntityRoot();
        this.parsingTemplate.setProperty(getPropertyKey(element), newEntity);
        element = replaceElementWithEntityReference(element, newEntity);
        return element;
    }

    /**
     * Save the specified node as a new (default) instance to db. The typeof-attribute of the node will determine it's entity-class
     * @param element
     * @param id
     * @return a referencing node to the freshly stored entity
     * @throws IDException
     * @throws CacheException
     * @throws com.beligum.blocks.core.exceptions.DatabaseException
     * @throws ParseException
     */
    private Node saveNewEntity(Element element, BlocksID id) throws IDException, CacheException, DatabaseException, ParseException
    {
        /*
         * HtmlToStoredInstance needs a html-document to traverse correctly.
         * We put the root of the entity to be a default into the body and let the visitor save new instances where needed.
         */
        Document entityRoot = new Document(BlocksConfig.getSiteDomain());
        entityRoot.appendChild(element.clone());
        //traverse the entity-root and save new instances to db
        HtmlToStoreVisitor visitor = new HtmlToStoreVisitor(id.getUrl(), id.getLanguage(), entityRoot);
        Traversor traversor = new Traversor(visitor);
        traversor.traverse(entityRoot);
        EntityTemplate newEntity = visitor.getFoundEntityRoot();
        this.parsingTemplate.setProperty(getPropertyKey(element), newEntity);
        return this.replaceElementWithEntityReference(element, newEntity);
    }

//    /**
//     * Checks if the properties af a template are unique (or have a unique name if multiple equal properties are present).
//     * @param templateRoot root-node of a template
//     * @return true if all properties are unique, throws {@link ParseException} otherwise.
//     * @throws ParseException
//     */
//    private boolean checkPropertyUniqueness(Element templateRoot) throws ParseException
//    {
//        Elements properties = templateRoot.select("[" + ParserConstants.PROPERTY + "]");
//        //the class-root is not a property of this class, so if it contains the "property"-attribute, it is removed from the list
//        properties.remove(templateRoot);
//        //since we are sure to be working with class-properties, we now all of them will hold an attribute "property", so we can use this in a comparator to sort all elements according to the property-value
//        Collections.sort(properties, new Comparator<Element>() {
//            @Override
//            public int compare(Element classProperty1, Element classProperty2) {
//                return getProperty(classProperty1).compareTo(getProperty(classProperty2));
//            }
//        });
//        for(int i = 1; i<properties.size(); i++){
//            Element previousClassProperty = properties.get(i-1);
//            String previousClassPropertyValue = getProperty(previousClassProperty);
//            Element classProperty = properties.get(i);
//            String classPropertyValue = getProperty(classProperty);
//            if(previousClassPropertyValue.equals(classPropertyValue)){
//                //check if properties with the same attribute-value, have a different name (<div property="something" name="some_thing"></div> and <div property="something"  name="so_me_th_ing"></div> is a correct situation)
//                String previousClassPropertyName = getPropertyName(previousClassProperty);
//                String classPropertyName = getPropertyName(classProperty);
//                if(StringUtils.isEmpty(previousClassPropertyName) || StringUtils.isEmpty(classPropertyName)){
//                    throw new ParseException("Found two properties with same property-value '" + previousClassPropertyValue + "' and no name-attribute to distinguish them at \n \n" + templateRoot + "\n \n");
//                }
//            }
//        }
//        return true;
//    }


}

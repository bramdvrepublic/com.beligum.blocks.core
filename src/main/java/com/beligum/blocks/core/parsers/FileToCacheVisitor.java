package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by wouter on 22/11/14.
 * Visitor holding all functionalities to parse a html-file to entity-classes stored in cache
 */
public class FileToCacheVisitor extends AbstractVisitor
{

    private String pageTemplateName = null;
    /**flag for indicating if the current traverse has encountered a tag indicating a page-template is being parsed*/
    private boolean parsingPageTemplate = false;

    @Override
    public Node head(Node node, int depth) throws ParseException {
        node = super.head(node, depth);
        if(isPageTemplateRootNode(node)) {
            this.pageTemplateName = getPageTemplateName(node);
            this.parsingPageTemplate = true;
        }
        else if(parsingPageTemplate && isPageTemplateContentNode(node) && node instanceof Element){
            this.createTemplate((Element) node);
        }
        return node;
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        node = super.tail(node, depth);
        if (node instanceof Element && isEntity(node)) {
            try {
                Element element = (Element) node;
                EntityTemplateClass entityTemplateClass;
                //if this element is a class-bleuprint, it must be added to the cache (even if a class with this name was cached before)
                if(isBlueprint(element)) {
                     entityTemplateClass = cacheEntityTemplateClassFromNode(element);
                }
                else{
                    String typeOf = getTypeOf(element);
                    //if no class of this type can be found, we use the found html as blueprint
                    if(!typeOf.equals(ParserConstants.DEFAULT_ENTITY_TEMPLATE_CLASS) && !EntityTemplateClassCache.getInstance().contains(typeOf)){
                        entityTemplateClass = cacheEntityTemplateClassFromNode(element);
                    }
                    else{
                        entityTemplateClass = EntityTemplateClassCache.getInstance().get(typeOf);
                    }
                }
                if(isProperty(element)) {
                    element.removeAttr(ParserConstants.BLUEPRINT);
                    EntityTemplate propertyInstance = new EntityTemplate(RedisID.renderNewPropertyId(this.getParentType(), getProperty(element)), entityTemplateClass, element.outerHtml());
                    RedisID lastVersion = new RedisID(propertyInstance.getUnversionedId(), RedisID.LAST_VERSION);
                    EntityTemplate storedInstance = Redis.getInstance().fetchEntityTemplate(lastVersion);
                    //if no version is present in db, or this version is different, save to db
                    if(storedInstance == null || !storedInstance.equals(propertyInstance)) {
                        Redis.getInstance().save(propertyInstance);
                    }
                    node = replaceElementWithPropertyReference(element);
                }
                else if(this.typeOfStack.size()>0){
                    Element entityTemplateClassRoot = TemplateParser.parse(entityTemplateClass.getTemplate()).child(0);
                    entityTemplateClassRoot.removeAttr(ParserConstants.BLUEPRINT);
                    EntityTemplate instance = new EntityTemplate(RedisID.renderNewEntityTemplateID(entityTemplateClass),entityTemplateClass, entityTemplateClassRoot.outerHtml());
                    RedisID lastVersion = new RedisID(instance.getUnversionedId(), RedisID.LAST_VERSION);
                    EntityTemplate storedInstance = Redis.getInstance().fetchEntityTemplate(lastVersion);
                    //if no version is present in db, or this version is different, save to db
                    if(storedInstance == null || !storedInstance.equals(instance)) {
                        Redis.getInstance().save(instance);
                    }
                    node = replaceElementWithEntityReference(element, instance);
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
                EntityTemplateClass entityTemplateClass = new EntityTemplateClass(entityClassName, classRoot.outerHtml(), this.pageTemplateName);
                Elements classProperties = classRoot.select("[" + ParserConstants.PROPERTY + "]");
                Set<String> propertyNames = new HashSet<>();
                for(Element classProperty : classProperties){
                    String propertyName = getProperty(classProperty);
                    if(!StringUtils.isEmpty(propertyName)) {
                        if(!propertyNames.add(propertyName)){
                            throw new ParseException("Cannot add two properties with the same name '"+propertyName+"'to one class ('" + entityClassName + "')for now. This will be possible in a later version. Found at \n \n " + classRoot);
                        }
                    }
                }
                EntityTemplateClassCache.getInstance().replace(entityTemplateClass);
                return EntityTemplateClassCache.getInstance().get(entityClassName);
            }
            else{
                throw new Exception(Node.class.getSimpleName() + " '" + classRoot + "' does not define an entity.");
            }
        }
        catch(Exception e){
            throw new ParseException("Error while creating new entity-class '" + entityClassName +"'.", e);
        }
    }

    private Node createTemplate(Element element) throws ParseException
    {
        try {
            if (element.hasAttr(ParserConstants.PAGE_TEMPLATE_CONTENT_ATTR)) {
                Node parent = element.parent();
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
                Node replacement = this.replaceElementWithReference(element, ParserConstants.PAGE_TEMPLATE_ENTITY_VARIABLE_NAME);
                //we need to instanciate the cache first, so a default-template surely will be cached with an older version than the page-template we're about to make
                PageTemplateCache cache = PageTemplateCache.getInstance();
                PageTemplate pageTemplate = new PageTemplate(templateName, parent.outerHtml());
                replacement.replaceWith(element);
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
            return element;
        }
        catch (Exception e) {
            throw new ParseException("Something went wrong while creating page-template.", e);
        }
    }
}

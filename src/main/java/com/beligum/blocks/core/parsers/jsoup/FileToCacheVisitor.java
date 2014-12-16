package com.beligum.blocks.core.parsers.jsoup;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

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
                EntityTemplateClass entityTemplateClass = cacheEntityTemplateClassFromNode(element);
                if(isProperty(element)) {
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
                    EntityTemplate instance = new EntityTemplate(RedisID.renderNewEntityTemplateID(entityTemplateClass),entityTemplateClass);
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
     *
     * @param node node defining an entity-template-class
     * @return the entity-template-class defined by the node
     * @throws ParseException
     */
    private EntityTemplateClass cacheEntityTemplateClassFromNode(Node node) throws ParseException
    {
        String entityClassName = "";
        try {
            entityClassName = this.getTypeOf(node);
            if(!StringUtils.isEmpty(entityClassName)) {
                EntityTemplateClass entityTemplateClass = new EntityTemplateClass(entityClassName, node.outerHtml(), this.pageTemplateName);
                boolean added = EntityTemplateClassCache.getInstance().add(entityTemplateClass);
                //if the node is a bleuprint and the cache already had a template-class present, force replace the template
                if(!added && isBlueprint(node)){
                    EntityTemplateClassCache.getInstance().replace(entityTemplateClass);
                }
                return EntityTemplateClassCache.getInstance().get(entityClassName);
            }
            else{
                throw new Exception(Node.class.getSimpleName() + " '" + node + "' does not define an entity.");
            }
        }
        catch(Exception e){
            throw new ParseException("Error while creating new entity-class '" + entityClassName +"'.", e);
        }
    }

    private Node createTemplate(Element element) throws ParseException
    {
        try {
            //TODO BAS SH: trying to add page-templating to entity-templates and entity-template classes. For the moment it seems to use the use the default-page-template always.
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

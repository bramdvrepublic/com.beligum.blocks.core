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
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 * Created by wouter on 22/11/14.
 */
public class FileToCacheVisitor extends AbstractVisitor
{
    Node template;

    @Override
    public Node head(Node node, int depth) throws ParseException {
        node = super.head(node, depth);
        this.prepareTemplate(node);
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
                if(isProperty(node)) {
                    EntityTemplate propertyInstance = new EntityTemplate(RedisID.renderNewPropertyId(this.getParentType(), getProperty(element)), entityTemplateClass, element.outerHtml());
                    //TODO BAS: the replacementnode, should be cached as a DefaultPropertyTemplate or something of the sort? or is db-storage enough?
                    //TODO BAS: only when the instance doesn't exist yet, the save should be performed!!!
                    Redis.getInstance().save(propertyInstance);
                    node = replaceElementWithPropertyReference(element);
                }
                else if(this.typeOfStack.size()>0){
                    EntityTemplate instance = new EntityTemplate(RedisID.renderNewEntityTemplateID(entityTemplateClass),entityTemplateClass);
                    //TODO BAS: only when the instance doesn't exist yet, the save should be performed!!!
                    Redis.getInstance().save(instance);
                    node = replaceElementWithEntityReference(element, instance);
                }
                else{
                    //do nothing, since we have found the ending of the outer-most typeof-tag
                }
            }
            catch (Exception e) {
                throw new ParseException("Could not parse an " + EntityTemplateClass.class.getSimpleName() + " from " + Node.class.getSimpleName() + " \n \n:" + node, e);
            }
        }
        else if (node instanceof Element) {
            this.createTemplate((Element) node);
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
            //TODO BAS SH: Je bent bezig met het vervolledigen van de parsers. Je hebt net de visitors in drie verschillende opgesplits: eentje voor het cachen van de classes, eentje voor het maken van nieuwe instances en eentje om van templates naar html-pagina's terug te keren. Ze moeten alledrie goed gedebugged worden, want er is aardig wat veranderd.
            //TODO BAS SH: Default instances van een bepaalde klasse (bijvoorbeeld de locatie-instance die in de waterput-klasse zit) moeten in de database opgeslagen worden onder "blocks://MOT/waterput#adresLocatie_eenpropertyid:version"
            entityClassName = this.getTypeOf(node);
            if(entityClassName != null) {
                EntityTemplateClass entityTemplateClass = new EntityTemplateClass(entityClassName, node.outerHtml(), null);
                boolean added = EntityTemplateClassCache.getInstance().add(entityTemplateClass);
                //if the node is a bleuprint and the cache already had a template-class present, force replace the template
                if(!added && node.hasAttr(ParserConstants.BLEUPRINT)){
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

    protected void prepareTemplate(Node node) {
        if (isPageTemplate(node)) {
            this.template = node;
        }
    }

    protected void createTemplate(Element element)
    {
        if (element.hasAttr(ParserConstants.PAGE_TEMPLATE_CONTENT_ATTR)) {
            try {
                Node parent = element.parent();
                //initialize the page-template name by searching for the first template-attribute we find before the specified node and take the value of that attribute to be the name
                String templateName = "";
                while (parent.parent() != null) {
                    if (parent.nodeName().equals("html")) {
                        templateName = parent.attr(ParserConstants.PAGE_TEMPLATE_ATTR);
                    }
                    parent = parent.parent();
                }
                this.replaceElementWithReference(element, ParserConstants.PAGE_TEMPLATE_ENTITY_VARIABLE_NAME);
                PageTemplate pageTemplate = new PageTemplate(templateName, parent.outerHtml());
                boolean added = PageTemplateCache.getInstance().add(pageTemplate);
                if(!added){
                    Logger.warn(PageTemplate.class.getName() + " '" + pageTemplate.getName() + "' was not added to the application-cache, since an other " + PageTemplate.class.getName() + " with the same id was already present.");
                }
            } catch (Exception e) {
                Logger.error("Something went wrong while creating template.", e);
                // TODO show error somewhere?
            }
        }
    }
}

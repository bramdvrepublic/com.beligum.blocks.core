package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplateClass;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URL;
import java.util.Stack;

/**
 * Created by bas on 03.12.14.
 * Visitor holding all functionalities to go from a cached class-template to a new instance
 */
public class ClassToStoredInstanceVisitor extends SuperVisitor
{
    //TODO BAS!: inject css-classes of class into instance, afterwards add classes of instance (which could overwrite the class-css)

    //the parent-nodes of the entity-template instances to be created
    private Stack<Node> newInstancesNodes = new Stack<>();

    private boolean parsingNewLanguage = false;

    private final String language;

    /**
     *
     * @param pageUrl
     * @param language the language the new instance will have, if this is not a correct language, the default language will be used
     * @throws IDException if the specified page-url cannot be used as an id
     */
    public ClassToStoredInstanceVisitor(URL pageUrl, String language) throws ParseException
    {
        try{
            RedisID pageId = new RedisID(pageUrl, RedisID.LAST_VERSION, true);
            EntityTemplate page = Redis.getInstance().fetchEntityTemplate(pageId);
            if(page != null && !page.getLanguage().equals(language)){
                parsingNewLanguage = true;
            }
            this.pageUrl = pageId.getUrl();
            if(Languages.isNonEmptyLanguageCode(language)) {
                this.language = language;
            }
            else{
                this.language = pageId.getLanguage();
            }
        }catch(Exception e){
            throw new ParseException("Could not initialize " + ClassToStoredInstanceVisitor.class.getSimpleName() + ".", e);
        }
    }

    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        node = super.head(node, depth);
        // node is TypeOf or Property
        if(node instanceof Element && isEntity(node)){
            try {
                String unversionedResourceId = getReferencedId(node);
                String defaultPropertyId = getPropertyId(node);
                String typeOf = getTypeOf(node);
                EntityTemplate instance = null;
                // this element has a reference to an instance, it is a property of an entity(-class) and the referenced id is the one of a default-property
                // => this is a reference to the default value of the property
                if (!StringUtils.isEmpty(unversionedResourceId) && !StringUtils.isEmpty(defaultPropertyId) && unversionedResourceId.equals(defaultPropertyId)){
                    EntityTemplate defaultPropertyTemplate = this.fetchDefaultEntityTemplate(unversionedResourceId);
                    instance = defaultPropertyTemplate;
                }
                // this is not a default property but still an entity and has an id
                else if(!StringUtils.isEmpty(unversionedResourceId) && !StringUtils.isEmpty(typeOf)){
                    // Fetch the default value in the db for this resource
                    EntityTemplate defaultEntityTemplate = this.fetchDefaultEntityTemplate(unversionedResourceId);
                    // Make a new entity-template-instance, which is a copy of the default-tempalte
                    EntityTemplateClass entityClass = EntityTemplateClassCache.getInstance().get(typeOf);
                    // If the current language is not present in the default template, copy the template in the primary-language to the new language
                    if(defaultEntityTemplate.getTemplate(this.language) == null){
                        RedisID newLanguageId = RedisID.renderLanguagedId(defaultEntityTemplate.getId().getUrl(), RedisID.NEW_VERSION, this.language);
                        defaultEntityTemplate.add(newLanguageId, defaultEntityTemplate.getTemplate());
                    };
                    EntityTemplate newEntityInstance = new EntityTemplate(RedisID.renderNewEntityTemplateID(entityClass, this.language), entityClass, defaultEntityTemplate.getTemplates());
                    instance = newEntityInstance;
                }
                node = replaceNodeWithEntity(node, instance);
                newInstancesNodes.push(node);
            }
            catch (Exception e) {
                throw new ParseException("Could not parse an " + EntityTemplate.class.getSimpleName() + " from " + Node.class.getSimpleName() + " ÷" +
                        "\n \n " + node + "\n \n ", e);
            }
        }
        return node;
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        try {
            node = super.tail(node, depth);
            Node lastInstanceNode = !newInstancesNodes.isEmpty() ? newInstancesNodes.peek() : null;
            if (node.equals(lastInstanceNode) && node instanceof Element) {
                EntityTemplateClass entityClass = EntityTemplateClassCache.getInstance().get(getTypeOf(node));
                RedisID newEntityId;
                // For the first root entity use pageUrl if available
                if (newInstancesNodes.size() == 1 && pageUrl != null) {
                    newEntityId = RedisID.renderLanguagedId(pageUrl, RedisID.NEW_VERSION, this.language);
                }
                //else render a new entity-template-id
                else{
                    newEntityId = RedisID.renderNewEntityTemplateID(entityClass, this.language);
                }
                node.removeAttr(ParserConstants.BLUEPRINT);
                node.attr(ParserConstants.RESOURCE, newEntityId.getUrl().toString());
                EntityTemplate newInstance = new EntityTemplate(newEntityId, entityClass, node.outerHtml());
                Redis.getInstance().save(newInstance);
                node = replaceElementWithEntityReference((Element) node, newInstance);
                newInstancesNodes.pop();
            }
            return node;
        }
        catch (ParseException e){
            throw e;
        }
        catch (Exception e) {
            throw new ParseException("Could not parse an " + EntityTemplate.class.getSimpleName() + "-instance from " + Node.class.getSimpleName() + " " + node, e);
        }
    }

    /**
     * Determine and fetch the default entity-template. First try to fetch the language we're parsing, if not found, fetch the primary language of the default template.
     * @param unversionedResourceId
     * @throws IDException
     * @throws RedisException
     * @throws ParseException
     */
    private EntityTemplate fetchDefaultEntityTemplate(String unversionedResourceId) throws IDException, RedisException, ParseException {
        RedisID defaultEntityId = new RedisID(unversionedResourceId, RedisID.LAST_VERSION, this.language);
        EntityTemplate defaultEntityTemplate = Redis.getInstance().fetchEntityTemplate(defaultEntityId);
        // If no such default template could be found, we're probably dealing with another language, which needs to be a copy of the primary-language
        if(defaultEntityTemplate == null){
            defaultEntityTemplate = (EntityTemplate) Redis.getInstance().fetchLastVersion(defaultEntityId, EntityTemplate.class);
            if(defaultEntityTemplate == null) {
                throw new ParseException("Found bad reference. Not present in db: " + defaultEntityId);
            }
        }
        return defaultEntityTemplate;
    }

    /**
     *
     * @param node
     * @return the entity-template-class correspoding to the node's typeof-attribute's value
     * @throws ParseException
     */
    private EntityTemplateClass getEntityTemplateClassForNode(Node node) throws ParseException
    {
        String entityClassName = "";
        try {
            entityClassName = this.getTypeOf(node);
            if(entityClassName != null) {
                return EntityTemplateClassCache.getInstance().get(entityClassName);
            }
            else{
                throw new Exception(Node.class.getSimpleName() + " '" + node + "' does not define an entity.");
            }
        }
        catch (CacheException e){
            throw new ParseException("Couldn't get entity-class '" + entityClassName +"' from cache, while parsing: \n \n " + node.outerHtml(), e);
        }
        catch (Exception e){
            throw new ParseException("Error while getting entity-template-class for: \n \n" + node.outerHtml(), e);
        }
    }


}

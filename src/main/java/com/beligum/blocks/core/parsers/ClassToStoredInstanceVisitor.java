package com.beligum.blocks.core.parsers;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URL;
import java.util.Stack;

/**
 * Created by bas on 03.12.14.
 * Visitor holding all functionalities to go from a cached class-template to a new instance
 */
public class ClassToStoredInstanceVisitor extends AbstractVisitor
{
    //the parent-nodes of the entity-template instances to be created
    private Stack<Node> newInstancesNodes = new Stack<>();

    private String language;

    /**
     *
     * @param pageUrl
     * @param language the language the new instance will have, if this is not a correct language, the default language will be used
     * @throws IDException if the specified page-url cannot be used as an id
     */
    public ClassToStoredInstanceVisitor(URL pageUrl, String language) throws ParseException
    {
        try{
            RedisID temp = new RedisID(pageUrl, RedisID.LAST_VERSION, true);
            this.pageUrl = temp.getUrl();
            if(Languages.isNonEmptyLanguageCode(language)) {
                this.language = language;
            }
            else{
                this.language = temp.getLanguage();
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
                // Reference-to
                String unversionedResourceId = getReferencedId(node);
                // Field in class
                String defaultPropertyId = getPropertyId(node, language);
                String typeOf = getTypeOf(node);
                // this element has a reference to an instance/class(in cache) and it is a property of an entity
                // and it's reference equals it's propertyname
                // => this is the default value of the property, not an instance
                if (!StringUtils.isEmpty(unversionedResourceId) && !StringUtils.isEmpty(defaultPropertyId) && unversionedResourceId.equals(defaultPropertyId)){
                    if(StringUtils.isEmpty(language)){
                        language = RedisID.PRIMARY_LANGUAGE;
                    }
                    RedisID lastPropertyVersion = new RedisID(unversionedResourceId, RedisID.LAST_VERSION, language);
                    EntityTemplate defaultPropertyTemplate = Redis.getInstance().fetchEntityTemplate(lastPropertyVersion);
                    if(defaultPropertyTemplate == null){
                        throw new ParseException("Found bad reference. Not present in db: " + unversionedResourceId);
                    }
                    node = replaceNodeWithEntity((Element) node, defaultPropertyTemplate);
                }
                // this is not a default property but still an entity and has an id
                else if(!StringUtils.isEmpty(unversionedResourceId) && !StringUtils.isEmpty(typeOf)){
                    if(StringUtils.isEmpty(language)){
                        language = RedisID.PRIMARY_LANGUAGE;
                    }
                    RedisID defaultEntityId = new RedisID(unversionedResourceId, RedisID.LAST_VERSION, language);
                    // Fetch the default value in the db for this resource
                    EntityTemplate defaultEntityTemplate = Redis.getInstance().fetchEntityTemplate(defaultEntityId);
                    if(defaultEntityTemplate == null){
                        throw new ParseException("Found bad reference. Not present in db: " + defaultEntityId);
                    }
                    // Fetch the class for this resource
                    EntityTemplateClass entityClass = EntityTemplateClassCache.getInstance().get(typeOf);
                    // get new instance
                    EntityTemplate newEntityInstance = new EntityTemplate(RedisID.renderNewEntityTemplateID(entityClass), entityClass, defaultEntityTemplate.getTemplates());

                    node = replaceNodeWithEntity((Element) node, newEntityInstance);
                }
                newInstancesNodes.push(node);
            }
            catch (ParseException e){
                throw e;
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
                    newEntityId = RedisID.renderNewEntityTemplateID(entityClass);
                }
                node.removeAttr(ParserConstants.BLUEPRINT);
                node.attr(ParserConstants.RESOURCE, newEntityId.getUrl().toString());
                String language = getLanguage(node, entityClass);
                EntityTemplate newInstance = new EntityTemplate(newEntityId, entityClass, node.outerHtml());
                Redis.getInstance().save(newInstance);
                node = replaceElementWithEntityReference((Element) node, newInstance);
                newInstancesNodes.pop();
            }
            return super.tail(node, depth);
        }
        catch (ParseException e){
            throw e;
        }
        catch (Exception e) {
            throw new ParseException("Could not parse an " + EntityTemplate.class.getSimpleName() + "-instance from " + Node.class.getSimpleName() + " " + node, e);
        }
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

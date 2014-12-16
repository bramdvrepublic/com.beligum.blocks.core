package com.beligum.blocks.core.parsers.jsoup;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
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

    public ClassToStoredInstanceVisitor(URL pageUrl) {
        this.pageUrl = pageUrl;
    }

    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        // node is TypeOf or Property
        if(node instanceof Element && isEntity(node)){
            try {
                // Reference-to
                String unversionedResourceId = getReferencedId(node);
                // Field in class
                String defaultPropertyId = getPropertyId(node);
                String typeOf = getTypeOf(node);
                // this element has a reference to an instance/class(in cache) and it is a property of an entity
                // and it's reference equals it's propertyname
                // => this is the default value of the property, not an instance
                if (!StringUtils.isEmpty(unversionedResourceId) && !StringUtils.isEmpty(defaultPropertyId) && unversionedResourceId.equals(defaultPropertyId)){
                    RedisID lastPropertyVersion = new RedisID(unversionedResourceId, RedisID.LAST_VERSION);
                    //TODO: the default property-template should probably be fetched from cache, instead of db
                    EntityTemplate defaultPropertyTemplate = Redis.getInstance().fetchEntityTemplate(lastPropertyVersion);
                    node = replaceReferenceWithEntity(node, defaultPropertyTemplate);
                }
                // this is not a property but an entity and has an id
                else if(!StringUtils.isEmpty(unversionedResourceId) && !StringUtils.isEmpty(typeOf)){
                    RedisID defaultEntityId = new RedisID(unversionedResourceId, RedisID.LAST_VERSION);
                    // Fetch the default value in the db for this resource
                    EntityTemplate defaultEntityTemplate = Redis.getInstance().fetchEntityTemplate(defaultEntityId);
                    // Fetch the class for this resource
                    EntityTemplateClass entityClass = EntityTemplateClassCache.getInstance().get(typeOf);
                    // get new instance
                    // First entity gets the pageUrl. Set PageUrl to null when used
                    EntityTemplate newEntityInstance = new EntityTemplate(RedisID.renderNewEntityTemplateID(entityClass), entityClass, defaultEntityTemplate.getTemplate());

                    node = replaceReferenceWithEntity(node, newEntityInstance);
                }
                newInstancesNodes.push(node);
            }
            catch (ParseException e){
                throw e;
            }
            catch (Exception e) {
                throw new ParseException("Could not parse an " + EntityTemplate.class.getSimpleName() + " from " + Node.class.getSimpleName() + " " + node, e);
            }
        }
        node = super.head(node, depth);
        return node;
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        try {
            Node lastInstanceNode = !newInstancesNodes.isEmpty() ? newInstancesNodes.peek() : null;
            if (node.equals(lastInstanceNode) && node instanceof Element) {
                EntityTemplateClass entityClass = EntityTemplateClassCache.getInstance().get(getTypeOf(node));
                RedisID newEntityId = RedisID.renderNewEntityTemplateID(entityClass);
                // For the first root entity use pageUrl if available
                if (newInstancesNodes.size() == 1 && pageUrl != null) {
                    newEntityId = new RedisID(pageUrl);
                }
                node.attr(ParserConstants.RESOURCE, newEntityId.getUrl().toString());
                EntityTemplate newInstance = new EntityTemplate(newEntityId, entityClass, node.outerHtml());
                Redis.getInstance().save(newInstance);
                node = replaceElementWithEntityReference((Element) node, newInstance);
                newInstancesNodes.pop();
            }
            node = super.tail(node, depth);
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

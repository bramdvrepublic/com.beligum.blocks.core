package com.beligum.blocks.core.parsers.jsoup;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.Stack;

/**
 * Created by bas on 03.12.14.
 */
public class ClassToStoredInstanceVisitor extends AbstractVisitor
{
    //the parent-nodes of the entity-template instances to be created
    private Stack<Node> newInstancesNodes = new Stack<>();

    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        if(node instanceof Element && isEntity(node)){
            try {
                String unversionedResourceId = getReferencedId(node);
                String defaultPropertyId = getPropertyId(node);
                String typeOf = getTypeOf(node);
                if (!StringUtils.isEmpty(unversionedResourceId) && !StringUtils.isEmpty(defaultPropertyId) && unversionedResourceId.contentEquals(defaultPropertyId)){
                    RedisID lastPropertyVersion = new RedisID(unversionedResourceId, RedisID.LAST_VERSION);
                    //TODO: the default property-template should probably be fetched from cache, instead of db
                    EntityTemplate defaultPropertyTemplate = (EntityTemplate) Redis.getInstance().fetchTemplate(lastPropertyVersion, EntityTemplate.class);
                    node = replaceReferenceWithEntity(node, defaultPropertyTemplate);
                }
                else if(!StringUtils.isEmpty(unversionedResourceId) && !StringUtils.isEmpty(typeOf)){
                    RedisID defaultEntityId = new RedisID(unversionedResourceId, RedisID.LAST_VERSION);
                    EntityTemplate defaultEntityTemplate = (EntityTemplate) Redis.getInstance().fetchTemplate(defaultEntityId, EntityTemplate.class);
                    EntityTemplateClass entityClass = EntityTemplateClassCache.getInstance().get(typeOf);
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
                EntityTemplate newInstance = new EntityTemplate(RedisID.renderNewEntityTemplateID(entityClass), entityClass, node.outerHtml());
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

package com.beligum.blocks.core.parsers.jsoup;

import antlr.debug.ParserEventSupport;
import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 * Created by bas on 03.12.14.
 */
public class ClassToStoredInstanceVisitor extends AbstractVisitor
{
    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        if(node instanceof Element && isEntity(node)){
            try {
                String unversionedResourceId = getReferencedId(node);
                if (!StringUtils.isEmpty(unversionedResourceId)) {
                    String propertyId = getPropertyId(node);
                    //TODO BAS SH2: als we werken met een typeof-property die geen property-attribute heeft, zal getPropertyId(node) null teruggeven en dan zal er nooit in de if-block gegaan worden, terwijl we dat wel willen
                    if(unversionedResourceId.contentEquals(propertyId)){
                        EntityTemplateClass nodeClass = EntityTemplateClassCache.getInstance().get(getTypeOf(node));
                        EntityTemplate newInstance = new EntityTemplate(nodeClass);
                        Redis.getInstance().save(newInstance);
                        node = replaceElementWithEntityReference((Element) node, newInstance);
                    }
                    else{
                        throw new ParseException("Class '" + this.getParentType() + "' doesn't hold correct referencing: " + unversionedResourceId);
                    }
                }
            }
            catch (ParseException e){
                throw e;
            }
            catch (Exception e) {
                throw new ParseException("Could not parse an " + EntityTemplate.class.getSimpleName() + " from " + Node.class.getSimpleName() + " " + node, e);
            }
        }
        super.head(node, depth);
        return node;
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

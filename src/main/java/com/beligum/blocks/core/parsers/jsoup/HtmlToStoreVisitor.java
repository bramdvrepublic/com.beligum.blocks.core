package com.beligum.blocks.core.parsers.jsoup;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URL;

/**
 * Created by bas on 10.12.14.
 * Visitor holding all functionalities to go from html to an updated stored instance of entities
 */
public class HtmlToStoreVisitor extends AbstractVisitor
{
    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        return super.head(node, depth);
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        try {
            String resourceUrl = getResource(node);
            if (!StringUtils.isEmpty(resourceUrl) && isEntity(node) && node instanceof Element) {
                RedisID resourceId = new RedisID(new URL(resourceUrl), RedisID.LAST_VERSION);
                EntityTemplate storedEntityTemplate = Redis.getInstance().fetchEntityTemplate(resourceId);
                RedisID newVersionId = new RedisID(new URL(resourceUrl));
                EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(getTypeOf(node));
                EntityTemplate currentEntityTemplate = new EntityTemplate(newVersionId, entityTemplateClass, node.outerHtml());
                if(currentEntityTemplate.equals(storedEntityTemplate)){
                    currentEntityTemplate = storedEntityTemplate;
                }
                else{
                    Redis.getInstance().save(currentEntityTemplate);
                }
                node = replaceElementWithEntityReference((Element) node, currentEntityTemplate);
            }

            return super.tail(node, depth);
        }catch(Exception e){
            throw new ParseException("Could not parse resource-node: \n \n " + node, e);
        }
    }
}

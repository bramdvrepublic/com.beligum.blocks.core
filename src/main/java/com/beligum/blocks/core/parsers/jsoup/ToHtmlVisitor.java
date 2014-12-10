package com.beligum.blocks.core.parsers.jsoup;

import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
* Created by wouter on 23/11/14.
 * Visitor holding all functionalities to go from a stored entity-templates to a html-page
*/
public class ToHtmlVisitor extends AbstractVisitor
{

    @Override
    public Node head(Node node, int depth) throws ParseException
    {

        if (node instanceof Element) {
            String unversionedResourceId = getReferencedId(node);
            if (!StringUtils.isEmpty(unversionedResourceId)) {
                try {
                    RedisID id = new RedisID(unversionedResourceId, RedisID.LAST_VERSION);
                    EntityTemplate resource = Redis.getInstance().fetchEntityTemplate(id);
                    //TODO BAS: here choices need to be made about can-layout, can-edit, class-layout and instances
                    node = replaceReferenceWithEntity(node, resource);
                }
                catch(Exception e){
                    throw new ParseException("Error while parsing node '" + node.nodeName() + "' at tree depth '" + depth + "'.", e);
                }
            }
        }
        node = super.head(node, depth);
        return node;
    }

}

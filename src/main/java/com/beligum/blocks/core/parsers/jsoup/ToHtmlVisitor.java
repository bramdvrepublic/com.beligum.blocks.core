package com.beligum.blocks.core.parsers.jsoup;

import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
* Created by wouter on 23/11/14.
*/
public class ToHtmlVisitor extends AbstractVisitor
{

    @Override
    public Node head(Node node, int depth) throws ParseException
    {

        if (node instanceof Element) {
            Element element = (Element)node;
            String unversionedResourceId = getReferencedId(element);
            if (!StringUtils.isEmpty(unversionedResourceId)) {
                try {
                    RedisID id = new RedisID(unversionedResourceId, RedisID.LAST_VERSION);
                    EntityTemplate resource = Redis.getInstance().fetchEntityTemplate(id);
                    if (resource != null) {
                        Document resourceDOM = Jsoup.parse(resource.getTemplate());
                        node.replaceWith(this.head(resourceDOM, depth + 1));
                    }
                }
                catch(ParseException e){
                    throw e;
                }
                catch(Exception e){
                    throw new ParseException("Error while parsing node '" + node.nodeName() + "' at tree depth '" + depth + "'.", e);
                }
            }
        }
        super.head(node, depth);
        return node;
    }

}

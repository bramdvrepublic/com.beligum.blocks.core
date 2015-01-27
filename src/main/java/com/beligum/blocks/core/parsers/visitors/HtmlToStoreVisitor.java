package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplateClass;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URL;

/**
 * Created by bas on 10.12.14.
 * Visitor holding all functionalities to go from html to an updated stored instance of entities
 */
public class HtmlToStoreVisitor extends SuperVisitor
{
    /**The root-node of the html-tree being parsed*/
    private final Document root;

    private final String language;

    /**
     *
     * @param entityUrl The url of the entity of which a certain language will be updated.
     * @throws ParseException If no language-information is specified in the entityUrl.
     */
    public HtmlToStoreVisitor(URL entityUrl, Document root) throws ParseException {
        try {
            if(entityUrl == null){
                throw new NullPointerException("Found empty entity-url.");
            }
            if(root == null){
                throw new NullPointerException("Found null-root.");
            }
            this.pageUrl = entityUrl;
            this.root = root;
            this.language = new RedisID(this.pageUrl, RedisID.NO_VERSION, false).getLanguage();
            if(!Languages.isNonEmptyLanguageCode(this.language)){
                throw new ParseException("Cannot update entity '" + entityUrl + "'. It's url does not hold language-information.");
            }
        }catch (Exception e){
            throw new ParseException("Could not parse language from page-url '" + this.pageUrl + "'.");
        }
    }

    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        return super.head(node, depth);
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        try {
            node = super.tail(node, depth);
            if (isEntity(node) && node instanceof Element) {
                /*
                 * If we reached an entity, which was the root of the received html, this entity should have the page-url as resource.
                 * This is done so we can parse entities which aren't imbedded in a html-page
                 */
                if(root.equals(node.parent())){
                    node.attr(ParserConstants.RESOURCE, pageUrl.toString());
                }
                String resourceUrl = getResource(node);
                EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(getTypeOf(node));
                RedisID resourceId;
                if(StringUtils.isEmpty(resourceUrl)) {
                    resourceId = RedisID.renderNewEntityTemplateID(entityTemplateClass, this.language);
                    resourceUrl = resourceId.getUrl().toString();
                    node.attr(ParserConstants.RESOURCE, resourceUrl);
                }
                else{
                    resourceId = RedisID.renderLanguagedId(new URL(resourceUrl), RedisID.LAST_VERSION, this.language);
                }
                EntityTemplate storedEntityTemplate = (EntityTemplate) Redis.getInstance().fetchLastVersion(resourceId, EntityTemplate.class);
                RedisID newVersionId = RedisID.renderLanguagedId(new URL(resourceUrl), RedisID.NEW_VERSION, this.language);
                EntityTemplate currentEntityTemplate = new EntityTemplate(newVersionId, entityTemplateClass, node.outerHtml());
                if (currentEntityTemplate.equals(storedEntityTemplate)) {
                    currentEntityTemplate = storedEntityTemplate;
                }
                else {
                    Redis.getInstance().save(currentEntityTemplate);
                }
                node = replaceElementWithEntityReference((Element) node, currentEntityTemplate);
            }
            return node;
        }catch(Exception e){
            throw new ParseException("Could not parse resource-node: \n \n " + node, e);
        }
    }
}

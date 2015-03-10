package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.URLMapping.XMLUrlIdMapper;
import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlocksID;
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
     * @param language The language that will be used, if no language info can be found in the specified url
     * @throws ParseException If no language-information is specified in the entityUrl.
     */
    public HtmlToStoreVisitor(URL entityUrl, String language, Document root) throws ParseException {
        try {
            if(entityUrl == null){
                throw new NullPointerException("Found empty entity-url.");
            }
            if(root == null){
                throw new NullPointerException("Found null-root.");
            }
            this.entityUrl = entityUrl;
            this.root = root;
            String foundLanguage = Languages.determineLanguage(this.entityUrl.toString());
            if(Languages.isNonEmptyLanguageCode(foundLanguage)) {
                this.language = foundLanguage;
            }
            else if(Languages.isNonEmptyLanguageCode(language)){
                this.language = language;
            }
            else{
                throw new ParseException("Cannot update entity '" + entityUrl + "'. It's url does not hold language-information and the unknown language '" + language + "' has been specified as a backup");
            }
        }catch (Exception e){
            throw new ParseException("Could not parse language from entity url '" + this.entityUrl + "'.");
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
                 * If we reached an entity, which was the root of the received html, this entity should have the entity url as resource.
                 * This is done so we can parse entities which aren't imbedded in a html-page
                 */
                if(root.equals(node.parent())){
                    node.attr(ParserConstants.RESOURCE, entityUrl.toString());
                }
                String resourceUrl = getResource(node);
                EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(getBlueprintType(node));
                BlocksID resourceId;
                if(StringUtils.isEmpty(resourceUrl)) {
                    resourceId = BlocksID.renderNewEntityTemplateID(entityTemplateClass, this.language);
                    resourceUrl = XMLUrlIdMapper.getInstance().getUrl(resourceId).toString();
                    node.attr(ParserConstants.RESOURCE, resourceUrl);
                }
                else{
                    resourceId = BlocksID.renderLanguagedId(new URL(resourceUrl), BlocksID.LAST_VERSION, this.language);
                }
                EntityTemplate storedEntityTemplate = (EntityTemplate) RedisDatabase.getInstance().fetchLastVersion(resourceId, EntityTemplate.class);
                BlocksID newVersionId = BlocksID.renderLanguagedId(new URL(resourceUrl), BlocksID.NEW_VERSION, this.language);
                EntityTemplate currentEntityTemplate = new EntityTemplate(newVersionId, entityTemplateClass, node.outerHtml());
                if (currentEntityTemplate.equals(storedEntityTemplate)) {
                    currentEntityTemplate = storedEntityTemplate;
                }
                else if(storedEntityTemplate == null) {
                    RedisDatabase.getInstance().create(currentEntityTemplate);
                }
                else{
                    RedisDatabase.getInstance().update(currentEntityTemplate);
                }
                node = replaceElementWithEntityReference((Element) node, currentEntityTemplate);
            }
            return node;
        }catch(Exception e){
            throw new ParseException("Could not parse resource-node: \n \n " + node, e);
        }
    }
}

package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.URLMapping.XMLUrlIdMapper;
import com.beligum.blocks.core.caching.BlueprintsCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.Blueprint;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Created by bas on 10.12.14.
 * Visitor holding all functionalities to go from html to an updated stored instance of entities
 */
public class HtmlToStoreVisitor extends SuperVisitor
{
    //TODO: make TypeOf.class with properties (String or TypeOf, per language), fill it up and store it to a db; properties of blueprints which aren't typeofs inside a typeof are also stored in the properties, but if multiple properties are inside such a blueprint, a warning must be displayed

    /**The root-node of the html-tree being parsed*/
    private final Document root;

    protected Stack<Map<String, EntityTemplate>> propertiesStack = new Stack<>();

    private EntityTemplate foundEntityRoot = null;

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
        node = super.head(node, depth);
        if(isEntity(node)){
            this.propertiesStack.push(new HashMap<String, EntityTemplate>());
        }
        return node;
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
                Blueprint blueprint = BlueprintsCache.getInstance().get(getBlueprintType(node));
                BlocksID resourceId;
                if(StringUtils.isEmpty(resourceUrl)) {
                    resourceId = BlocksID.renderNewEntityTemplateID(blueprint, this.language);
                    resourceUrl = XMLUrlIdMapper.getInstance().getUrl(resourceId).toString();
                    node.attr(ParserConstants.RESOURCE, resourceUrl);
                }
                else{
                    resourceId = BlocksID.renderLanguagedId(new URL(resourceUrl), BlocksID.LAST_VERSION, this.language);
                }
                BlocksID newVersionId = BlocksID.renderLanguagedId(new URL(resourceUrl), BlocksID.NEW_VERSION, this.language);
                EntityTemplate currentEntityTemplate = new EntityTemplate(newVersionId, blueprint, node.outerHtml());
                currentEntityTemplate = (EntityTemplate) RedisDatabase.getInstance().createOrUpdate(resourceId, currentEntityTemplate, EntityTemplate.class);

                //add this entity as a property of it's parent if needed
                currentEntityTemplate.setProperties(propertiesStack.pop());
                if(isProperty(node)) {
                    if(propertiesStack.size()>0) {
                        propertiesStack.peek().put(getPropertyKey(node), currentEntityTemplate);
                    }
                    else{
                        foundEntityRoot = currentEntityTemplate;
                    }
                }
                node = replaceElementWithEntityReference((Element) node, currentEntityTemplate);
            }
            return node;
        }catch(Exception e){
            throw new ParseException("Could not parse resource-node.", e, node);
        }
    }

    public EntityTemplate getFoundEntityRoot(){
        return foundEntityRoot;
    }
}

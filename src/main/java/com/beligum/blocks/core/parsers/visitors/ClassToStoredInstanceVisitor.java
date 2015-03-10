package com.beligum.blocks.core.parsers.visitors;

import com.beligum.blocks.core.URLMapping.XMLUrlIdMapper;
import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplateClass;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URL;
import java.util.Stack;

/**
 * Created by bas on 03.12.14.
 * Visitor holding all functionalities to go from a cached class-template to a new instance
 */
public class ClassToStoredInstanceVisitor extends SuperVisitor
{
    //TODO BAS: inject css-classes of class into instance, afterwards add classes of instance (which could overwrite the class-css)

    //the parent-nodes of the entity-template instances to be created
    private Stack<Node> newInstancesNodes = new Stack<>();

    private boolean parsingNewLanguage = false;

    private final String language;

    /**
     *
     * @param entityUrl the url of an entity (that is the url of it's id, not the human readable url)
     * @param language the language the new instance will have, if this is not a correct language, the default language will be used
     * @throws IDException if the specified entity url cannot be used as an id
     */
    public ClassToStoredInstanceVisitor(URL entityUrl, String language) throws ParseException
    {
        try{
            BlocksID entityId = XMLUrlIdMapper.getInstance().getId(entityUrl);
            EntityTemplate entity = (EntityTemplate) RedisDatabase.getInstance().fetchLastVersion(entityId, EntityTemplate.class);
            if(entity != null && !entity.getLanguage().equals(language)){
                parsingNewLanguage = true;
            }
            this.entityUrl = entityId.getUrl();
            if(Languages.isNonEmptyLanguageCode(language)) {
                this.language = language;
            }
            else{
                this.language = entityId.getLanguage();
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
                String unversionedResourceId = getReferencedId(node);
                String defaultPropertyId = getPropertyId(node);
                String blueprintType = getBlueprintType(node);
                EntityTemplate instance = null;
                // this element has a reference to an instance, it is a property of an entity(-class) and the referenced id is the one of a default-property
                // => this is a reference to the default value of the property
                if (!StringUtils.isEmpty(unversionedResourceId) && !StringUtils.isEmpty(defaultPropertyId) && unversionedResourceId.equals(defaultPropertyId)){
                    EntityTemplate defaultPropertyTemplate = this.fetchDefaultEntityTemplate(unversionedResourceId);
                    instance = defaultPropertyTemplate;
                }
                // this is not a default property but still an entity and has an id
                else if(!StringUtils.isEmpty(unversionedResourceId) && !StringUtils.isEmpty(blueprintType)){
                    // Fetch the default value in the db for this resource
                    EntityTemplate defaultEntityTemplate = this.fetchDefaultEntityTemplate(unversionedResourceId);
                    // Make a new entity-template-instance, which is a copy of the default-tempalte
                    EntityTemplateClass entityClass = EntityTemplateClassCache.getInstance().get(blueprintType);
                    // If the current language is not present in the default template, copy the template in the primary-language to the new language
                    if(defaultEntityTemplate.getTemplate(this.language) == null){
                        BlocksID newLanguageId = BlocksID.renderLanguagedId(defaultEntityTemplate.getId().getUrl(), BlocksID.NEW_VERSION, this.language);
                        defaultEntityTemplate.add(newLanguageId, defaultEntityTemplate.getTemplate());
                    };
                    EntityTemplate newEntityInstance = new EntityTemplate(BlocksID.renderNewEntityTemplateID(entityClass, this.language), entityClass, defaultEntityTemplate.getTemplates());
                    instance = newEntityInstance;
                }
                if(instance != null) {
                    node = replaceNodeWithEntity(node, instance);
                }
                else{
                    node = setUseBlueprintType(node);
                }
                newInstancesNodes.push(node);
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
            node = super.tail(node, depth);
            Node lastInstanceNode = !newInstancesNodes.isEmpty() ? newInstancesNodes.peek() : null;
            if (node.equals(lastInstanceNode) && node instanceof Element) {
                EntityTemplateClass entityClass = EntityTemplateClassCache.getInstance().get(getBlueprintType(node));
                BlocksID newEntityId;
                // For the first root entity use entityUrl if available
                if (newInstancesNodes.size() == 1 && entityUrl != null) {
                    newEntityId = BlocksID.renderLanguagedId(entityUrl, BlocksID.NEW_VERSION, this.language);
                }
                //else render a new entity-template-id
                else{
                    newEntityId = BlocksID.renderNewEntityTemplateID(entityClass, this.language);
                }
                node.attr(ParserConstants.RESOURCE, XMLUrlIdMapper.getInstance().getUrl(newEntityId).toString());
                EntityTemplate newInstance = new EntityTemplate(newEntityId, entityClass, node.outerHtml());
                //for default instances, a version could already be present in db, which is equal to this one
                EntityTemplate storedInstance = (EntityTemplate) RedisDatabase.getInstance().fetchLastVersion(newEntityId, EntityTemplate.class);
                if(storedInstance == null) {
                    RedisDatabase.getInstance().create(newInstance);
                }
                else if(!newInstance.equals(storedInstance)){
                    RedisDatabase.getInstance().update(newInstance);
                }
                node = replaceElementWithEntityReference((Element) node, newInstance);
                newInstancesNodes.pop();
            }
            return node;
        }
        catch (ParseException e){
            throw e;
        }
        catch (Exception e) {
            throw new ParseException("Could not parse an " + EntityTemplate.class.getSimpleName() + "-instance from " + Node.class.getSimpleName() + " \n\n" + node + "\n\n", e);
        }
    }

    /**
     * Determine and fetch the default entity-template. First try to fetch the language we're parsing, if not found, fetch the primary language of the default template.
     * @param unversionedResourceId
     * @throws IDException
     * @throws com.beligum.blocks.core.exceptions.DatabaseException
     * @throws ParseException
     */
    private EntityTemplate fetchDefaultEntityTemplate(String unversionedResourceId) throws IDException, DatabaseException, ParseException {
        BlocksID defaultEntityId = new BlocksID(unversionedResourceId, BlocksID.LAST_VERSION, this.language);
        EntityTemplate defaultEntityTemplate = (EntityTemplate) RedisDatabase.getInstance().fetch(defaultEntityId, EntityTemplate.class);
        // If no such default template could be found, we're probably dealing with another language, which needs to be a copy of the primary-language
        if(defaultEntityTemplate == null){
            defaultEntityTemplate = (EntityTemplate) RedisDatabase.getInstance().fetchLastVersion(defaultEntityId, EntityTemplate.class);
            if(defaultEntityTemplate == null) {
                throw new ParseException("Found bad reference. Not present in db: " + defaultEntityId);
            }
        }
        return defaultEntityTemplate;
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
            entityClassName = this.getBlueprintType(node);
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

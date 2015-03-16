package com.beligum.blocks.core.models.redis.templates;

import com.beligum.blocks.core.caching.BlueprintsCache;
import com.beligum.blocks.core.caching.PageTemplatesCache;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.*;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.blocks.core.utils.Utils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bas on 05.11.14.
 */
public class EntityTemplate extends AbstractTemplate
{
    /**the class of which this viewable is a viewable-instance*/
    protected String blueprintType = ParserConstants.DEFAULT_BLUEPRINT;

    private String pageTemplateName = ParserConstants.DEFAULT_PAGE_TEMPLATE;

    /**
     *
     * Constructor for a new entity-instance of a certain entity-class. It will uses copies of the templates of the entity-class.
     * It's UID will be the of the form "[url]:[version]". It uses the current application version and the currently logged in user for field initialization.
     * @param id the id of this entity* @param entityTemplateClass the class of which this entity is a entity-instance
     * @param templatesToBeCopied a map (languageId -> html-template) with all language-templates, only the language present in the keys of the map will be used to put into this maps templates
     *                  this can be used to copy one template's language-templates to another one
     * @throws IDException if no template in the language specified by the id could be found in the templates-map, or if that map was empty
     */
    public EntityTemplate(BlocksID id, Blueprint blueprint, Map<BlocksID, String> templatesToBeCopied) throws IDException, CacheException
    {
        //no scripts or links are specified for an entity-template (this could be implemented later, if wanted)
        super(id, (Map) null, null, null);
        this.templates = new HashMap<>();
        for(BlocksID classTemplateId : templatesToBeCopied.keySet()){
            this.templates.put(new BlocksID(id, classTemplateId.getLanguage()), templatesToBeCopied.get(classTemplateId));
        }
        this.blueprintType = blueprint.getName();
        this.pageTemplateName = blueprint.getPageTemplateName();
        if(!this.getLanguages().contains(id.getLanguage()) || templatesToBeCopied.isEmpty()){
            throw new IDException("No html-template in language '" + id.getLanguage() + "' found between templates.");
        }
    }

    /**
     * Constructor for template with one language: the one precent in the id. (Other language-templates could be added later if wanted.)
     * @param id       id for this template
     * @param blueprint the class of which this entity is a entity-instance
     * @param template the html-template of this template
     */
    public EntityTemplate(BlocksID id, Blueprint blueprint, String template) throws CacheException
    {
        super(id, template, null, null);
        this.blueprintType = blueprint.getName();
        this.pageTemplateName = blueprint.getPageTemplateName();
    }

    /**
     * Constructor used by static create-from-hash-function.
     * @param id
     * @param blueprintType
     * @param templates
     * @throw IDException if no template in the language specified by the id could be found in the templates-map
     */
    private EntityTemplate(BlocksID id, String blueprintType, Map<BlocksID, String> templates) throws IDException
    {
        super(id, templates, null, null);
        this.blueprintType = blueprintType;
        if(!this.getLanguages().contains(id.getLanguage())){
            throw new IDException("No html-template in language '" + id.getLanguage() + "' found between templates.");
        }
    }

    /**
     * The EntityTemplate-class can be used as a factory, to construct entity-templates from data found in a hash in the redis-db
     * @param hash a map, mapping field-names to field-values
     * @return an entity-template or throws an exception if no entity-template could be constructed from the specified hash
     * @throws DeserializationException when a bad hash is found
     */
    //is protected so that all classes in package can access this method
    protected static EntityTemplate createInstanceFromHash(BlocksID id, Map<String, String> hash) throws DeserializationException
    {
        try{
            if(hash != null && !hash.isEmpty() && hash.containsKey(DatabaseConstants.BLUEPRINT_TYPE)) {
                /*
                 * Fetch all fields from the hash, removing them as they are used.
                 * Afterwards use all remaining information to be wired to the a new instance
                 */
                Map<BlocksID, String> templates = AbstractTemplate.fetchLanguageTemplatesFromHash(hash);
                String blueprintType = hash.get(DatabaseConstants.BLUEPRINT_TYPE);
                hash.remove(DatabaseConstants.BLUEPRINT_TYPE);
                EntityTemplate newInstance = new EntityTemplate(id, blueprintType, templates);
                //all the information that still remains in the hash is wired to the instance
                Utils.autowireDaoToModel(hash, newInstance);
                return newInstance;
            }
            else{
                throw new DeserializationException("Hash doesn't contain key '" + DatabaseConstants.BLUEPRINT_TYPE + "'."  + hash);
            }
        }catch (Exception e){
            throw new DeserializationException("Could not construct an entity-template from the specified hash", e);
        }
    }

    public static EntityTemplate copyToNewId(EntityTemplate original, BlocksID newId) throws Exception
    {
        try {
            Map<String, String> hash = original.toHash();
            EntityTemplate newVersion = new EntityTemplate(newId, original.getEntityTemplateClass(), original.getTemplates());
            //all the information that still remains in the hash is wired to the instance
            Utils.autowireDaoToModel(hash, newVersion);
            return newVersion;

        }catch (Exception e){
            throw new Exception("Could not copy " + EntityTemplate.class.getSimpleName() + " '" + original.getName() + "' to new version '" + newId + "'.", e);
        }
    }

    @Override
    public List<String> getScripts() throws Exception
    {
        List<String> scripts = super.getScripts();
        if(scripts == null){
            scripts = this.getEntityTemplateClass().getScripts();
        }
        return scripts;
    }
    @Override
    public List<String> getLinks() throws Exception
    {

        List<String> links = super.getLinks();
        if(links == null){
            links = this.getEntityTemplateClass().getScripts();
        }
        return links;
    }
    /**
     *
     * @return the entity-class of this entity-instance
     */
    public Blueprint getEntityTemplateClass() throws Exception
    {
        return BlueprintsCache.getInstance().get(this.blueprintType);
    }

    public String getBlueprintType()
    {
        return blueprintType;
    }
    public String getPageTemplateName()
    {
        return pageTemplateName;
    }
    /**
     *
     * @return a url to the latest version of this entity
     */
    public URL getUrl(){
        return this.getId().getUrl();
    }

    /**
     *
     * @return the default page-template this entity-template should be rendered in, fetched from cache
     */
    public PageTemplate getPageTemplate() throws Exception
    {
        return PageTemplatesCache.getInstance().get(pageTemplateName);
    }


    public void setPageTemplateName(String pageTemplateName){
        this.pageTemplateName = pageTemplateName;
    }

    /**
     *
     * @return the url of this entity
     */
    @Override
    public String getName()
    {
        return this.getUrl().toString();
    }
    /**
     * render the html of this entity-template, using it's page-template (or, if it is the default-page-template, use the page-template of the class) and class-template
     */
    public String renderEntityInPageTemplate(String language) throws Exception
    {
        PageTemplate pageTemplate = getPageTemplate();
        PageTemplate classPageTemplate = this.getEntityTemplateClass().getPageTemplate();
        if(pageTemplate.getName().equals(ParserConstants.DEFAULT_PAGE_TEMPLATE) && !classPageTemplate.getName().equals(ParserConstants.DEFAULT_PAGE_TEMPLATE)){
            pageTemplate = classPageTemplate;
        }
        return TemplateParser.renderEntityInsidePageTemplate(pageTemplate, this, language);
    }

    /**
     * Gives a hash-representation of this storable to save to the db. This method decides what information is stored in db, and what is not.
     *
     * @return a map representing the key-value structure of this element to be saved to db
     */
    @Override
    public Map<String, String> toHash() throws SerializationException
    {
        try {
            Map<String, String> hash = super.toHash();
            String blueprintType = hash.remove(EntityTemplate.class.getDeclaredField("blueprintType").getName());
            hash.put(DatabaseConstants.BLUEPRINT_TYPE, blueprintType);
            //an entity-template doesn't have links and scripts (for the moment, this could be implemented later)
            hash.remove(DatabaseConstants.SCRIPTS);
            hash.remove(DatabaseConstants.LINKS);
            return hash;
        }catch(NoSuchFieldException e){
            throw new SerializationException("Could not transform " + EntityTemplate.class.getName() + " into hash.", e);
        }
    }

    //___________OVERRIDE OF OBJECT_____________//

    /**
     * Two templates are equal when their template-content, url and meta-data are equal
     * (thus equal through object-state, not object-address).
     * @param obj
     * @return true if two templates are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        if(!(obj instanceof EntityTemplate)) {
            return false;
        }
        else{
            boolean equals = super.equals(obj);
            if(!equals){
                return false;
            }
            else{
                EntityTemplate templObj = (EntityTemplate) obj;
                EqualsBuilder significantFieldsSet = new EqualsBuilder();
                significantFieldsSet = significantFieldsSet.append(pageTemplateName, templObj.pageTemplateName);
                return significantFieldsSet.isEquals();
            }
        }
    }

    /**
     * Two templates have the same hashCode when their template-content, url and meta-data are equal.
     * (thus equal through object-state, not object-address)
     */
    @Override
    public int hashCode()
    {
        int hashCode = super.hashCode();
        HashCodeBuilder significantFieldsSet = new HashCodeBuilder(3, 41);
        significantFieldsSet = significantFieldsSet.appendSuper(hashCode)
                                                   .append(this.pageTemplateName);
        return significantFieldsSet.toHashCode();
    }
}

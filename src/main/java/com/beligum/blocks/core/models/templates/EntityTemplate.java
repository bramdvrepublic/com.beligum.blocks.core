package com.beligum.blocks.core.models.templates;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.*;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.ifaces.Storable;
import com.beligum.blocks.core.parsers.TemplateParser;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bas on 05.11.14.
 */
public class EntityTemplate extends AbstractTemplate implements Storable
{
    /**the class of which this viewable is a viewable-instance*/
    protected String entityTemplateClassName = ParserConstants.DEFAULT_ENTITY_TEMPLATE_CLASS;

    private String pageTemplateName = ParserConstants.DEFAULT_PAGE_TEMPLATE;


//    /**
//     *
//     * Constructor for a new entity-instance of a certain entity-class. It will uses copies of the templates of the entity-class.
//     * It's UID will be the of the form "[url]:[version]". It uses the current application version and the currently logged in user for field initialization.
//     * @param id the id of this entity* @param entityTemplateClass the class of which this entity is a entity-instance
//     * @throw IDException if no template in the language specified by the id could be found in the templates-map
//     */
//    public EntityTemplate(RedisID id, EntityTemplateClass entityTemplateClass) throws IDException
//    {
//        super(id, (Map) null);
//        this.templates = new HashMap<>();
//        Map<RedisID, String> classTemplates = entityTemplateClass.getTemplates();
//        for(RedisID classTemplateId : classTemplates.keySet()){
//            this.templates.put(new RedisID(id, classTemplateId.getLanguage()), classTemplates.get(classTemplateId));
//        }
//        this.entityTemplateClassName = entityTemplateClass.getName();
//        this.pageTemplateName = entityTemplateClass.getPageTemplateName();
//        if(!this.getLanguages().contains(id.getLanguage())){
//            throw new IDException("No html-template in language '" + id.getLanguage() + "' found between templates.");
//        }
//    }

    /**
     *
     * Constructor for a new entity-instance of a certain entity-class. It will uses copies of the templates of the entity-class.
     * It's UID will be the of the form "[url]:[version]". It uses the current application version and the currently logged in user for field initialization.
     * @param id the id of this entity* @param entityTemplateClass the class of which this entity is a entity-instance
     * @param templatesToBeCopied a map (languageId -> html-template) with all language-templates, only the language present in the keys of the map will be used to put into this maps templates
     *                  this can be used to copy one template's language-templates to another one
     * @throw IDException if no template in the language specified by the id could be found in the templates-map, or if that map was empty
     */
    public EntityTemplate(RedisID id, EntityTemplateClass entityTemplateClass, Map<RedisID, String> templatesToBeCopied) throws IDException
    {
        super(id, (Map) null);
        this.templates = new HashMap<>();
        for(RedisID classTemplateId : templatesToBeCopied.keySet()){
            this.templates.put(new RedisID(id, classTemplateId.getLanguage()), templatesToBeCopied.get(classTemplateId));
        }
        this.entityTemplateClassName = entityTemplateClass.getName();
        this.pageTemplateName = entityTemplateClass.getPageTemplateName();
        if(!this.getLanguages().contains(id.getLanguage()) || templatesToBeCopied.isEmpty()){
            throw new IDException("No html-template in language '" + id.getLanguage() + "' found between templates.");
        }
    }

    /**
     * Constructor for template with one language: the one precent in the id. (Other language-templates could be added later if wanted.)
     * @param id       id for this template
     * @param entityTemplateClass the class of which this entity is a entity-instance
     * @param template the html-template of this template
     */
    public EntityTemplate(RedisID id, EntityTemplateClass entityTemplateClass, String template)
    {
        super(id, template);
        this.entityTemplateClassName = entityTemplateClass.getName();
        this.pageTemplateName = entityTemplateClass.getPageTemplateName();
    }

    /**
     * Constructor used by static create-from-hash-function.
     * @param id
     * @param entityTemplateClassName
     * @param templates
     * @param pageTemplateName
     * @throw IDException if no template in the language specified by the id could be found in the templates-map
     */
    private EntityTemplate(RedisID id, String entityTemplateClassName, Map<RedisID, String> templates, String pageTemplateName) throws IDException
    {
        super(id, templates);
        this.entityTemplateClassName = entityTemplateClassName;
        this.pageTemplateName = pageTemplateName;
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
    protected static EntityTemplate createInstanceFromHash(RedisID id, Map<String, String> hash) throws DeserializationException
    {
        try{
            if(hash != null && !hash.isEmpty() && hash.containsKey(DatabaseConstants.ENTITY_TEMPLATE_CLASS)) {
                Map<RedisID, String> templates = AbstractTemplate.fetchLanguageTemplatesFromHash(hash);
                EntityTemplate newInstance = new EntityTemplate(id, hash.get(DatabaseConstants.ENTITY_TEMPLATE_CLASS), templates, hash.get(DatabaseConstants.PAGE_TEMPLATE));
                newInstance.applicationVersion = hash.get(DatabaseConstants.APP_VERSION);
                newInstance.creator = hash.get(DatabaseConstants.CREATOR);
                return newInstance;
            }
            else{
                throw new DeserializationException("Hash doesn't contain key '" + DatabaseConstants.ENTITY_TEMPLATE_CLASS + "'."  + hash);
            }
        }catch (Exception e){
            throw new DeserializationException("Could not construct an entity-template from the specified hash", e);
        }
    }

    /**
     *
     * @return the entity-class of this entity-instance
     */
    public EntityTemplateClass getEntityTemplateClass() throws CacheException
    {
        return EntityTemplateClassCache.getInstance().get(this.entityTemplateClassName);
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
    public PageTemplate getPageTemplate() throws CacheException
    {
        return PageTemplateCache.getInstance().get(pageTemplateName);
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
     * @return
     */
    public String renderEntityInPageTemplate(String language) throws CacheException, ParseException
    {
        PageTemplate pageTemplate = getPageTemplate();
        PageTemplate classPageTemplate = this.getEntityTemplateClass().getPageTemplate();
        if(pageTemplate.getName().equals(ParserConstants.DEFAULT_PAGE_TEMPLATE) && !classPageTemplate.getName().equals(ParserConstants.DEFAULT_PAGE_TEMPLATE)){
            pageTemplate = classPageTemplate;
        }
        return TemplateParser.renderEntityInsidePageTemplate(pageTemplate, this, language);
    }

    /**
     * render the html of this entity-template, without using a page-template
     * @return
     * @throws ParseException
     */
    public String renderEntity() throws ParseException
    {
        return TemplateParser.renderTemplate(this);
    }

    /**
     * Gives a hash-representation of this storable to save to the db. This method decides what information is stored in db, and what is not.
     *
     * @return a map representing the key-value structure of this element to be saved to db
     */
    @Override
    public Map<String, String> toHash() throws SerializationException
    {
        Map<String, String> hash = super.toHash();
        hash.put(DatabaseConstants.ENTITY_TEMPLATE_CLASS, this.entityTemplateClassName);
        hash.put(DatabaseConstants.PAGE_TEMPLATE, this.pageTemplateName);
        return hash;
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
     * @return
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

package com.beligum.blocks.core.models.templates;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.DeserializationException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.ifaces.Storable;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.net.URL;
import java.util.Map;

/**
 * Created by bas on 05.11.14.
 */
public class EntityTemplate extends AbstractTemplate implements Storable
{
    /**the class of which this viewable is a viewable-instance*/
    protected final EntityTemplateClass entityTemplateClass;

    private String pageTemplateName = ParserConstants.DEFAULT_PAGE_TEMPLATE;

    /**
     *
     * Constructor for a new entity-instance of a certain entity-class, which will be filled with the default rows and blocks from the entity-class.
     * It's UID will be the of the form "[url]:[version]". It used the current application version and the currently logged in user for field initialization.
     * @param id the id of this entity
     * @param entityTemplateClass the class of which this entity is a entity-instance
     * @throw URISyntaxException if a url is specified not formatted strictly according to to RFC2396
     */
    public EntityTemplate(RedisID id, EntityTemplateClass entityTemplateClass)
    {
        //a entity cannot be altered by the client, so it always is final
        super(id, entityTemplateClass.getTemplate());
        this.entityTemplateClass = entityTemplateClass;
        this.pageTemplateName = entityTemplateClass.getPageTemplateName();
    }

    public EntityTemplate(RedisID id, EntityTemplateClass entityTemplateClass, String template){
        super(id, template);
        this.entityTemplateClass = entityTemplateClass;
        this.pageTemplateName = entityTemplateClass.getPageTemplateName();
    }

    /**
     * The EntityTemplate-class can be used as a factory, to construct entity-templates from data found in a hash in the redis-db
     * @param hash a map, mapping field-names to field-values
     * @return an entity-template or throws an exception if no entity-template could be constructed from the specified hash
     */
    public static EntityTemplate createInstanceFromHash(RedisID id, Map<String, String> hash) throws DeserializationException
    {
        try{
            if(hash != null && !hash.isEmpty() && hash.containsKey(DatabaseConstants.TEMPLATE) && hash.containsKey(DatabaseConstants.ENTITY_TEMPLATE_CLASS)) {
                EntityTemplate newInstance = new EntityTemplate(id, EntityTemplateClassCache.getInstance().get(hash.get(DatabaseConstants.ENTITY_TEMPLATE_CLASS)));
                newInstance.template = hash.get(DatabaseConstants.TEMPLATE);
                newInstance.applicationVersion = hash.get(DatabaseConstants.APP_VERSION);
                newInstance.creator = hash.get(DatabaseConstants.CREATOR);
                newInstance.pageTemplateName = hash.get(DatabaseConstants.PAGE_TEMPLATE);
                //TODO BAS: this should go to AbstractTemplate: here use Field.java or something of the sort, should make sure the rest of the hash (like application version and creator) is filled in, even if not all fields are present in the hash

                return newInstance;
            }
            else{
                Logger.error("Could not construct an entity-template from the specified hash: " + hash);
                throw new DeserializationException("Could not construct an entity-template from the specified hash: " + hash);
            }
        }
        catch(DeserializationException e){
            throw e;
        }
        catch(CacheException e){
            throw new DeserializationException("Could not construct an object of class '" + EntityTemplate.class.getName() + "' from specified hash.", e);
        }
    }

    /**
     *
     * @return the entity-class of this entity-instance
     */
    public EntityTemplateClass getEntityTemplateClass(){
        return this.entityTemplateClass;
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
     * render the html of this entity-template, using it's page-template (or, if it is the default-page-template, use the page-template of the class) and class-template
     * @return
     */
    public String render() throws CacheException, ParseException
    {
        PageTemplate pageTemplate = getPageTemplate();
        PageTemplate classPageTemplate = this.getEntityTemplateClass().getPageTemplate();
        if(pageTemplate.getName().equals(ParserConstants.DEFAULT_PAGE_TEMPLATE) && !classPageTemplate.getName().equals(ParserConstants.DEFAULT_PAGE_TEMPLATE)){
            pageTemplate = classPageTemplate;
        }
        return TemplateParser.renderEntityInsidePageTemplate(pageTemplate, this);
    }


    /**
     * Gives a hash-representation of this storable to save to the db. This method decides what information is stored in db, and what is not.
     *
     * @return a map representing the key-value structure of this element to be saved to db
     */
    @Override
    public Map<String, String> toHash()
    {
        Map<String, String> hash = super.toHash();
        hash.put(DatabaseConstants.ENTITY_TEMPLATE_CLASS, this.getEntityTemplateClass().getName());
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

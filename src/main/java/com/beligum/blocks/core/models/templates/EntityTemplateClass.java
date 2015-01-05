package com.beligum.blocks.core.models.templates;

import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.DeserializationException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;

/**
 * Created by bas on 05.11.14.
 */
public class EntityTemplateClass extends AbstractTemplate
{
    /**the default page-template this class should be rendered in*/
    private String pageTemplateName = ParserConstants.DEFAULT_PAGE_TEMPLATE;
    /**string the name of this entity-class*/
    private String name;

    /**
     *
     * @param name the name of this entity-class
     * @param templates a map relating languages to template-strings corresponding to the most outer layer of the element-tree in this entity
     * @param pageTemplateName the default page-template this entity-class should be rendered in
     */
    public EntityTemplateClass(String name, Map<String, String> templates, String pageTemplateName) throws IDException, CacheException
    {
        super(RedisID.renderNewEntityTemplateClassID(name), templates);
        this.name = name;
        if(pageTemplateName != null) {
            this.pageTemplateName = pageTemplateName;
        }
    }

    private EntityTemplateClass(RedisID id, Map<String, String> templates, String pageTemplateName) throws CacheException
    {
        super(id, templates);
        //the name of this entity-template-class doesn't start with a "/", so we split it of the given path
        String[] splitted = id.getUrl().getPath().split("/");
        if (splitted.length > 0) {
            this.name = splitted[1];
        }
        else {
            this.name = null;
        }
        if(pageTemplateName != null) {
            this.pageTemplateName = pageTemplateName;
        }
    }

    /**
     *
     * @return the name of this entity-class
     */
    public String getName()
    {
        return name;
    }

    /**
     *
     * @return the name of the page-template this entity-class should be rendered in
     */
    public String getPageTemplateName()
    {
        return pageTemplateName;
    }
    /**
     *
     * @return the default page-template this entity-class should be rendered in, fetched from cache
     */
    public PageTemplate getPageTemplate() throws CacheException
    {
        return PageTemplateCache.getInstance().get(pageTemplateName);
    }


    /**
     * returns the base-url for the entity-class
     * @param entityClassName the name of the entity-class (f.i. "default" for a entityClass filtered from the file "entities/default/index.html")
     * @return
     */
    public static URL getBaseUrl(String entityClassName) throws MalformedURLException
    {
        return new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.ENTITY_CLASS_ID_PREFIX + "/" + entityClassName);
    }

    /**
     * The EntityTemplateClass-class can be used as a factory, to construct entity-template-classes from data found in a hash in the redis-db
     * @param hash a map, mapping field-names to field-values
     * @return an entity-template-class or throws an exception if no entity-template-class could be constructed from the specified hash
     */
    public static EntityTemplateClass createInstanceFromHash(RedisID id, Map<String, String> hash) throws DeserializationException
    {
        try {
            if (hash != null && !hash.isEmpty() && hash.containsKey(DatabaseConstants.TEMPLATE)) {
                EntityTemplateClass newInstance = new EntityTemplateClass(id, hash.get(DatabaseConstants.TEMPLATE), hash.get(DatabaseConstants.PAGE_TEMPLATE));
                newInstance.applicationVersion = hash.get(DatabaseConstants.APP_VERSION);
                newInstance.creator = hash.get(DatabaseConstants.CREATOR);
                return newInstance;
            }
            else {
                Logger.error("Could not construct an entity-template-class from the specified hash: " + hash);
                throw new DeserializationException("Could not construct an entity-template-class from the specified hash: " + hash);
            }
        }
        catch(DeserializationException e){
            throw e;
        }
        catch(Exception e){
            throw new DeserializationException("Could not construct an object of class '" + EntityTemplateClass.class.getName() + "' from specified hash.", e);
        }
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
        hash.put(DatabaseConstants.PAGE_TEMPLATE, this.pageTemplateName);
        return hash;
    }

    //________________OVERRIDE OF OBJECT_______________//

    /**
     * Two templates have the same hashCode when their template-content, url and meta-data are equal.
     * (thus equal through object-state, not object-address)
     * @return
     */
    @Override
    public int hashCode()
    {
        int hashCode = super.hashCode();
        HashCodeBuilder significantFieldsSet = new HashCodeBuilder(9, 17);
        significantFieldsSet = significantFieldsSet.appendSuper(hashCode)
                                                   .append(this.pageTemplateName);
        return significantFieldsSet.toHashCode();
    }

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
                EntityTemplateClass templObj = (EntityTemplateClass) obj;
                EqualsBuilder significantFieldsSet = new EqualsBuilder();
                significantFieldsSet = significantFieldsSet.append(pageTemplateName, templObj.pageTemplateName);
                return significantFieldsSet.isEquals();
            }
        }
    }
}

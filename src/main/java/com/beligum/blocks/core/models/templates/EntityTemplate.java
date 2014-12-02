package com.beligum.blocks.core.models.templates;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.ifaces.Storable;
import com.beligum.core.framework.utils.Logger;
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
    //TODO BAS: is this actually a class-object, or just a name?
    protected final EntityTemplateClass entityTemplateClass;

    /**
     *
     * Constructor for a new entity-instance of a certain entity-class, who's template will be the default from the template-class.
     * It's UID will be the of the form "[url]:[version]" and will be rendered by the Redis-singleton.
     * It uses the current application version and the currently logged in user for field initialization.
     * @param entityTemplateClass the class of which this entity is a entity-instance
     * @throw URISyntaxException if a url is specified not formatted strictly according to RFC2396
     */
    public EntityTemplate(EntityTemplateClass entityTemplateClass){
        super(RedisID.renderNewEntityTemplateID(entityTemplateClass), entityTemplateClass.getTemplate());
        this.entityTemplateClass = entityTemplateClass;
    }

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
    }

    /**
     * The EntityTemplate-class can be used as a factory, to construct entity-templates from data found in a hash in the redis-db
     * @param hash a map, mapping field-names to field-values
     * @return an entity-template or throws an exception if no entity-template could be constructed from the specified hash
     */
    public static EntityTemplate createInstanceFromHash(RedisID id, Map<String, String> hash) throws Exception
    {
        if(hash != null && !hash.isEmpty() && hash.containsKey(DatabaseConstants.TEMPLATE) && hash.containsKey(DatabaseConstants.ENTITY_TEMPLATE_CLASS)) {
            EntityTemplate newInstance = new EntityTemplate(id, EntityTemplateClassCache.getInstance().get(hash.get(DatabaseConstants.ENTITY_TEMPLATE_CLASS)));
            newInstance.template = hash.get(DatabaseConstants.TEMPLATE);
            //TODO BAS: maybe this should go to AbstractTemplate?: here use Field.java or something of the sort, should make sure the rest of the hash (like application version and creator) is filled in, even if not all fields are present in the hash

            return newInstance;
        }
        else{
            Logger.error("Could not construct an entity-template from the specified hash: " + hash);
            throw new Exception("Could not construct an entity-template from the specified hash: " + hash);
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

//    /**
//     *
//     * @return all non-final children of this entity that aren't present in it's viewable-class (and thus already in the application-cache)
//     */
//    public HashSet<Entity> getNotCachedNonFinalChildren(){
//        HashSet<Entity> notCachedNonFinalChildren = this.getAllNonFinalChildren();
//        Set<Entity> cachedNonFinalChildren = this.getViewableClass().getAllNonFinalChildren();
//        notCachedNonFinalChildren.removeAll(cachedNonFinalChildren);
//        return notCachedNonFinalChildren;
//    }


    /**
     * @return the name of the variable of this viewable in the template holding this viewable
     */
    public String getTemplateVariableName()
    {
        return this.getUnversionedId();
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
        return hash;
    }

    //___________OVERRIDE OF OBJECT_____________//

    /**
     * Two rows are equal when their template, meta-data (page-class, creator and application-version), site-domain and unversioned element-id (everything after the '#') are equal
     * (thus equal through object-state, not object-address).
     * @param obj
     * @return true if two rows are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof EntityTemplate) {
            if(obj == this){
                return true;
            }
            else {
                EntityTemplate entityTemplateObj = (EntityTemplate) obj;
                EqualsBuilder significantFieldsSet = new EqualsBuilder();
                significantFieldsSet = significantFieldsSet.append(template, entityTemplateObj.template)
                                                           .append(this.getUnversionedId(), entityTemplateObj.getUnversionedId())
//                                                           .append(this.getTemplateVariableName(), entityTemplateObj.getTemplateVariableName())
                                                           .append(this.creator, entityTemplateObj.creator)
                                                           .append(this.applicationVersion, entityTemplateObj.applicationVersion);
                return significantFieldsSet.isEquals();
            }
        }
        else{
            return false;
        }
    }

    /**
     * Two rows have the same hashCode when their template, meta-data (page-class, creator and application-version), site-domain and unversioned element-id (everything after the '#') are equal
     * (thus equal through object-state, not object-address)
     * @return
     */
    @Override
    public int hashCode()
    {
        //7 and 31 are two randomly chosen prime numbers, needed for building hashcodes, ideally, these are different for each class
        HashCodeBuilder significantFieldsSet = new HashCodeBuilder(7, 31);
        significantFieldsSet = significantFieldsSet.append(template)
                                                   .append(this.getUnversionedId())
//                                                   .append(this.getTemplateVariableName())
                                                   .append(this.creator)
                                                   .append(this.applicationVersion);
        return significantFieldsSet.toHashCode();
    }
}

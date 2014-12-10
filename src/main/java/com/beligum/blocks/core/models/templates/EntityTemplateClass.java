package com.beligum.blocks.core.models.templates;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.DeserializationException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.core.framework.utils.Logger;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

/**
 * Created by bas on 05.11.14.
 */
public class EntityTemplateClass extends AbstractTemplate
{
    /**the doctype of this entityclass*/
    private String docType;
    /**string the name of this entity-class*/
    private String name;

    /**
     *
     * @param name the name of this entity-class
     * @param template the template-string corresponding to the most outer layer of the element-tree in this entity
     * @param docType the doctype of this entity-class
     */
    public EntityTemplateClass(String name, String template, String docType) throws IDException
    {
        super(RedisID.renderNewEntityTemplateClassID(name), template);
        this.name = name;
        this.docType = docType;
    }

    private EntityTemplateClass(RedisID id, String template, String docType){
        super(id, template);
        //the name of this entity-template-class doesn't start with a "/", so we split it of the given path
        String[] splitted = id.getUrl().getPath().split("/");
        if(splitted.length>0) {
            this.name = splitted[1];
        }
        else{
            this.name = null;
        }
        this.docType = docType;
    }

    /**
     *
     * @return the name of this entity-class
     */
    public String getName()
    {
        return name;
    }
    public String getDocType()
    {
        return docType;
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
                EntityTemplateClass newInstance = new EntityTemplateClass(id, hash.get(DatabaseConstants.TEMPLATE), hash.get(DatabaseConstants.DOC_TYPE));
                newInstance.applicationVersion = hash.get(DatabaseConstants.APP_VERSION);
                newInstance.creator = hash.get(DatabaseConstants.CREATOR);
                //TODO BAS: this should go to AbstractTemplate: use Field.java or something of the sort, should make sure the rest of the hash (like application version and creator) is filled in, even if not all fields are present in the hash

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


    //________________OVERRIDE OF OBJECT_______________//

    /**
     * Two templates have the same hashCode when their template-content, url and meta-data are equal.
     * (thus equal through object-state, not object-address)
     * @return
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
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
        if(obj instanceof EntityTemplateClass) {
            return super.equals(obj);
        }
        else{
            return false;
        }
    }
}

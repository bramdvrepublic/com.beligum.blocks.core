package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.caching.EntityClassCache;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.classes.EntityClass;

import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 05.11.14.
 */
public class Entity extends ViewableInstance
{
    /**
     *
     * Constructor for a new entity-instance of a certain entity-class, which will be filled with the default rows and blocks from the entity-class.
     * It's UID will be the of the form "[url]:[version]". It used the current application version and the currently logged in user for field initialization.
     * @param id the id of this entity
     * @param entityClass the class of which this entity is a entity-instance
     * @throw URISyntaxException if a url is specified not formatted strictly according to to RFC2396
     */
    public Entity(RedisID id, EntityClass entityClass)
    {
        //a entity cannot be altered by the client, so it always is final
        super(id, entityClass, true);
    }

    /**
     *
     * Constructor for a new entity-instance of a certain entity-class, which will be filled with the default rows and blocks from the entity-class.
     * It's UID will be the of the form "[url]:[version]"
     * @param id the id of this entity
     * @param entityClass the class of which this entity is a entity-instance
     * @param applicationVersion the version of the app this entity was saved under
     * @param creator the creator of this entity
     * @throw URISyntaxException if a url is specified not formatted strictly according to to RFC2396
     */
    public Entity(RedisID id, EntityClass entityClass, String applicationVersion, String creator)
    {
        //a entity cannot be altered by the client, so it always is final
        super(id, entityClass, true);
    }

    /**
     * Constructor for a new entity-instance taking elements fetched from db and a entityclass (fetched from application cache).
     * The rows and blocks are added to this entity in the following order:
     * 1. final elements of entity-class, 2. blocks and rows from database specified in the set, 3. non-final elements of entity-class, whose element-id's are not yet present in the entity
     * @param id the id of this entity
     * @param childrenFromDB the children of the entity fetched form db
     * @param entityClassName the entity-class this entity is an instance of
     * @throws CacheException when the entity-class can not be found in the application cache
     *
     */
    public Entity(RedisID id, Set<Row> childrenFromDB, String entityClassName) throws CacheException
    {
        super(id, childrenFromDB, EntityClassCache.getInstance().get(entityClassName), true);
    }

    /**
     * Constructor for a new entity-instance taking elements fetched from db and a entityclass (fetched from application cache).
     * The rows and blocks are added to this entity in the following order:
     * 1. final elements of entity-class, 2. blocks and rows from database specified in the set, 3. non-final elements of entity-class, whose element-id's are not yet present in the entity
     * @param id the id of this entity
     * @param childrenFromDB the children of the entity fetched form db
     * @param entityClassName the entity-class this entity is an instance of
     * @param applicationVersion the version of the app this entity was saved under
     * @param creator the creator of this entity
     * @throws CacheException when the entity-class can not be found in the application cache
     *
     */
    public Entity(RedisID id, Set<Row> childrenFromDB, String entityClassName, String applicationVersion, String creator) throws CacheException
    {
        super(id, childrenFromDB, EntityClassCache.getInstance().get(entityClassName), true, applicationVersion, creator);
    }

    /**
     *
     * @return the entity-class of this entity-instance
     */
    public EntityClass getEntityClass(){
        return (EntityClass) this.viewableClass;
    }

    /**
     *
     * @return a url to the latest version of this entity
     */
    public URL getUrl(){
        return getId().getUrl();
    }

    /**
     *
     * @return all non-final children of this entity that aren't present in it's viewable-class (and thus already in the application-cache)
     */
    public HashSet<Row> getNotCachedNonFinalChildren(){
        HashSet<Row> notCachedNonFinalChildren = this.getAllNonFinalChildren();
        Set<Row> cachedNonFinalChildren = this.getViewableClass().getAllNonFinalChildren();
        notCachedNonFinalChildren.removeAll(cachedNonFinalChildren);
        return notCachedNonFinalChildren;
    }

    @Override
    public Map<String, String> toHash()
    {
        Map<String, String> hash = super.toHash();
        hash.remove(DatabaseConstants.TEMPLATE);
        return hash;
    }
}

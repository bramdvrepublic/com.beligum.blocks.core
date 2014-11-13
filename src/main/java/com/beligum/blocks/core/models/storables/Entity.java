package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.caching.EntityClassCache;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.identifiers.EntityID;
import com.beligum.blocks.core.models.classes.EntityClass;

import java.net.URL;
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
    public Entity(EntityID id, EntityClass entityClass)
    {
        //a entity cannot be altered by the client, so it always is final
        super(id, entityClass, true);
        this.addChildren(entityClass.getAllChildren());
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
    public Entity(EntityID id, EntityClass entityClass, String applicationVersion, String creator)
    {
        //a entity cannot be altered by the client, so it always is final
        super(id, entityClass, true);
        this.addChildren(entityClass.getAllChildren());
    }

    /**
     * Constructor for a new entity-instance taking children and a entityclass. The rows and blocks of the entityClass are NOT copied to this entity.
     * @param id the id of this entity
     * @param allChildren all children for this entity
     * @param entityClassName the name of the entity-class this entity is an instance of
     * @throws CacheException when the entity-class cannot be found in the application-cache
     */
    public Entity(EntityID id, Set<Row> allChildren, String entityClassName) throws CacheException
    {
        //the template of a entity is always the template of it's entity-class; a entity cannot be altered by the client, so it always is final
        super(id, EntityClassCache.getInstance().get(entityClassName), true);
        this.addChildren(allChildren);
    }

    /**
     * Constructor for a new entity-instance taking elements fetched from db and a entityclass (fetched from application cache).
     * The rows and blocks are added to this entity in the following order:
     * 1. final elements of entity-class, 2. blocks and rows from database specified in the set, 3. non-final elements of entity-class, whose element-id's are not yet present in the entity
     * @param id the id of this entity
     * @param allChildrenFromDB the direct children of the entity
     * @param entityClass the entity-class this entity is an instance of
     * @param applicationVersion the version of the app this entity was saved under
     * @param creator the creator of this entity
     *
     */
    public Entity(EntityID id, Set<Row> allChildrenFromDB, EntityClass entityClass, String applicationVersion, String creator)
    {
        super(id, entityClass, true, applicationVersion, creator);
        this.addChildren(entityClass.getAllFinalChildren().values());
        this.addChildren(allChildrenFromDB);
        this.addChildren(entityClass.getAllNonFinalChildren());
    }

    /**
     *
     * @return the entity-class of this entity-instance
     */
    public EntityClass getPageEntityClass(){
        return (EntityClass) this.viewableClass;
    }

    @Override
    public EntityID getId(){
        return (EntityID) this.id;
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
     * @return the id of the hash containing the info of this entity in the db
     */
    public String getInfoId(){
        return this.getId().getPageInfoId();
    }

    public Map<String, String> getInfo(){
        return this.toHash();
    }
}

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
     * Constructor for a new page-instance of a certain page-class, which will be filled with the default rows and blocks from the page-class.
     * It's UID will be the of the form "[url]:[version]". It used the current application version and the currently logged in user for field initialization.
     * @param id the id of this page
     * @param entityClass the class of which this page is a page-instance
     * @throw URISyntaxException if a url is specified not formatted strictly according to to RFC2396
     */
    public Entity(EntityID id, EntityClass entityClass)
    {
        //a page cannot be altered by the client, so it always is final
        super(id, entityClass, true);
        this.addDirectChildren(entityClass.getDirectChildren());
    }

    /**
     *
     * Constructor for a new page-instance of a certain page-class, which will be filled with the default rows and blocks from the page-class.
     * It's UID will be the of the form "[url]:[version]"
     * @param id the id of this page
     * @param entityClass the class of which this page is a page-instance
     * @param applicationVersion the version of the app this page was saved under
     * @param creator the creator of this page
     * @throw URISyntaxException if a url is specified not formatted strictly according to to RFC2396
     */
    public Entity(EntityID id, EntityClass entityClass, String applicationVersion, String creator)
    {
        //a page cannot be altered by the client, so it always is final
        super(id, entityClass, true);
        this.addDirectChildren(entityClass.getDirectChildren());
    }

    /**
     * Constructor for a new page-instance taking children and a pageclass. The rows and blocks of the pageClass are NOT copied to this page.
     * @param id the id of this page
     * @param directChildren the direct children for this page
     * @param pageClassName the name of the page-class this page is an instance of
     * @throws CacheException when the page-class cannot be found in the application-cache
     */
    public Entity(EntityID id, Set<Row> directChildren, String pageClassName) throws CacheException
    {
        //the template of a page is always the template of it's page-class; a page cannot be altered by the client, so it always is final
        super(id, EntityClassCache.getInstance().get(pageClassName), true);
        this.addDirectChildren(directChildren);
    }

    /**
     * Constructor for a new page-instance taking elements fetched from db and a pageclass (fetched from application cache).
     * The rows and blocks are added to this page in the following order:
     * 1. final elements of page-class, 2. blocks and rows from database specified in the set, 3. non-final elements of page-class, whose element-id's are not yet present in the page
     * @param id the id of this page
     * @param directChildrenFromDB the direct children of the page
     * @param entityClass the page-class this page is an instance of
     * @param applicationVersion the version of the app this page was saved under
     * @param creator the creator of this page
     *
     */
    public Entity(EntityID id, Set<Row> directChildrenFromDB, EntityClass entityClass, String applicationVersion, String creator)
    {
        super(id, entityClass, true, applicationVersion, creator);
        this.addDirectChildren(entityClass.getAllFinalElements().values());
        this.addDirectChildren(directChildrenFromDB);
        this.addDirectChildren(entityClass.getAllNonFinalElements());
    }

    /**
     *
     * @return the page-entity-class of this page-entity-instance
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
     * @return a url to the latest version of this page
     */
    public URL getUrl(){
        return getId().getUrl();
    }

    /**
     *
     * @return the id of the hash containing the info of this page in the db
     */
    public String getInfoId(){
        return this.getId().getPageInfoId();
    }

    public Map<String, String> getInfo(){
        return this.toHash();
    }
}

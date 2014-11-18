package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.classes.AbstractViewableClass;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 05.11.14.
 */
public class ViewableInstance extends Row
{

    /**the class of which this viewable is a viewable-instance*/
    protected final AbstractViewableClass viewableClass;

    /**
     * @param id       the id of this instance (is of the form "[site]/[pageName]#[rowId]")
     * @param viewableClass the class of which this viewable is a viewable-instance
     * @param isFinal  boolean whether or not the template of this instance can be changed by the client
     */
    public ViewableInstance(RedisID id, AbstractViewableClass viewableClass, boolean isFinal)
    {
        //the template of a viewable instance is always the template of it's viewable-class
        super(id, viewableClass.getTemplate(), viewableClass.getAllChildren(), isFinal);
        this.viewableClass = viewableClass;
    }
    /**
     * @param id       the id of this instance (is of the form "[site]/[pageName]#[rowId]")
     * @param viewableClass the class of which this viewable is a viewable-instance
     * @param isFinal  boolean whether or not the template of this instance can be changed by the client
     * @param applicationVersion the version of the application this instance was saved under
     * @param creator            the creator of this instance
     */
    public ViewableInstance(RedisID id, AbstractViewableClass viewableClass, boolean isFinal, String applicationVersion, String creator)
    {
        //the template of a viewable instance is always the template of it's viewable-class
        super(id, viewableClass.getTemplate(), viewableClass.getAllChildren(), isFinal, applicationVersion, creator);
        this.viewableClass = viewableClass;
    }

    /**
     * Constructor for a new viewable-instance taking children and a entityclass. First the final children from the viewableClass are added, then the children specified in the parameter 'allChildren' and in the end the non-final children found in the viewableClass.
     * @param id the id of this entity
     * @param childrenFromDB all children for this entity fetched form db
     * @param viewableClass the viewable-class this viewable is an instance of
     */
    public ViewableInstance(RedisID id, Set<Row> childrenFromDB, AbstractViewableClass viewableClass, boolean isFinal)
    {
        //the children of the viewable class should not be copied to the entity lightly
        super(id, viewableClass.getTemplate(), new HashSet<Row>(), isFinal);
        this.addChildren(viewableClass.getAllFinalChildren().values());
        this.addChildren(childrenFromDB);
        this.addChildren(viewableClass.getAllNonFinalChildren());
        this.viewableClass = viewableClass;
    }

    /**
     * Constructor for a new viewable-instance taking children and a entityclass. First the final children from the viewableClass are added, then the children specified in the parameter 'allChildren' and in the end the non-final children found in the viewableClass.
     * @param id the id of this entity
     * @param childrenFromDB all children for this entity fetched form db
     * @param viewableClass the viewable-class this viewable is an instance of
     * @param applicationVersion the version of the application this instance was saved under
     * @param creator            the creator of this instance
     */
    public ViewableInstance(RedisID id, Set<Row> childrenFromDB, AbstractViewableClass viewableClass, boolean isFinal, String applicationVersion, String creator)
    {
        //the children of the viewable class should not be copied to the entity lightly
        super(id, viewableClass.getTemplate(), new HashSet<Row>(), isFinal, applicationVersion, creator);
        this.addChildren(viewableClass.getAllFinalChildren().values());
        this.addChildren(childrenFromDB);
        this.addChildren(viewableClass.getAllNonFinalChildren());
        this.viewableClass = viewableClass;
    }

    /**
     *
     * @param id the id of this instance (is of the form "[site]/[pageName]#[rowId]")
     * @param content the content of this viewable instance (is probably a template of some sort)
     * @param viewableClass the class of which this viewable is a viewable-instance
     * @param isFinal  boolean whether or not the template of this instance can be changed by the client
     */
    public ViewableInstance(RedisID id, String content, AbstractViewableClass viewableClass, boolean isFinal)
    {
        super(id, content, viewableClass.getAllChildren(), isFinal);
        this.viewableClass = viewableClass;
    }

    /**
     *
     * @param id the id of this instance (is of the form "[site]/[pageName]#[rowId]")
     * @param content the content of this viewable instance (is probably a template of some sort)
     * @param viewableClass the class of which this viewable is a viewable-instance
     * @param isFinal  boolean whether or not the template of this instance can be changed by the client
     * @param applicationVersion the version of the application this instance was saved under
     * @param creator            the creator of this instance
     */
    public ViewableInstance(RedisID id, String content, AbstractViewableClass viewableClass, boolean isFinal, String applicationVersion, String creator)
    {
        super(id, content, viewableClass.getAllChildren(), isFinal, applicationVersion, creator);
        this.viewableClass = viewableClass;
    }

    /**
     *
     * @return the class of which this viewable is a viewable-instance
     */
    public AbstractViewableClass getViewableClass()
    {
        return viewableClass;
    }

    /**
     *
     * @return the content of this viewable instance, it is probably a template of some sort
     */
    public String getContent(){
        return this.template;
    }

    @Override
    public Map<String, String> toHash()
    {
        Map<String, String> hash = super.toHash();
        hash.put(DatabaseConstants.VIEWABLE_CLASS, this.getViewableClass().getName());
        return hash;
    }
}

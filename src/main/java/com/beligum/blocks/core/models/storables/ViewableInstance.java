package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.classes.AbstractViewableClass;

import java.util.Map;

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
     *
     * @return the class of which this viewable is a viewable-instance
     */
    public AbstractViewableClass getViewableClass()
    {
        return viewableClass;
    }

    @Override
    public Map<String, String> toHash()
    {
        Map<String, String> hash = super.toHash();
        hash.put(DatabaseConstants.VIEWABLE_CLASS, this.getViewableClass().getName());
        return hash;
    }
}

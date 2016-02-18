package com.beligum.blocks.rdf.schema;

import com.beligum.base.models.ifaces.BasicModel;
import com.beligum.base.models.ifaces.Subject;

import java.time.LocalDateTime;

/**
 * Created by bram on 2/18/16.
 */
public class Page implements BasicModel
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public Long getId()
    {
        return null;
    }
    @Override
    public LocalDateTime getCreatedAt()
    {
        return null;
    }
    @Override
    public void setCreatedAt(LocalDateTime created_at)
    {

    }
    @Override
    public LocalDateTime getUpdatedAt()
    {
        return null;
    }
    @Override
    public void setUpdatedAt(LocalDateTime updatedAt)
    {

    }
    @Override
    public Subject getCreatedBy()
    {
        return null;
    }
    @Override
    public void setCreatedBy(Subject createdBy)
    {

    }
    @Override
    public Subject getUpdatedBy()
    {
        return null;
    }
    @Override
    public void setUpdatedBy(Subject updatedBy)
    {

    }
    @Override
    public boolean isDeleted()
    {
        return false;
    }
    @Override
    public void setDeleted(boolean deleted)
    {

    }
    @Override
    public boolean getIsNew()
    {
        return false;
    }
    @Override
    public String getResourceUriClassName()
    {
        return "page";
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.core.mongo;

import com.beligum.blocks.core.urlmapping.UrlDispatcher;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.identifiers.MongoID;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by wouter on 23/03/15.
 */
public class MongoUrlDispatcher extends UrlDispatcher
{
    public static final String dispatcherID = "urldispatcher";

    @JsonProperty("_id")
    private String dbID;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;


    @Override
    public BlockId getId() {
        return new MongoID(this.dispatcherID);
    }

    @Override
    public void setId(BlockId id) {
        this.dbID = id.toString();
    }

    @Override
    public String getCreatedBy()
    {
        return createdBy;
    }
    @Override
    public void setCreatedBy(String createdBy)
    {
        this.createdBy = createdBy;
    }
    @Override
    public String getCreatedAt()
    {
        return createdAt;
    }
    @Override
    public void setCreatedAt(String createdAt)
    {
        this.createdAt = createdAt;
    }
    @Override
    public String getUpdatedBy()
    {
        return updatedBy;
    }
    @Override
    public void setUpdatedBy(String updatedBy)
    {
        this.updatedBy = updatedBy;
    }
    public String getUpdatedAt()
    {
        return updatedAt;
    }
    public void setUpdatedAt(String updatedAt)
    {
        this.updatedAt = updatedAt;
    }
}

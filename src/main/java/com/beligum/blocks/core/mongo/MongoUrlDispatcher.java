package com.beligum.blocks.core.mongo;

import com.beligum.blocks.core.URLMapping.simple.UrlDispatcher;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.identifiers.MongoID;
import com.beligum.blocks.core.models.nosql.BlocksStorable;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by wouter on 23/03/15.
 */
public class MongoUrlDispatcher extends UrlDispatcher
{
    @JsonProperty("_id")
    private String dbID;


    @Override
    public BlockId getId() {
        return new MongoID(this.dbID);
    }

    @Override
    public void setId(BlockId id) {
        this.dbID = id.toString();
    }

}

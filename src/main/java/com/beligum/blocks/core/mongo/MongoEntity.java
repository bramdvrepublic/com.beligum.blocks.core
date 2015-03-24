package com.beligum.blocks.core.mongo;

import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.identifiers.MongoID;
import com.beligum.blocks.core.models.nosql.Entity;
import com.beligum.core.framework.models.ifaces.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import sun.jvm.hotspot.opto.Block;

/**
 * Created by wouter on 23/03/15.
 */
public class MongoEntity extends Entity
{
    @JsonProperty("_id")
    protected String realDBId;

    public MongoEntity() {
        super();
    }

    public MongoEntity(String name) {
        super(name);
    }

    public MongoEntity(String name, String language) {
        super(name, language);
    }

    public MongoID getIdForString(String s) {
        return new MongoID(s);
    }

    @JsonIgnore
    @Override
    public BlockId getId() {
        MongoID retVal = null;
        if (this.realDBId != null) {
            return new MongoID(this.realDBId);
        }
        return retVal;
    }

    @Override
    public void setId(BlockId id) {
        if (id != null) {
            this.realDBId = id.toString();
        } else {
            this.realDBId = null;
        }
    }

//    public String getJsonHash() {
//        return null;
//    }
//
//    public String getTypeName() {
//        return this.getClass().toString();
//    }
//
//    public String toJson() {
//        return null;
//    }
//

}

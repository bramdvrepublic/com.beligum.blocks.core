package com.beligum.blocks.core.mongo;

import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.identifiers.MongoID;
import com.beligum.blocks.core.models.Blueprint;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jsoup.nodes.Element;

/**
 * Created by wouter on 23/03/15.
 */
public class MongoBlueprint extends Blueprint
{
    public MongoBlueprint() {

    }

    public MongoBlueprint(Element element, String language) throws ParseException
    {
        super(element, language);
    }

    @JsonProperty("_id")
    protected String realDBId;

    public MongoID getIdForString(String s) {
        MongoID retVal = null;
        if (retVal != null) {
            retVal = new MongoID(s);
        }
        return retVal;
    }

    @JsonIgnore
    @Override
    public MongoID getId() {
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
}

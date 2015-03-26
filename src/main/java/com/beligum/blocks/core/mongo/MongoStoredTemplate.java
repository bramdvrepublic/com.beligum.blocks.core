package com.beligum.blocks.core.mongo;

import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.identifiers.MongoID;
import com.beligum.blocks.core.models.StoredTemplate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jsoup.nodes.Element;

import javax.persistence.Id;
import java.net.URL;

/**
 * Created by wouter on 20/03/15.
 */

public class MongoStoredTemplate extends StoredTemplate {

    // This constructor is needed to unmarshall json from db
    public MongoStoredTemplate() {
    }

    public MongoStoredTemplate(Element element, String language) throws ParseException
    {
        super(element, language);
    }

    public MongoStoredTemplate(Element element, URL url) throws ParseException
    {
        super(element, url);
    }

    @JsonProperty("_id")
    protected String realDBId;



    @JsonIgnore
    @Override
    @Id
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
}



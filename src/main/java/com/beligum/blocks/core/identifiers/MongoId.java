package com.beligum.blocks.core.identifiers;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.IDException;
import org.bson.types.ObjectId;

import java.net.URL;

/**
 * Created by wouter on 17/03/15.
 */
public class MongoID implements BlockId
{
    private String id;

    public MongoID(String s) {
        this.id = s;
    }

    public String toString() {
        return this.id;
    }

}

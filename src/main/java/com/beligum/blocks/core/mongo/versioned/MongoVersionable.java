package com.beligum.blocks.core.mongo.versioned;

import com.beligum.blocks.core.models.interfaces.BlocksVersionedStorable;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by wouter on 27/03/15.
 */
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="class")
public interface MongoVersionable extends BlocksVersionedStorable
{
}

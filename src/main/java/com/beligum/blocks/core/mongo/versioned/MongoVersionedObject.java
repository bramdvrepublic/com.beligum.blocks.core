package com.beligum.blocks.core.mongo.versioned;

import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.models.nosql.interfaces.BlocksVersionedStorable;

/**
 * Created by wouter on 25/03/15.
 */
public class MongoVersionedObject
{
    private BlockId id;
    private Long documentVersion;
    private String applicationVersion;
    private String language;
    private BlocksVersionedStorable versionedStorable;

    public MongoVersionedObject(BlocksVersionedStorable storable) {
        this.versionedStorable = storable;
    }

    public BlockId getId()
    {
        return id;
    }

    public void setId(BlockId id)
    {
        this.id = id;
    }


    public Long getDocumentVersion()
    {
        return documentVersion;
    }

    public void setDocumentVersion(Long documentVersion)
    {
        this.documentVersion = documentVersion;
    }

    public BlocksVersionedStorable getVersionedStorable() {
        return this.versionedStorable;
    }

    public String getApplicationVersion()
    {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion)
    {
        this.applicationVersion = applicationVersion;
    }


    public String getLanguage()
    {
        return language;
    }
    public void setLanguage(String language)
    {
        this.language = language;
    }
}

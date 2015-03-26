package com.beligum.blocks.core.mongo.versioned;

import com.beligum.blocks.core.identifiers.MongoID;
import com.beligum.blocks.core.models.interfaces.BlocksVersionedStorable;

/**
 * Created by wouter on 25/03/15.
 */
public class MongoVersionedObject
{
    private MongoID id;
    private MongoID versionedId;
    private Long documentVersion;
    private String applicationVersion;
    private String language;
    private BlocksVersionedStorable versionedStorable;

    public MongoVersionedObject(BlocksVersionedStorable storable) {
        this.versionedStorable = storable;
        this.documentVersion = storable.getDocumentVersion();
        this.applicationVersion = storable.getApplicationVersion();
        this.language = storable.getLanguage();
        this.versionedId = (MongoID)storable.getId();
    }

    public MongoID getId()
    {
        return id;
    }

    public void setId(MongoID id)
    {
        this.id = id;
    }

    public MongoID getVersionedId()
    {
        return versionedId;
    }
    public void setVersionedId(MongoID versionedId)
    {
        this.versionedId = versionedId;
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

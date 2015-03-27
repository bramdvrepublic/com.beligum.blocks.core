package com.beligum.blocks.core.mongo.versioned;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.identifiers.MongoID;
import com.beligum.blocks.core.models.interfaces.BlocksVersionedStorable;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by wouter on 25/03/15.
 */
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="class")
public class MongoVersionedObject
{
    private MongoID id;
    private String versionedId;
    private Long documentVersion;
    private String applicationVersion;
    private String language;
    private MongoVersionable versionedStorable;

    public MongoVersionedObject() {

    }

    public MongoVersionedObject(MongoVersionable storable) {
        this.versionedStorable = storable;
        this.documentVersion = storable.getDocumentVersion();
        this.applicationVersion = storable.getApplicationVersion();
        this.language = storable.getLanguage();
        this.versionedId = storable.getId().toString();
    }

    public MongoID getId()
    {
        return id;
    }

    public void setId(MongoID id)
    {
        this.id = id;
    }

    public String getVersionedId()
    {
        return versionedId;
    }
    public void setVersionedId(String versionedId)
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

    public MongoVersionable getVersionedStorable() {
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

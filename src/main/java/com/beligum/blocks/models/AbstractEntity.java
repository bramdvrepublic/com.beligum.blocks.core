package com.beligum.blocks.models;

import com.beligum.blocks.models.interfaces.BlocksVersionedStorable;
import com.beligum.blocks.models.interfaces.NamedProperty;

/**
 * Created by wouter on 25/03/15.
 */
public abstract class AbstractEntity extends EntityField implements BlocksVersionedStorable, NamedProperty
{

    private Long documentVersion;
    private String applicationVersion;
    private String createdBy;
    private String updatedBy;
    private String createdAt;
    private String updatedAt;
    private String language;

    public AbstractEntity() {

    }

    public AbstractEntity(String name, String value, String language) {
        super(name, value);
        this.language = language;
    }


    @Override
    public Long getDocumentVersion()
    {
        return this.documentVersion;
    }

    @Override
    public void setDocumentVersion(Long documentVersion)
    {
        this.documentVersion = documentVersion;
    }

    @Override
    public String getApplicationVersion()
    {
        return this.applicationVersion;
    }

    @Override
    public void setApplicationVersion(String applicationVersion)
    {
        this.applicationVersion = applicationVersion;
    }

    /**
     * @return the creator of this storable
     */
    @Override
    public String getCreatedBy()
    {
        return this.createdBy;
    }

    @Override
    public void setCreatedBy(String created_by)
    {
        this.createdBy = created_by;
    }
    /**
     * @return the updater of this storable
     */
    @Override
    public String getUpdatedBy()
    {
        return this.updatedBy;
    }
    @Override
    public void setUpdatedBy(String updated_by)
    {
        this.updatedBy = updated_by;
    }
    /**
     * @return the moment of creation of this storable
     */
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
    /**
     * @return the moment of last update of this storable
     */
    @Override
    public String getUpdatedAt()
    {
        return updatedAt;
    }
    @Override
    public void setUpdatedAt(String updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    @Override
    public String getLanguage()
    {
        return language;
    }
    @Override
    public void setLanguage(String language)
    {
        this.language = language;
    }

}

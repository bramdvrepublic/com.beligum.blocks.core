package com.beligum.blocks.models;

/**
 * Created by wouter on 17/03/15.
 */
public interface Storable
{
    public String getLanguage();


    public void setLanguage(String language);


    public Long getDocumentVersion();


    public void setDocumentVersion(Long documentVersion);


    public String getApplicationVersion();


    public void setApplicationVersion(String applicationVersion);

    public String getCreatedBy();


    public void setCreatedBy(String created_by);


    public String getUpdatedBy();

    public void setUpdatedBy(String updated_by);


    public String getCreatedAt();

    public void setCreatedAt(String createdAt);


    public String getUpdatedAt();

    public void setUpdatedAt(String updatedAt);
}

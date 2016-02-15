package com.beligum.blocks.models.interfaces;

/**
 * Created by wouter on 19/06/15.
 */
public interface DocumentInfo
{
    public void setCreatedAt(java.time.LocalDateTime date);

    public java.time.LocalDateTime getCreatedAt();

    public void setCreatedBy(String user);

    public String getCreatedBy();

    public void setUpdatedAt(java.time.LocalDateTime date);

    public java.time.LocalDateTime getUpdatedAt();

    public void setUpdatedBy(String user);

    public String getUpdatedBy();
}

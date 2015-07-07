package com.beligum.blocks.models.interfaces;

import org.joda.time.LocalDateTime;

/**
 * Created by wouter on 19/06/15.
 */
public interface DocumentInfo
{
    public void setCreatedAt(LocalDateTime date);

    public LocalDateTime getCreatedAt();

    public void setCreatedBy(String user);

    public String getCreatedBy();

    public void setUpdatedAt(LocalDateTime date);

    public LocalDateTime getUpdatedAt();

    public void setUpdatedBy(String user);

    public String getUpdatedBy();
}

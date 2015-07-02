package com.beligum.blocks.resources.interfaces;

import org.joda.time.LocalDateTime;

import java.util.Calendar;
import java.util.Date;

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

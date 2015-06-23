package com.beligum.blocks.resources.interfaces;

import java.util.Calendar;

/**
 * Created by wouter on 19/06/15.
 */
public interface DocumentInfo
{
    public void setCreatedAt(Calendar date);

    public Calendar getCreatedAt();

    public void setCreatedBy(String user);

    public String getCreatedBy();

    public void setUpdatedAt(Calendar date);

    public Calendar getUpdatedAt();

    public void setUpdatedBy(String user);

    public String getUpdatedBy();
}

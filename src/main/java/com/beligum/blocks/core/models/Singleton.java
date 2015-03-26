package com.beligum.blocks.core.models;

/**
 * Created by wouter on 26/03/15.
 */
public class Singleton extends StoredTemplate
{
    private String singletonName;

    public String getSingletonName()
    {
        return singletonName;
    }
    public void setSingletonName(String singletonName)
    {
        this.singletonName = singletonName;
    }
}

package com.beligum.blocks.core.models;

/**
 * Created by bas on 07.10.14.
 * Class representing objects having a unique identifier
 */
public class IdentifiableObject
{
    //string representing the unique id of this object
    protected String uid;

    public IdentifiableObject(String uid)
    {
        this.uid = uid;
    }

    public String getUid()
    {
        return uid;
    }
    public void setUid(String uid)
    {
        this.uid = uid;
    }
}

package com.beligum.blocks.core.models;

import com.beligum.blocks.core.models.AbstractIdentifiableElement;

/**
 * Created by bas on 01.10.14.
 * Class representing the basic layout-element in a html-page
 */
public class Row extends AbstractIdentifiableElement
{
    //the name of the set of all row-elements in the database
    private final String DB_SET_NAME = "rows";

    public Row(String content, String uid){
        super(content, uid);
    }

    @Override
    public String getDBSetName()
    {
        return DB_SET_NAME;
    }
}

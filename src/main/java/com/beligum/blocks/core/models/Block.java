package com.beligum.blocks.core.models;

/**
 * Created by bas on 01.10.14.
 * Class representing the basic content-element in a html-page
 */
public class Block extends AbstractIdentifiableElement
{
    //the name of the set of all block-elements in the database
    private final String DB_SET_NAME = "blocks";

    public Block(String content, String uid){
        super(content, uid);
    }

    @Override
    public String getDBSetName()
    {
        return DB_SET_NAME;
    }
}

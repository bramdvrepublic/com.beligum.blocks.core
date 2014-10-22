package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.AbstractElement;
import com.beligum.blocks.core.models.ifaces.StorableElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bas on 01.10.14.
 * Class representing the basic content-element in a html-page
 */
public class Block extends AbstractElement implements StorableElement
{
    //the name of the set of all block-elements in the database
    private final String DB_SET_NAME = "blocks";


    /**
     * Constructor
     * @param content the (velocity) content of this block
     * @param id the url to this row (is of the form "[site]/[pageName]#[blockId]")
     */
    public Block(RedisID id, String content)
    {
        super(id, content);
    }



    //_______________IMPLEMENTATION OF STORABLE_ELEMENT____________________//
    @Override
    public long getVersion()
    {
        return this.getId().getVersion();
    }
    @Override
    public RedisID getId()
    {
        return (RedisID) super.getId();
    }
    @Override
    public String getUnversionedId(){
        return this.getId().getUnversionedId();
    }
    @Override
    public String getVersionedId(){
        return this.getId().getVersionedId();
    }
    @Override
    public Map<String, String> toHash(){
        Map<String, String> hash = new HashMap<>();
        hash.put("velocityContent", this.getContent());
        //TODO BAS: this version should be fetched from pom.xml and added to the block.java as a field
        hash.put("appVersion", "test");
        //TODO: logged in user should be added here and added to the block.java as a field
        hash.put("creator", "me");
        hash.put("type", this.getClass().getSimpleName());
        return hash;
    }
    @Override
    public String getTemplateVariableName()
    {
        return this.getId().toURI().getFragment();
    }

    //__________IMPLEMENTATION OF ABSTRACT METHODS OF ABSTRACTELEMENT________//
    @Override
    public String getDBSetName()
    {
        return DB_SET_NAME;
    }
}

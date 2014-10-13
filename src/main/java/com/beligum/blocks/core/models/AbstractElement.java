package com.beligum.blocks.core.models;

import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.identifiers.RedisID;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by bas on 01.10.14.
 * An abstract node in a row- and block tree
 */
public abstract class AbstractElement extends IdentifiableObject
{
    //string representing the (html- or velocity-)content of this element
    protected String content;
    //long representing the version of this element
    protected long version;

    /**
     * Constructor
      * @param content the (velocity) content of this element
     * @param id the id of this element
     */
    public AbstractElement(ID id, String content)
    {
        super(id);
        this.content = content;
    }

    public String getContent()
    {
        return content;
    }
    public void setContent(String content)
    {
        this.content = content;
    }



    /**
     * @return the name of the set of all these elements in the database
     */
    abstract public String getDBSetName();

}

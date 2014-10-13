package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.AbstractElement;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by bas on 01.10.14.
 * Class representing the basic content-element in a html-page
 */
public class Block extends AbstractElement
{
    //the name of the set of all block-elements in the database
    private final String DB_SET_NAME = "blocks";


    /**
     * Constructor
     * @param content the (velocity) content of this block
     * @param id the url to this row (is of the form "<site>/<pageName>#<blockId>")
     */
    public Block(RedisID id, String content)
    {
        super(id, content);
    }

    @Override
    public String getDBSetName()
    {
        return DB_SET_NAME;
    }
}

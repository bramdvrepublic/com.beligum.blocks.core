package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.AbstractElement;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by bas on 01.10.14.
 * Class representing the basic layout-element in a html-page
 */
public class Row extends AbstractElement
{
    //the name of the set of all row-elements in the database
    private final String DB_SET_NAME = "rows";

    /**
     * Constructor
     * @param content the (velocity) content of this row
     * @param id the id to this row (is of the form "<site>/<pageName>#<rowId>")
     */
    public Row(RedisID id, String content)
    {
        super(id, content);
    }

    @Override
    public String getDBSetName()
    {
        return DB_SET_NAME;
    }
}

package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Page;
import com.beligum.blocks.core.models.storables.Row;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bas on 09.10.14.
 * Wrapper class for talking to the redis-db
 */
public class Redis
{
    //TODO BAS: implement this Redis-class!
    /**
     * Save the page to db, together with it's rows and blocks
     * @param page
     */
    public void savePage(Page page){

    }
    /**
     * Save the row to db
     * @param row
     */
    public void saveRow(Row row){
        //TODO BAS: remove this DEBUG feature
        System.out.println("\n (" + row.getId().toString() + ", " + row.getContent() + ") \n");
    }
    /**
     * Save the block to db
     * @param block
     */
    public void saveBlock(Block block)
    {

    }

    /**
     * Get a page and all of it's blocks an rows from the db
     * @param uid the id of the page to fetch
     * @return page from db
     */
    public Page getPage(URI uid) throws URISyntaxException
    {
        return new Page(null, null);
    }
    /**
     * Get a row from the db
     * @param id the id of the row to fetch
     * @return
     */
    public Row getRow(RedisID id) throws URISyntaxException
    {
        return new Row(id, "");
    }
    /**
     * Get a block from the db
     * @param id the id of the block to fetch
     * @return
     */
    public Block getBlock(RedisID id) throws URISyntaxException
    {
        return new Block(id, "");
    }
}

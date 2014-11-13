package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.caching.BlockClassCache;
import com.beligum.blocks.core.caching.EntityClassCache;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.classes.BlockClass;
import com.beligum.blocks.core.models.classes.EntityClass;

import java.util.Set;

/**
 * Created by bas on 05.11.14.
 */
public class Block extends ViewableInstance
{
    /**the content of this block*/
    private String content;

    /**
     * Constructor
     *
     * @param id       the id to this block (is of the form "[site]/[pageName]#[blockId]")
     * @param content the content of this block
     * @param blockClassName the class of which this block is a block-instance
     * @param isFinal  boolean whether or not the template of this element can be changed by the client
     * @throws CacheException if something went wrong while fetching the blockclass for the application cache
     */
    public Block(RedisID id, String content, String blockClassName, boolean isFinal) throws CacheException
    {
        super(id, BlockClassCache.getInstance().get(blockClassName), isFinal);
        this.content = content;
    }

    /**
     * @param id                 the id to this block (is of the form "[site]/[pageName]#[blockId]")
     * @param content the content of this block
     * @param blockClassName the class of which this block is a block-instance
     * @param isFinal            boolean whether or not the template of this element can be changed by the client
     * @param applicationVersion the version of the application this block was saved under
     * @param creator            the creator of this block
     */
    public Block(RedisID id, String content, String blockClassName, boolean isFinal, String applicationVersion, String creator) throws CacheException
    {
        super(id, BlockClassCache.getInstance().get(blockClassName), isFinal, applicationVersion, creator);
        this.content = content;
    }

    /**
     * Constructor for a new block-instance taking elements fetched from db and a blockclass (fetched from application cache).
     * The rows and blocks are added to this block in the following order:
     * 1. final elements of block-class, 2. blocks and rows from database specified in the set, 3. non-final elements of block-class, whose element-id's are not yet present in the block
     * @param id the id of this block
     * @param content the content of this block
     * @param directChildrenFromDB the direct children of the block
     * @param blockClass the block-class this block is an instance of
     * @param applicationVersion the version of the app this block was saved under
     * @param creator the creator of this block
     *
     */
    public Block(RedisID id, String content, Set<Row> directChildrenFromDB, BlockClass blockClass, String applicationVersion, String creator)
    {
        super(id, blockClass, true, applicationVersion, creator);
        this.content = content;
        this.addDirectChildren(blockClass.getAllFinalElements().values());
        this.addDirectChildren(directChildrenFromDB);
        this.addDirectChildren(blockClass.getAllNonFinalElements());
    }
}

package com.beligum.blocks.core.models.storables;

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

    /**
     * Constructor
     *
     * @param id       the id to this block (is of the form "[site]/[pageName]#[blockId]")
     * @param blockClass the class of which this block is a block-instance
     * @param isFinal  boolean whether or not the template of this element can be changed by the client
     */
    public Block(RedisID id, BlockClass blockClass, boolean isFinal)
    {
        super(id, blockClass, isFinal);
    }

    /**
     * @param id                 the id to this block (is of the form "[site]/[pageName]#[blockId]")
     * @param blockClass the class of which this block is a block-instance
     * @param isFinal            boolean whether or not the template of this element can be changed by the client
     * @param applicationVersion the version of the application this block was saved under
     * @param creator            the creator of this block
     */
    public Block(RedisID id, BlockClass blockClass, boolean isFinal, String applicationVersion, String creator)
    {
        super(id, blockClass, isFinal, applicationVersion, creator);
    }

    /**
     * Constructor for a new block-instance taking children and a blockclass. The rows and blocks of the blockClass are NOT copied to this block.
     * @param id the id of this block
     * @param directChildren the direct children for this block
     * @param blockClassName the name of the block-class this block is an instance of
     * @throws com.beligum.blocks.core.exceptions.CacheException when the block-class cannot be found in the application-cache
     */
    public Block(RedisID id, Set<Row> directChildren, String blockClassName) throws CacheException
    {
        //the template of a block is always the template of it's block-class; a block cannot be altered by the client, so it always is final
        super(id, EntityClassCache.getInstance().get(blockClassName), true);
        this.addDirectChildren(directChildren);
    }

    /**
     * Constructor for a new block-instance taking elements fetched from db and a blockclass (fetched from application cache).
     * The rows and blocks are added to this block in the following order:
     * 1. final elements of block-class, 2. blocks and rows from database specified in the set, 3. non-final elements of block-class, whose element-id's are not yet present in the block
     * @param id the id of this block
     * @param directChildrenFromDB the direct children of the block
     * @param blockEntityClass the block-class this block is an instance of
     * @param applicationVersion the version of the app this block was saved under
     * @param creator the creator of this block
     *
     */
    public Block(RedisID id, Set<Row> directChildrenFromDB, EntityClass blockEntityClass, String applicationVersion, String creator)
    {
        super(id, blockEntityClass, true, applicationVersion, creator);
        this.addDirectChildren(blockEntityClass.getAllFinalElements().values());
        this.addDirectChildren(directChildrenFromDB);
        this.addDirectChildren(blockEntityClass.getAllNonFinalElements());
    }
}

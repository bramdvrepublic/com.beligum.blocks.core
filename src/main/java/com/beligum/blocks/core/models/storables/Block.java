//package com.beligum.blocks.core.models.storables;
//
//import com.beligum.blocks.core.caching.BlockClassCache;
//import com.beligum.blocks.core.config.DatabaseConstants;
//import com.beligum.blocks.core.exceptions.CacheException;
//import com.beligum.blocks.core.identifiers.RedisID;
//import com.beligum.blocks.core.models.classes.BlockClass;
//
//import java.util.Map;
//import java.util.Set;
//
///**
// * Created by bas on 05.11.14.
// */
//public class Block extends ViewableInstance
//{
//    /**
//     * Constructor
//     *
//     * @param id       the id to this block (is of the form "[site]/[pageName]#[blockId]")
//     * @param content the content of this block
//     * @param blockClassName the class of which this block is a block-instance
//     * @param isFinal  boolean whether or not the template of this element can be changed by the client
//     * @throws CacheException if something went wrong while fetching the blockclass for the application cache
//     */
//    public Block(RedisID id, String content, String blockClassName, boolean isFinal) throws CacheException
//    {
//        super(id, content, BlockClassCache.getInstance().get(blockClassName), isFinal);
//    }
//
//    /**
//     * @param id                 the id to this block (is of the form "[site]/[pageName]#[blockId]")
//     * @param content the content of this block
//     * @param blockClassName the class of which this block is a block-instance
//     * @param isFinal            boolean whether or not the template of this element can be changed by the client
//     * @param applicationVersion the version of the application this block was saved under
//     * @param creator            the creator of this block
//     */
//    public Block(RedisID id, String content, String blockClassName, boolean isFinal, String applicationVersion, String creator) throws CacheException
//    {
//        super(id, content, BlockClassCache.getInstance().get(blockClassName), isFinal, applicationVersion, creator);
//    }
//}

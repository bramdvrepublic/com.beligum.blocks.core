package com.beligum.blocks.core.base;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.AbstractBlockDatabase;
import com.beligum.blocks.core.dbs.BlocksTemplateCache;
import com.beligum.blocks.core.dbs.BlocksUrlDispatcher;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.mongo.BlocksFactory;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.cache.CacheKey;

/**
 * Created by wouter on 26/03/15.
 */
public class Blocks
{
    private enum BlocksConfigCacheKey implements CacheKey
    {
        BLOCKS_CONFIG_KEY,
        BLOCKS_DB_KEY,
        BLOCKS_TEMPLATE_CACHE_KEY,
        BLOCKS_URL_DISPATCHER_KEY,
        BLOCKS_FACTORY_KEY
    }

    public static AbstractBlockDatabase database() {
        return (AbstractBlockDatabase) R.cacheManager().getApplicationCache().get(BlocksConfigCacheKey.BLOCKS_DB_KEY);
    }

    public static void setDatabase(AbstractBlockDatabase database)
    {
        R.cacheManager().getApplicationCache().put(BlocksConfigCacheKey.BLOCKS_DB_KEY, database);
    }

    public static BlocksTemplateCache templateCache() {
        return (BlocksTemplateCache)R.cacheManager().getApplicationCache().get(BlocksConfigCacheKey.BLOCKS_TEMPLATE_CACHE_KEY);
    }

    public static void setTemplateCache(BlocksTemplateCache templateCache) throws CacheException
    {
        if (Blocks.templateCache() == null) {
            R.cacheManager().getApplicationCache().put(BlocksConfigCacheKey.BLOCKS_TEMPLATE_CACHE_KEY, templateCache);
        }
        Blocks.templateCache().reset();
    }

    public static BlocksConfig config() {
        if (!R.cacheManager().getApplicationCache().containsKey(BlocksConfigCacheKey.BLOCKS_CONFIG_KEY)) {
            R.cacheManager().getApplicationCache().put(BlocksConfigCacheKey.BLOCKS_CONFIG_KEY, new BlocksConfig());
        }
        return (BlocksConfig)R.cacheManager().getApplicationCache().get(BlocksConfigCacheKey.BLOCKS_CONFIG_KEY);
    }


    public static BlocksUrlDispatcher urlDispatcher() {
        if (!R.cacheManager().getApplicationCache().containsKey(BlocksConfigCacheKey.BLOCKS_URL_DISPATCHER_KEY)) {
            BlocksUrlDispatcher retVal = Blocks.database().fetchUrlDispatcher();
            if (retVal == null) retVal = Blocks.factory().createUrlDispatcher();
            R.cacheManager().getApplicationCache().put(BlocksConfigCacheKey.BLOCKS_URL_DISPATCHER_KEY, retVal);
        }
        return (BlocksUrlDispatcher)R.cacheManager().getApplicationCache().get(BlocksConfigCacheKey.BLOCKS_URL_DISPATCHER_KEY);
    }

    public static BlocksFactory factory() {
        return (BlocksFactory)R.cacheManager().getApplicationCache().get(BlocksConfigCacheKey.BLOCKS_FACTORY_KEY);
    }

    public static void setFactory(BlocksFactory factory)
    {
        R.cacheManager().getApplicationCache().put(BlocksConfigCacheKey.BLOCKS_FACTORY_KEY, factory);
    }





}

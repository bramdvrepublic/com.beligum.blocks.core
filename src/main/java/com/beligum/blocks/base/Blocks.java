package com.beligum.blocks.base;

import com.beligum.base.cache.CacheKey;
import com.beligum.base.server.R;
import com.beligum.blocks.caching.BlocksTemplateCache;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.dynamic.DynamicBlockHandler;
import com.beligum.blocks.exceptions.CacheException;
import com.beligum.blocks.models.factory.BlocksFactory;

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
        BLOCKS_FACTORY_KEY,
        BLOCKS_HANDLER_KEY,
        BLOCKS_RENDERER_KEY,
        BlOCKS_RDF_FACTORY
    }


    public static BlocksTemplateCache templateCache()
    {
        return (BlocksTemplateCache) R.cacheManager().getApplicationCache().get(BlocksConfigCacheKey.BLOCKS_TEMPLATE_CACHE_KEY);
    }

    public static void setTemplateCache(BlocksTemplateCache templateCache) throws CacheException
    {
        if (Blocks.templateCache() == null) {
            R.cacheManager().getApplicationCache().put(BlocksConfigCacheKey.BLOCKS_TEMPLATE_CACHE_KEY, templateCache);
        }
        Blocks.templateCache().reset();
    }

    public static BlocksConfig config()
    {
        if (!R.cacheManager().getApplicationCache().containsKey(BlocksConfigCacheKey.BLOCKS_CONFIG_KEY)) {
            R.cacheManager().getApplicationCache().put(BlocksConfigCacheKey.BLOCKS_CONFIG_KEY, new BlocksConfig());
        }
        return (BlocksConfig) R.cacheManager().getApplicationCache().get(BlocksConfigCacheKey.BLOCKS_CONFIG_KEY);
    }

    public static BlocksFactory factory()
    {
        return (BlocksFactory) R.cacheManager().getApplicationCache().get(BlocksConfigCacheKey.BLOCKS_FACTORY_KEY);
    }

    public static void setFactory(BlocksFactory factory)
    {
        R.cacheManager().getApplicationCache().put(BlocksConfigCacheKey.BLOCKS_FACTORY_KEY, factory);
    }

    public static DynamicBlockHandler blockHandler()
    {
        if (!R.cacheManager().getApplicationCache().containsKey(BlocksConfigCacheKey.BLOCKS_HANDLER_KEY)) {
            R.cacheManager().getApplicationCache().put(BlocksConfigCacheKey.BLOCKS_HANDLER_KEY, new DynamicBlockHandler());
        }
        return (DynamicBlockHandler) R.cacheManager().getApplicationCache().get(BlocksConfigCacheKey.BLOCKS_HANDLER_KEY);
    }

}

package com.beligum.blocks.core.caching;

import com.beligum.core.framework.cache.CacheKey;

/**
 * Created by bas on 07.10.14.
 */
public enum CacheKeys implements CacheKey
{
    //points to hashmap of entity-classes
    ENTITY_CLASSES,
    //points to hashmap of block-classes
    BLOCK_CLASSES,

    PAGETEMPLATES
}

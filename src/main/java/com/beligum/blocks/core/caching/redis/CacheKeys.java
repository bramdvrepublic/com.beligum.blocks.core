package com.beligum.blocks.core.caching.redis;

import com.beligum.core.framework.cache.CacheKey;

/**
 * Created by bas on 07.10.14.
 */
public enum CacheKeys implements CacheKey
{
    //points to hashmap of entity-classes
    BLUEPRINTS,
    //point to hashmap of page-templates
    PAGE_TEMPLATES
}

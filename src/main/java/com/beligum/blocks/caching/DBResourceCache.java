package com.beligum.blocks.caching;

import com.beligum.base.cache.Cache;
import com.beligum.base.cache.HashMapCache;

/**
 * Created by wouter on 29/06/15.
 */
public class DBResourceCache extends HashMapCache<String, Object> implements Cache<String, Object>
{
    public DBResourceCache(String name)
    {
        super(name);
    }

}

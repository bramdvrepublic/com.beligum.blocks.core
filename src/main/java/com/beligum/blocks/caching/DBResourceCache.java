package com.beligum.blocks.caching;

import com.beligum.base.cache.Cache;
import com.beligum.base.cache.HashMapCache;
import com.beligum.blocks.resources.interfaces.Resource;
import net.sf.ehcache.management.CacheMBean;
import org.apache.shiro.crypto.hash.Hash;

import java.util.HashMap;
import java.util.Map;

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

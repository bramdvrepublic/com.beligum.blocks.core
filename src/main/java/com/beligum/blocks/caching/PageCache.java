package com.beligum.blocks.caching;

import com.beligum.base.cache.EhCacheAdaptor;
import com.beligum.base.server.R;

import java.util.UUID;

/**
 * Created by wouter on 24/07/15.
 */
public class PageCache
{

    // Page stays in cache for 1 hour since last visited default
    public int IDLE_TIME = 3600;

    private static PageCache instance;
    private EhCacheAdaptor<String> cache;

    private PageCache() {
        cache = new EhCacheAdaptor<String>(CacheKeys.PAGES.name() , 0, true, false, 0, IDLE_TIME);
    }

    public static PageCache instance() {
        if (PageCache.instance == null) {
            PageCache.instance = new PageCache();
        }
        return PageCache.instance;
    }

    public static boolean isEnabled() {
        return false;
    }

    public void flush() {
        this.cache.clear();
    }

    public void put(String url, String html) {
        this.cache.put(url, html);
    }

    public String get(String url) {
        return (String)cache.get(url);
    }

    public boolean hasUrl(String url) {
        boolean retVal = false;

        if (cache.containsKey(url)) {
            retVal = true;
        }

        return retVal;
    }
}

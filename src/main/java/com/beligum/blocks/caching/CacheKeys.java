package com.beligum.blocks.caching;

import com.beligum.base.cache.CacheKey;

/**
 * Created by bas on 07.10.14.
 */
public enum CacheKeys implements CacheKey
{
    //points to hashmap of entity-classes
    BLUEPRINTS,
    //point to hashmap of page-templates
    PAGE_TEMPLATES,
    //point to hashmap of cached web component templates
    WEBCOMPONENT_TEMPLATES,
    //the names of the entries (keys) in WEBCOMPONENT_TEMPLATES as a CSV string
    WEBCOMPONENT_TEMPLATES_CSV
}

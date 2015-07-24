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
    //point to hashmap of cached blocks tag templates (eg. <mot-waterput>)
    TAG_TEMPLATES,
    //the names of the entries (keys) in BLOCKS_TEMPLATES as a CSV string
    TAG_TEMPLATES_CSV,
    //the hashmap that contains mappings for templateTag -> templateControllerClass
    TAG_TEMPLATE_CONTROLLERS,
    //an list of Jericho tag types for all tag templates
    TAG_TEMPLATE_TYPES,
    //the hashmap that contains mappings for the parsed html files on disk (parsed to Velocity intermediates)
    BLOCKS_TEMPLATES,
    PAGES
}

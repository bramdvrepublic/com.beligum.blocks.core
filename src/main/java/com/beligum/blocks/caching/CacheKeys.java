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
    //the key that maps to the eh cache that contains the cached pages
    PAGES,
    //the key that maps to the mode in which we are now (set in HtmlRouter)
    BLOCKS_MODE,
    // the ES controller singleton
    ELASTIC_SEARCH_INSTANCE,
    //the ES node when in embedded mode
    ELASTIC_SEARCH_NODE,
    //application cache key to store the configuration of the HDFS page store
    HDFS_PAGESTORE_FS_CONFIG,
    //application cache key to store the configuration of the HDFS page read filesystem
    HDFS_PAGEVIEW_FS_CONFIG,
    //application cache key to store the HDFS page store
    HDFS_PAGE_STORE,
    //application cache key to store the Elastic Search page indexer
    ELASTIC_PAGE_INDEX,
    //application cache key to store the main page indexer
    MAIN_PAGE_INDEX,
    //application cache key to store the triplestore page indexer
    TRIPLESTORE_PAGE_INDEX,
    //application cache key to store the XAFileSystem
    XADISK_FILE_SYSTEM,
    //application cache key to store the TX manager
    TRANSACTION_MANAGER,
    //application cache key to store the triplestore engine
    REQUEST_TRANSACTION,
    //application cache key to store the triplestore engine
    TRIPLESTORE_ENGINE,
    //flash cache key that holds the temporarily selected new page tempalte
    NEW_PAGE_TEMPLATE_NAME,
    //Application cache key that hold the lucene index reader
    LUCENE_INDEX_READER,
    //Application cache key that hold the lucene index searcher
    LUCENE_INDEX_SEARCHER,
    //Application cache key that hold the Infinispan cache manager
    INFINISPAN_CACHE_MANAGER,
    //Application cache key that keeps a reference to the active indexers
    REGISTERED_INDEXERS,
    //Application cache key that keeps a map to the instantiated vocabularies
    RDF_VOCABULARIES,
    //Application cache key that maps vocabulary prefixes to full length vocabulary namespace URIs
    RDF_VOCABULARY_PREFIXES,
    //Application cache key that keeps a map to the discovered Rdf vocabularies-entries (classes and properties)
    RDF_VOCABULARY_ENTRIES
}

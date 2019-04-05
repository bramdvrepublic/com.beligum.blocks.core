/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.caching;

import com.beligum.base.cache.CacheKey;

/**
 * Created by bas on 07.10.14.
 */
public enum CacheKeys implements CacheKey
{
    //point to hashmap of cached blocks tag templates (eg. <mot-waterput>)
    TAG_TEMPLATES,
    //the hashmap that contains mappings for templateTag -> templateControllerClass
    TAG_TEMPLATE_CONTROLLERS,
    //application cache key that stores the single XAttr resolver factory
    BLOCKS_XATTR_RESOLVER_FACTORY,
    //application cache key to store the configuration of the HDFS page store
    HDFS_PAGESTORE_FS_CONFIG,
    //application cache key to store the HDFS page store instance
    HDFS_PAGESTORE_FS,
    //application cache key to store the configuration of the HDFS page read filesystem
    HDFS_PAGEVIEW_FS_CONFIG,
    //application cache key to store the HDFS page read instance
    HDFS_PAGEVIEW_FS,
    //application cache key to store the json page indexer
    JSON_PAGE_INDEXER,
    //application cache key to store the sparql page indexer
    SPARQL_PAGE_INDEXER,
    //application cache key to store the XAFileSystem
    XADISK_FILE_SYSTEM,
    //application cache key to store the TX manager
    TRANSACTION_MANAGER,
    //application cache key to store the request transaction
    REQUEST_TRANSACTION,
    //application cache key to store the triplestore engine
    TRIPLESTORE_ENGINE,
    //flash cache key that holds the temporarily selected new page template
    NEW_PAGE_TEMPLATE_NAME,
    //flash cache key that holds the temporarily selected new page URL to copy from
    NEW_PAGE_COPY_URL,
    //flash cache key that holds the temporarily boolean to see if we need to link the copy to the original
    NEW_PAGE_COPY_LINK,
    //flash cache key that holds the temporarily boolean to see if we need to immediately save a page after creating it
    NEW_PAGE_PERSISTENT,
    //flash cache key that holds a number of extra get-params that were passed in during new page template creation
    NEW_PAGE_EXTRA_PARAMS,
    //Application cache key that keeps a reference to the active indexers
    REGISTERED_INDEXERS,
    //Application cache key that keeps a cache of all active thread-bound transactions
    THREAD_TX_REGISTRY,
    //Application cache key that keeps a map to the instantiated ontologies
    RDF_RELEVANT_ONTOLOGIES,
    //Application cache key that maps ontology prefixes to full length vocabulary namespace URIs
    RDF_RELEVANT_ONTOLOGY_PREFIXES,
    //Application cache key that maps ontology classes to their instances
    RDF_RELEVANT_ONTOLOGY_CLASSES,
    //Request cache key that holds the resource action (create/update/...) for this request
    RESOURCE_ACTION
}

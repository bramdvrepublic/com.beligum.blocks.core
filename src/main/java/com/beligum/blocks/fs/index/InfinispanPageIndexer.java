package com.beligum.blocks.fs.index;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.index.ifaces.PageIndexer;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.TransactionMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * See this:
 * http://infinispan.org/tutorials/simple/query/
 * and
 * http://infinispan.org/docs/8.1.x/user_guide/user_guide.html#_persistence
 * <p/>
 * Created by bram on 1/26/16.
 */
public class InfinispanPageIndexer implements PageIndexer
{
    //-----CONSTANTS-----
    private static final String SUBFOLDR_STORE = "store";
    private static final String SUBFOLDR_INDEX = "index";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public InfinispanPageIndexer() throws IOException
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexConnection connect() throws IOException
    {
        Cache<String, PageIndexEntry> cache = this.getCacheManager().getCache();

        return new InfinispanPageIndexConnection(cache);
    }
//    @Override
//    public QueryBuilder getNewQueryBuilder() throws IOException
//    {
//        Cache<String, PageIndexEntry> cache = this.getCacheManager().getCache();
//        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(cache);
//        return searchManager.buildQueryBuilderForClass(INDEX_ENTRY_CLASS).get();
//    }
//    @Override
//    public CacheQuery executeQuery(Query query) throws IOException
//    {
//        Cache<String, PageIndexEntry> cache = this.getCacheManager().getCache();
//        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(cache);
//        return searchManager.getQuery(query, INDEX_ENTRY_CLASS);
//    }
    @Override
    public void shutdown()
    {
        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.INFINISPAN_CACHE_MANAGER)) {
            try {
                //TODO intested, don't know if this is ok
                this.getCacheManager().stop();
            }
            catch (Exception e) {
                Logger.error("Exception caught while closing Infinispan cache manager", e);
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private EmbeddedCacheManager getCacheManager() throws IOException
    {
        boolean skippedInit = true;

        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.INFINISPAN_CACHE_MANAGER)) {

            skippedInit = false;

            final java.nio.file.Path docDir = Settings.instance().getPageMainIndexFolder().toPath();
            if (!Files.exists(docDir)) {
                Files.createDirectories(docDir);
            }
            if (!Files.isWritable(docDir)) {
                throw new IOException("Infinispan index directory is not writable, please check the path; " + docDir);
            }

            //this subfolder will persist the cache to disk
            Path storeDir = Files.createDirectories(docDir.resolve(SUBFOLDR_STORE));
            //this subfolder will hold the lucene (or actually Hibernate-Search) index
            Path indexDir = Files.createDirectories(docDir.resolve(SUBFOLDR_INDEX));

            //configure Hibernate Search
            ConfigurationBuilder builder = new ConfigurationBuilder();

            //configure the lucene indexing
            builder.indexing()
                   // For replicated and local caches, the indexing is configured to be persisted on disk and not shared with any other processes.
                   // Also, it is configured so that minimum delay exists between the moment an object is indexed
                   // and the moment it is available for searches (near real time).
                   .index(Index.LOCAL)
                   // The attribute auto-config provides a simple way of configuring indexing based on the cache type.
                   // For details: http://infinispan.org/docs/8.1.x/user_guide/user_guide.html#_automatic_configuration
                   .autoConfig(true)
                   .addProperty("default.directory_provider", "filesystem")
                   .addProperty("default.indexBase", indexDir.toFile().getAbsolutePath())
                   .addProperty("lucene_version", "LUCENE_CURRENT");

            //configure the persistent cache store
            //From the docs:
            // "By default, unless marked explicitly as write-behind or asynchronous, all cache stores are write-through or synchronous."
            builder.persistence()
                   .addSingleFileStore()
                   .location(storeDir.toFile().getAbsolutePath())
                   //From the docs:
                   // 'If this maximum limit is set when the Infinispan is used as an authoritative data store,
                   //  it could lead to data loss, and hence it’s not recommended for this use case.'
                   .maxEntries(-1); //= unlimited

            //configure the transaction manager
            builder.transaction()
                   .autoCommit(false)
                   .transactionMode(TransactionMode.TRANSACTIONAL)
                   .transactionManagerLookup(new org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup());

            R.cacheManager().getApplicationCache().put(CacheKeys.INFINISPAN_CACHE_MANAGER, new DefaultCacheManager(builder.build()));
        }

        return (EmbeddedCacheManager) R.cacheManager().getApplicationCache().get(CacheKeys.INFINISPAN_CACHE_MANAGER);
    }
}

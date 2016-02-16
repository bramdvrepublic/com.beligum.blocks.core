package com.beligum.blocks.fs.indexes;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.indexes.ifaces.PageIndexer;
import com.beligum.blocks.fs.indexes.stubs.PageStub;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.SearchManager;
import org.infinispan.transaction.TransactionMode;

import javax.transaction.TransactionManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    public void indexPage(Page page) throws IOException
    {
        Cache<String, PageStub> cache = this.getCacheManager().getCache();

        PageStub stub = new PageStub(page);
        cache.put(stub.getId().toString(), stub);

        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(cache);
        QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(PageStub.class).get();
        org.apache.lucene.search.Query luceneQuery = queryBuilder.phrase()
                                                                 .onField("title")
                                                                 .sentence("please")
                                                                 .createQuery();

        CacheQuery query = searchManager.getQuery(luceneQuery, PageStub.class);
        List objectList = query.list();
        for (Object p : objectList) {
            Logger.info(p);
        }
    }
    @Override
    public void beginTransaction() throws IOException
    {
        try {
            this.getCacheManager().getCache().getAdvancedCache().getTransactionManager().begin();
        }
        catch (Exception e) {
            throw new IOException("Exception caught while starting a transaction", e);
        }
    }
    @Override
    public void commitTransaction() throws IOException
    {
        try {
            TransactionManager tm = this.getCacheManager().getCache().getAdvancedCache().getTransactionManager();
            if (tm.getTransaction()!=null) {
                tm.commit();
            }
        }
        catch (Exception e) {
            throw new IOException("Exception caught while starting a transaction", e);
        }
    }
    @Override
    public void rollbackTransaction() throws IOException
    {
        try {
            TransactionManager tm = this.getCacheManager().getCache().getAdvancedCache().getTransactionManager();
            if (tm.getTransaction()!=null) {
                tm.rollback();
            }
        }
        catch (Exception e) {
            throw new IOException("Exception caught while starting a transaction", e);
        }
    }
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
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.INFINISPAN_CACHE_MANAGER)) {

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

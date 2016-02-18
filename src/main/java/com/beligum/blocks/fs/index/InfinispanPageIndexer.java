package com.beligum.blocks.fs.index;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexer;
import com.beligum.blocks.fs.pages.DefaultPageImpl;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import org.apache.hadoop.fs.FileContext;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.ResultIterator;
import org.infinispan.query.SearchManager;
import org.infinispan.transaction.TransactionMode;

import javax.transaction.TransactionManager;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * See this:
 * http://infinispan.org/tutorials/simple/query/
 * and
 * http://infinispan.org/docs/8.1.x/user_guide/user_guide.html#_persistence
 * <p/>
 * Created by bram on 1/26/16.
 */
public class InfinispanPageIndexer implements PageIndexer<QueryBuilder, Query, CacheQuery>
{
    //-----CONSTANTS-----
    private static final String SUBFOLDR_STORE = "store";
    private static final String SUBFOLDR_INDEX = "index";

    private static final Class<? extends IndexEntry> INDEX_ENTRY_CLASS = PageIndexEntry.class;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public InfinispanPageIndexer() throws IOException
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexEntry get(URI key) throws IOException
    {
        Cache<String, PageIndexEntry> cache = this.getCacheManager().getCache();
        return cache.get(key.toString());
    }
    @Override
    public void delete(Page page) throws IOException
    {
        Cache<String, PageIndexEntry> cache = this.getCacheManager().getCache();
        cache.remove(page.buildAddress().toString());
    }
    @Override
    public void indexPage(Page page) throws IOException
    {
        Cache<String, PageIndexEntry> cache = this.getCacheManager().getCache();

        HtmlAnalyzer htmlAnalyzer = page.createAnalyzer();

        FileContext fc = page.getResourcePath().getFileContext();
        URI pageAddress = page.buildAddress();
        PageIndexEntry entry = new PageIndexEntry();
        entry.setId(pageAddress);
        entry.setResource(htmlAnalyzer.getHtmlResource() == null ? null : URI.create(htmlAnalyzer.getHtmlResource().value));
        entry.setLanguage(htmlAnalyzer.getHtmlLanguage() == null ? null : htmlAnalyzer.getHtmlLanguage().getLanguage());
        entry.setParent(this.getParentUri(pageAddress, fc));
        entry.setTitle(htmlAnalyzer.getTitle());
        entry.setTranslations(this.getTranslations(page, htmlAnalyzer.getHtmlLanguage()));

        //PageIndexEntry stub = new PageIndexEntry(page, htmlAnalyzer, this.getParentUri(page));
        cache.put(entry.getId().toString(), entry);

        //now, we need to update all pages with this page as a parent path (as a wildcard)
        // and adjust their logical parent because it might have changed
        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(cache);

//        Query allQuery = searchManager.buildQueryBuilderForClass(INDEX_ENTRY_CLASS).get().all().createQuery();
//        ResultIterator all = searchManager.getQuery(allQuery, INDEX_ENTRY_CLASS).iterator();
//        while (all.hasNext()) {
//            Logger.info(all.next().toString());
//        }

        Query query = searchManager.buildQueryBuilderForClass(INDEX_ENTRY_CLASS).get()
                                   .keyword()
                                   .wildcard()
                                   .onField("id")
                                   .matching(pageAddress.toString()+"*")
                                   .createQuery();
        ResultIterator resultIter = searchManager.getQuery(query, INDEX_ENTRY_CLASS).iterator();
        while (resultIter.hasNext()) {
            PageIndexEntry e = (PageIndexEntry) resultIter.next();
            //don't index ourself again
            if (!e.getId().equals(pageAddress)) {
                e.setParent(this.getParentUri(e.getId(), fc));
            }
        }
    }
    @Override
    public QueryBuilder getNewQueryBuilder() throws IOException
    {
        Cache<String, PageIndexEntry> cache = this.getCacheManager().getCache();
        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(cache);
        return searchManager.buildQueryBuilderForClass(INDEX_ENTRY_CLASS).get();
    }
    @Override
    public CacheQuery executeQuery(Query query) throws IOException
    {
        Cache<String, PageIndexEntry> cache = this.getCacheManager().getCache();
        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(cache);
        return searchManager.getQuery(query, INDEX_ENTRY_CLASS);
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
            if (tm.getTransaction() != null) {
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
            if (tm.getTransaction() != null) {
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
    /**
     * Look in our file system and search for the parent of this document (with the same language).
     */
    private URI getParentUri(URI pageUri, FileContext fc) throws IOException
    {
        URI retVal = null;

        URI parentUri = StringFunctions.getParent(pageUri);
        while (retVal == null) {
            if (parentUri == null) {
                break;
            }
            else {
                //note: this is null proof
                URI parentResourceUri = DefaultPageImpl.toResourceUri(parentUri, Settings.instance().getPagesStorePath());
                if (parentResourceUri != null && fc.util().exists(new org.apache.hadoop.fs.Path(parentResourceUri))) {
                    retVal = parentUri;
                }
                else {
                    parentUri = StringFunctions.getParent(parentUri);
                }

            }
        }

        return retVal;
    }
    /**
     * This will try to find all translations of this source (based on existing file structures)
     */
    private Map<String, URI> getTranslations(Page page, Locale htmlLanguage) throws IOException
    {
        Map<String, URI> retVal = new HashMap<>();

        //this is the lang attribute in the <html> tag
        if (htmlLanguage != null) {
            URI pageAddress = page.buildAddress();
            Map<String, Locale> siteLanguages = Settings.instance().getLanguages();
            for (Map.Entry<String, Locale> l : siteLanguages.entrySet()) {
                Locale lang = l.getValue();
                if (!lang.equals(htmlLanguage)) {
                    UriBuilder translatedUri = UriBuilder.fromUri(pageAddress);
                    Locale detectedLang = R.i18nFactory().getUrlLocale(pageAddress, translatedUri, lang);
                    if (detectedLang != null) {
                        URI transPagePublicUri = translatedUri.build();
                        URI transPageResourceUri = DefaultPageImpl.toResourceUri(transPagePublicUri, Settings.instance().getPagesStorePath());
                        if (page.getResourcePath().getFileContext().util().exists(new org.apache.hadoop.fs.Path(transPageResourceUri))) {
                            retVal.put(lang.getLanguage(), transPagePublicUri);
                        }
                    }
                }
            }
        }

        return retVal;
    }
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

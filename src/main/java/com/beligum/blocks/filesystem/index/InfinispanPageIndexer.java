//package com.beligum.blocks.fs.index;
//
//import com.beligum.base.server.R;
//import com.beligum.base.utils.Logger;
//import com.beligum.blocks.caching.CacheKeys;
//import com.beligum.blocks.config.Settings;
//import com.beligum.blocks.fs.index.entries.PageIndexEntry;
//import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
//import com.beligum.blocks.fs.index.ifaces.PageIndexer;
//import org.infinispan.Cache;
//import org.infinispan.configuration.cache.ConfigurationBuilder;
//import org.infinispan.configuration.cache.Index;
//import org.infinispan.manager.DefaultCacheManager;
//import org.infinispan.manager.EmbeddedCacheManager;
//import org.infinispan.transaction.TransactionMode;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//
///**
// * See this:
// * http://infinispan.org/tutorials/simple/query/
// * and
// * http://infinispan.org/docs/8.1.x/user_guide/user_guide.html#_persistence
// * <p/>
// * Created by bram on 1/26/16.
// */

//<dependency>
//<groupId>org.infinispan</groupId>
//<artifactId>infinispan-embedded</artifactId>
//<version>${version.infinispan}</version>
//</dependency>
//<dependency>
//<groupId>org.infinispan</groupId>
//<artifactId>infinispan-embedded-query</artifactId>
//<version>${version.infinispan}</version>
//</dependency>
//
//<!-- See https://github.com/infinispan/infinispan/blob/8.1.x/parent/pom.xml -->
//<dependency>
//<groupId>org.jboss.narayana.jta</groupId>
//<artifactId>narayana-jta</artifactId>
//<version>${version.jbossjta}</version>
//<exclusions>
//<exclusion>
//<artifactId>commons-httpclient</artifactId>
//<groupId>commons-httpclient</groupId>
//</exclusion>
//<exclusion>
//<artifactId>ironjacamar-spec-api</artifactId>
//<groupId>org.jboss.ironjacamar</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jbogging-spi</artifactId>
//<groupId>org.jboss.logging</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-logging</artifactId>
//<groupId>org.jboss.logging</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-logging-processor</artifactId>
//<groupId>org.jboss.logging</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-logging-generator</artifactId>
//<groupId>org.jboss.logging</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jbossws-native-core</artifactId>
//<groupId>org.jboss.ws.native</groupId>
//</exclusion>
//<exclusion>
//<artifactId>emma</artifactId>
//<groupId>emma</groupId>
//</exclusion>
//<exclusion>
//<artifactId>emma_ant</artifactId>
//<groupId>emma</groupId>
//</exclusion>
//<exclusion>
//<artifactId>hornetq-core</artifactId>
//<groupId>org.hornetq</groupId>
//</exclusion>
//<exclusion>
//<artifactId>netty</artifactId>
//<groupId>io.netty</groupId>
//</exclusion>
//<exclusion>
//<artifactId>wrapper</artifactId>
//<groupId>tanukisoft</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jacorb</artifactId>
//<groupId>jacorb</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jfreechart</artifactId>
//<groupId>jfree</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-corba-ots-spi</artifactId>
//<groupId>org.jboss.integration</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-server-manager</artifactId>
//<groupId>org.jboss.jbossas</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-ejb-api_3.1_spec</artifactId>
//<groupId>org.jboss.spec.javax.ejb</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jnp-client</artifactId>
//<groupId>org.jboss.naming</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-servlet-api_3.0_spec</artifactId>
//<groupId>org.jboss.spec.javax.servlet</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jbossws-common</artifactId>
//<groupId>org.jboss.ws</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jcl-over-slf4j</artifactId>
//<groupId>org.slf4j</groupId>
//</exclusion>
//<exclusion>
//<artifactId>stax-api</artifactId>
//<groupId>stax</groupId>
//</exclusion>
//<exclusion>
//<artifactId>idl</artifactId>
//<groupId>jacorb</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-logging-tools</artifactId>
//<groupId>org.jboss.logging</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-connector-api_1.5_spec</artifactId>
//<groupId>org.jboss.spec.javax.resource</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-remoting</artifactId>
//<groupId>org.jboss.remoting</groupId>
//</exclusion>
//<exclusion>
//<artifactId>slf4j-api</artifactId>
//<groupId>org.slf4j</groupId>
//</exclusion>
//<exclusion>
//<artifactId>dom4j</artifactId>
//<groupId>dom4j</groupId>
//</exclusion>
//<exclusion>
//<artifactId>commons-codec</artifactId>
//<groupId>commons-codec</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-logmanager</artifactId>
//<groupId>org.jboss.logmanager</groupId>
//</exclusion>
//<exclusion>
//<artifactId>hibernate-jpa-2.0-api</artifactId>
//<groupId>org.hibernate.javax.persistence</groupId>
//</exclusion>
//<exclusion>
//<artifactId>commons-logging</artifactId>
//<groupId>commons-logging</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jcommon</artifactId>
//<groupId>jfree</groupId>
//</exclusion>
//<exclusion>
//<artifactId>jboss-transaction-api_1.1_spec</artifactId>
//<groupId>org.jboss.spec.javax.transaction</groupId>
//</exclusion>
//<exclusion>
//<artifactId>byteman</artifactId>
//<groupId>org.jboss.byteman</groupId>
//</exclusion>
//<exclusion>
//<artifactId>byteman-submit</artifactId>
//<groupId>org.jboss.byteman</groupId>
//</exclusion>
//</exclusions>
//</dependency>

//public class InfinispanPageIndexer implements PageIndexer
//{
//    //-----CONSTANTS-----
//    private static final String SUBFOLDR_STORE = "store";
//    private static final String SUBFOLDR_INDEX = "index";
//
//    //-----VARIABLES-----
//
//    //-----CONSTRUCTORS-----
//    public InfinispanPageIndexer() throws IOException
//    {
//    }
//
//    //-----PUBLIC METHODS-----
//    @Override
//    public PageIndexConnection connect() throws IOException
//    {
//        Cache<String, PageIndexEntry> cache = this.getCacheManager().getCache();
//
//        return new InfinispanPageIndexConnection(cache);
//    }
////    @Override
////    public QueryBuilder getNewQueryBuilder() throws IOException
////    {
////        Cache<String, PageIndexEntry> cache = this.getCacheManager().getCache();
////        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(cache);
////        return searchManager.buildQueryBuilderForClass(INDEX_ENTRY_CLASS).get();
////    }
////    @Override
////    public CacheQuery executeQuery(Query query) throws IOException
////    {
////        Cache<String, PageIndexEntry> cache = this.getCacheManager().getCache();
////        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(cache);
////        return searchManager.getQuery(query, INDEX_ENTRY_CLASS);
////    }
//    @Override
//    public void shutdown()
//    {
//        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.INFINISPAN_CACHE_MANAGER)) {
//            try {
//                //TODO intested, don't know if this is ok
//                this.getCacheManager().stop();
//            }
//            catch (Exception e) {
//                Logger.setRollbackOnly("Exception caught while closing Infinispan cache manager", e);
//            }
//        }
//    }
//
//    //-----PROTECTED METHODS-----
//
//    //-----PRIVATE METHODS-----
//    private EmbeddedCacheManager getCacheManager() throws IOException
//    {
//        boolean skippedInit = true;
//
//        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.INFINISPAN_CACHE_MANAGER)) {
//
//            skippedInit = false;
//
//            final java.nio.file.Path docDir = Settings.instance().getPageMainIndexFolder().toPath();
//            if (!Files.exists(docDir)) {
//                Files.createDirectories(docDir);
//            }
//            if (!Files.isWritable(docDir)) {
//                throw new IOException("Infinispan index directory is not writable, please check the path; " + docDir);
//            }
//
//            //this subfolder will persist the cache to disk
//            Path storeDir = Files.createDirectories(docDir.resolve(SUBFOLDR_STORE));
//            //this subfolder will hold the lucene (or actually Hibernate-Search) index
//            Path indexDir = Files.createDirectories(docDir.resolve(SUBFOLDR_INDEX));
//
//            //configure Hibernate Search
//            ConfigurationBuilder builder = new ConfigurationBuilder();
//
//            //configure the lucene indexing
//            builder.indexing()
//                   // For replicated and local caches, the indexing is configured to be persisted on disk and not shared with any other processes.
//                   // Also, it is configured so that minimum delay exists between the moment an object is indexed
//                   // and the moment it is available for searches (near real time).
//                   .index(Index.LOCAL)
//                   // The attribute auto-config provides a simple way of configuring indexing based on the cache type.
//                   // For details: http://infinispan.org/docs/8.1.x/user_guide/user_guide.html#_automatic_configuration
//                   .autoConfig(true)
//                   .addProperty("default.directory_provider", "filesystem")
//                   .addProperty("default.indexBase", indexDir.toFile().getAbsolutePath())
//                   .addProperty("lucene_version", "LUCENE_CURRENT");
//
//            //configure the persistent cache store
//            //From the docs:
//            // "By default, unless marked explicitly as write-behind or asynchronous, all cache stores are write-through or synchronous."
//            builder.persistence()
//                   .addSingleFileStore()
//                   .location(storeDir.toFile().getAbsolutePath())
//                   //From the docs:
//                   // 'If this maximum limit is set when the Infinispan is used as an authoritative data store,
//                   //  it could lead to data loss, and hence it’s not recommended for this use case.'
//                   .maxEntries(-1); //= unlimited
//
//            //configure the transaction manager
//            builder.transaction()
//                   .autoCommit(false)
//                   .transactionMode(TransactionMode.TRANSACTIONAL)
//                   .transactionManagerLookup(new org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup());
//
//            R.cacheManager().getApplicationCache().put(CacheKeys.INFINISPAN_CACHE_MANAGER, new DefaultCacheManager(builder.build()));
//        }
//
//        return R.cacheManager().getApplicationCache().get(CacheKeys.INFINISPAN_CACHE_MANAGER);
//    }
//}

//package com.beligum.blocks.fs.index;
//
//import com.beligum.base.utils.toolkit.StringFunctions;
//import com.beligum.blocks.config.Settings;
//import com.beligum.blocks.fs.index.entries.IndexEntry;
//import com.beligum.blocks.fs.index.entries.PageIndexEntry;
//import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
//import com.beligum.blocks.fs.pages.DefaultPageImpl;
//import com.beligum.blocks.fs.pages.ifaces.Page;
//import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
//import org.apache.hadoop.fs.FileContext;
//import org.apache.lucene.search.Query;
//import org.infinispan.Cache;
//import org.infinispan.query.CacheQuery;
//import org.infinispan.query.ResultIterator;
//import org.infinispan.query.SearchManager;
//
//import java.io.IOException;
//import java.net.URI;
//
///**
// * Created by bram on 2/21/16.
// */
//public class InfinispanPageIndexConnection implements PageIndexConnection
//{
//    //-----CONSTANTS-----
//    private static final Class<? extends IndexEntry> INDEX_ENTRY_CLASS = PageIndexEntry.class;
//
//    //-----VARIABLES-----
//    private Cache<String, PageIndexEntry> cache;
//    private boolean transactionInProgress;
//
//    //-----CONSTRUCTORS-----
//    public InfinispanPageIndexConnection(Cache<String, PageIndexEntry> cache) throws IOException
//    {
//        this.cache = cache;
//
//        //start up a new transaction
//        this.transactionInProgress = false;
//    }
//
//    //-----PUBLIC METHODS-----
//    @Override
//    public PageIndexEntry get(URI key) throws IOException
//    {
//        return this.cache.get(key.toString());
//    }
//    @Override
//    public void delete(Page page) throws IOException
//    {
//        this.assertWriteTransaction();
//
//        cache.remove(page.buildAddress().toString());
//    }
//    @Override
//    public void update(Page page) throws IOException
//    {
//        this.assertWriteTransaction();
//
//        HtmlAnalyzer htmlAnalyzer = page.createAnalyzer();
//
//        FileContext fc = page.getResourcePath().getFileContext();
//        URI pageAddress = page.buildAddress();
//        PageIndexEntry entry = new PageIndexEntry();
//        entry.setId(pageAddress);
//        entry.setResource(htmlAnalyzer.getHtmlResource() == null ? null : URI.create(htmlAnalyzer.getHtmlResource().value));
//        entry.setLanguage(htmlAnalyzer.getHtmlLanguage() == null ? null : htmlAnalyzer.getHtmlLanguage().getLanguage());
//        entry.setParent(this.getParentUri(pageAddress, fc));
//        entry.setTitle(htmlAnalyzer.getTitle());
//
//        //PageIndexEntry stub = new PageIndexEntry(page, htmlAnalyzer, this.getParentUri(page));
//        this.cache.put(entry.getId().toString(), entry);
//
//        //now, we need to update all pages with this page as a parent path (as a wildcard)
//        // and adjust their logical parent because it might have changed
//        SearchManager searchManager = org.infinispan.query.Search.getSearchManager(this.cache);
//
//        //        Query allQuery = searchManager.buildQueryBuilderForClass(INDEX_ENTRY_CLASS).get().all().createQuery();
//        //        ResultIterator all = searchManager.getQuery(allQuery, INDEX_ENTRY_CLASS).iterator();
//        //        while (all.hasNext()) {
//        //            Logger.info(all.next().toString());
//        //        }
//
//        Query query = searchManager.buildQueryBuilderForClass(INDEX_ENTRY_CLASS).get()
//                                   .keyword()
//                                   .wildcard()
//                                   .onField("id")
//                                   .matching(pageAddress.toString()+"*")
//                                   .createQuery();
//
//        CacheQuery result = searchManager.getQuery(query, INDEX_ENTRY_CLASS);
//        if (result.getResultSize()>0) {
//            ResultIterator resultIter = result.iterator();
//            while (resultIter.hasNext()) {
//                PageIndexEntry e = (PageIndexEntry) resultIter.next();
//                //don't index ourself again
//                if (!e.getId().equals(pageAddress)) {
//                    e.setParent(this.getParentUri(e.getId(), fc));
//                }
//            }
//        }
//    }
//    @Override
//    public void commit() throws IOException
//    {
//        if (this.transactionInProgress) {
//            try {
//                this.cache.getAdvancedCache().getTransactionManager().commit();
//                this.transactionInProgress = false;
//            }
//            catch (Exception e) {
//                throw new IOException("Error occurred while committing infinispan page indexer transaction", e);
//            }
//        }
//    }
//    @Override
//    public void rollback() throws IOException
//    {
//        if (this.transactionInProgress) {
//            try {
//                this.cache.getAdvancedCache().getTransactionManager().rollback();
//                this.transactionInProgress = false;
//            }
//            catch (Exception e) {
//                throw new IOException("Error occurred while rolling back infinispan page indexer transaction", e);
//            }
//        }
//    }
//    @Override
//    public void close() throws Exception
//    {
//        //if we still have a transaction open, something's wrong cause we do manual tx management
//        if (this.transactionInProgress) {
//            this.rollback();
//            throw new IOException("Open transaction found while closing infinispan page index connection; rolled back the transaction just to be safe...");
//        }
//
//        //we nullify so this connection can't be reused
//        this.cache = null;
//    }
//
//    //-----PROTECTED METHODS-----
//
//    //-----PRIVATE METHODS-----
//    private void assertWriteTransaction() throws IOException
//    {
//        if (!this.transactionInProgress) {
//            try {
//                this.cache.getAdvancedCache().getTransactionManager().begin();
//                this.transactionInProgress = true;
//            }
//            catch (Exception e) {
//                throw new IOException("Error occurred while booting infinispan page indexer transaction", e);
//            }
//        }
//    }
//    /**
//     * Look in our file system and search for the parent of this document (with the same language).
//     */
//    private URI getParentUri(URI pageUri, FileContext fc) throws IOException
//    {
//        URI retVal = null;
//
//        URI parentUri = StringFunctions.getParent(pageUri);
//        while (retVal == null) {
//            if (parentUri == null) {
//                break;
//            }
//            else {
//                //note: this is null proof
//                URI parentResourceUri = DefaultPageImpl.toResourceUri(parentUri, Settings.instance().getPagesStorePath());
//                if (parentResourceUri != null && fc.util().exists(new org.apache.hadoop.fs.Path(parentResourceUri))) {
//                    retVal = parentUri;
//                }
//                else {
//                    parentUri = StringFunctions.getParent(parentUri);
//                }
//
//            }
//        }
//
//        return retVal;
//    }
//}

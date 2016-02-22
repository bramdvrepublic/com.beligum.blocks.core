package com.beligum.blocks.fs.index;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.index.entries.AbstractIndexEntry;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.index.ifaces.PageIndexer;
import com.beligum.blocks.fs.pages.DefaultPageImpl;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import org.apache.hadoop.fs.FileContext;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Started with this:
 * http://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/IndexFiles.html
 * <p/>
 * interesting read:
 * http://stackoverflow.com/questions/9377572/is-it-good-practice-to-keep-a-lucene-indexwriter-indexsearcher-open-for-the-li
 * <p/>
 * and for Hibernate:
 * org.hibernate.search.spi.SearchFactoryBuilder.initDocumentBuilders()
 * <p/>
 * Created by bram on 1/26/16.
 */
public class LucenePageIndexer implements PageIndexer, PageIndexConnection
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Map<String, String> fieldToAnalyzerMap;
    private Object indexLock;

    //-----CONSTRUCTORS-----
    public LucenePageIndexer() throws IOException
    {
        //we don't use this, so let's create it once and keep it around
        this.fieldToAnalyzerMap = new HashMap<String, String>();
        this.indexLock = new Object();
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexEntry get(URI key) throws IOException
    {
        TermQuery query = new TermQuery(AbstractIndexEntry.toLuceneId(key));
        TopDocs topdocs = getLuceneIndexSearcher().search(query, 1);

        if (topdocs.totalHits == 0) {
            return null;
        }
        else {
            return PageIndexEntry.fromLuceneDoc(getLuceneIndexReader().document(topdocs.scoreDocs[0].doc));
        }
    }
    @Override
    public void delete(Page page) throws IOException
    {

    }
    @Override
    public void indexPage(Page page) throws IOException
    {
        PageIndexEntry indexExtry = this.createEntry(page);

        //let's not mix-and-mingle writes (even though the IndexWriter is thread-safe),
        // so we can do a clean commit/rollback on our own
        //TODO this should probably be synchronized with the transaction methods in some way
        synchronized (this.indexLock) {
            try (IndexWriter indexWriter = this.getNewLuceneIndexWriter()) {
                //note: there's not such thing as a .begin(); the begin is just where the last .commit() left off
                indexWriter.updateDocument(AbstractIndexEntry.toLuceneId(indexExtry), PageIndexEntry.toLuceneDoc(indexExtry));
            }

            this.printLuceneIndex();
        }
    }
    @Override
    public void close() throws Exception
    {
    }
    @Override
    public PageIndexConnection connect() throws IOException
    {
        return this;
    }
    @Override
    public void commit() throws IOException
    {
        try (IndexWriter indexWriter = this.getNewLuceneIndexWriter()) {
            if (indexWriter.isOpen()) {
                indexWriter.commit();
            }
        }
    }
    @Override
    public void rollback() throws IOException
    {
        try (IndexWriter indexWriter = this.getNewLuceneIndexWriter()) {
            if (indexWriter.isOpen()) {
                indexWriter.rollback();
            }
        }
    }
    @Override
    public void shutdown()
    {
        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_READER)) {
            try (IndexReader reader = this.getLuceneIndexReader()) {
                reader.close();
            }
            catch (Exception e) {
                Logger.error("Exception caught while closing Lucene reader", e);
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void printLuceneIndex() throws IOException
    {
        final java.nio.file.Path docDir = Settings.instance().getPageMainIndexFolder().toPath();
        Directory dir = FSDirectory.open(docDir);

        try (IndexReader reader = DirectoryReader.open(dir)) {
            int numDocs = reader.numDocs();
            for (int i = 0; i < numDocs; i++) {
                Document d = reader.document(i);
                System.out.println(i + ") " + d);
            }
        }
    }
    private PageIndexEntry createEntry(Page page) throws IOException
    {
        HtmlAnalyzer htmlAnalyzer = page.createAnalyzer();

        FileContext fc = page.getResourcePath().getFileContext();
        URI pageAddress = page.buildAddress();
        PageIndexEntry entry = new PageIndexEntry(pageAddress);
        entry.setResource(htmlAnalyzer.getHtmlResource() == null ? null : htmlAnalyzer.getHtmlResource().value);
        entry.setLanguage(htmlAnalyzer.getHtmlLanguage() == null ? null : htmlAnalyzer.getHtmlLanguage().getLanguage());
        entry.setParent(this.getParentUri(pageAddress, fc).toString());
        entry.setTitle(htmlAnalyzer.getTitle());

        return entry;
    }
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
     * From the Lucene JavaDoc:
     * "IndexWriter instances are completely thread safe, meaning multiple threads can call any of its methods, concurrently."
     * so I hope it's ok to keep this open.
     * <p/>
     * Reading here, it seems to be an OK usecase:
     * http://stackoverflow.com/questions/8878448/lucene-good-practice-and-thread-safety
     *
     * @return
     * @throws IOException
     */
    private IndexWriter getNewLuceneIndexWriter() throws IOException
    {
        final java.nio.file.Path docDir = Settings.instance().getPageMainIndexFolder().toPath();
        if (!Files.exists(docDir)) {
            Files.createDirectories(docDir);
        }
        if (!Files.isWritable(docDir)) {
            throw new IOException("Lucene index directory is not writable, please check the path; " + docDir);
        }

        IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());

        // Add new documents to an existing index:
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        return new IndexWriter(FSDirectory.open(Settings.instance().getPageMainIndexFolder().toPath()), iwc);
    }
    private IndexReader getLuceneIndexReader() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_READER)) {
            //make sure the basic structure to read stuff exists
            try (IndexWriter writer = getNewLuceneIndexWriter()) {}

            IndexReader reader = DirectoryReader.open(FSDirectory.open(Settings.instance().getPageMainIndexFolder().toPath()));

            R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_READER, reader);
        }

        return (IndexReader) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_READER);
    }
    private IndexSearcher getLuceneIndexSearcher() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_SEARCHER)) {
            IndexSearcher searcher = new IndexSearcher(getLuceneIndexReader());

            R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_SEARCHER, searcher);
        }

        return (IndexSearcher) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_SEARCHER);
    }
}

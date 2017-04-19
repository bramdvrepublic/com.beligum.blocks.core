package com.beligum.blocks.filesystem.index;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexConnection;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FSLockFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Started with this:
 * http://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/IndexFiles.html
 * and
 * http://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/SearchFiles.html
 * <p/>
 * interesting read:
 * http://stackoverflow.com/questions/9377572/is-it-good-practice-to-keep-a-lucene-indexwriter-indexsearcher-open-for-the-li
 * <p/>
 * and for Hibernate:
 * org.hibernate.search.spi.SearchFactoryBuilder.initDocumentBuilders()
 * <p/>
 * Created by bram on 1/26/16.
 */
public class LucenePageIndexer implements PageIndexer
{
    //-----CONSTANTS-----
    public static final String DEFAULT_FIELD_JOINER = " ";

    private static final String CUSTOM_FIELD_PREFIX = "_";
    //mimics the "_all" field of ElasticSearch
    // see https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-all-field.html
    public static final String CUSTOM_FIELD_ALL = CUSTOM_FIELD_PREFIX + "all";
    //keeps a list of all fields in this doc, to be able to search for non-existence of a field
    public static final String CUSTOM_FIELD_FIELDS = CUSTOM_FIELD_PREFIX + "fields";

    protected static final Analyzer STANDARD_ANALYZER = new StandardAnalyzer();
    protected static final Analyzer KEYWORD_ANALYZER = new KeywordAnalyzer();
    protected static final Analyzer WHITESPACE_ANALYZER = new WhitespaceAnalyzer();
    public static final Analyzer DEFAULT_ANALYZER = STANDARD_ANALYZER;

    //-----VARIABLES-----
    private static final FSLockFactory luceneLockFactory = FSLockFactory.getDefault();
    private java.nio.file.Path indexFolder;
    private Object searcherLock;
    private Object writerLock;

    //-----CONSTRUCTORS-----
    public LucenePageIndexer() throws IOException
    {
        this.searcherLock = new Object();
        this.writerLock = new Object();

        this.reinit();
    }

    //-----PUBLIC METHODS-----
    @Override
    public synchronized PageIndexConnection connect(TX tx) throws IOException
    {
        return new LucenePageIndexConnection(this, tx);
    }
    @Override
    public synchronized void reboot() throws IOException
    {
        try {
            this.shutdown();
        }
        finally {
            this.reinit();
        }
    }
    @Override
    public synchronized void shutdown()
    {
        synchronized (this.searcherLock) {
            if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_SEARCHER)) {
                IndexSearcher indexSearcher = (IndexSearcher) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_SEARCHER);
                try (IndexReader indexReader = indexSearcher.getIndexReader()) {
                    indexReader.close();
                }
                catch (Exception e) {
                    Logger.error("Exception caught while closing Lucene reader", e);
                }
                finally {
                    R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_SEARCHER);
                }
            }
        }

        synchronized (this.writerLock) {
            if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_WRITER)) {
                try (IndexWriter indexWriter = (IndexWriter) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_WRITER)) {
                    indexWriter.close();
                }
                catch (Exception e) {
                    Logger.error("Exception caught while closing Lucene writer", e);
                }
                finally {
                    R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_WRITER);
                }
            }
        }
    }
    public IndexSearcher getIndexSearcher() throws IOException
    {
        synchronized (this.searcherLock) {
            if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_SEARCHER)) {
                IndexReader indexReader = DirectoryReader.open(FSDirectory.open(this.indexFolder));
                IndexSearcher indexSearcher = new IndexSearcher(indexReader);

                R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_SEARCHER, indexSearcher);
            }

            return (IndexSearcher) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_SEARCHER);
        }
    }
    public IndexWriter getIndexWriter() throws IOException
    {
        synchronized (this.writerLock) {
            if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_WRITER)) {
                R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_WRITER, buildNewLuceneIndexWriter(this.indexFolder));
            }

            //Note that a Lucene rollback closes the index for concurrency reasons, so double-check
            IndexWriter retVal = (IndexWriter) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_WRITER);
            if (retVal == null || !retVal.isOpen()) {
                R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_WRITER, retVal = buildNewLuceneIndexWriter(this.indexFolder));
            }

            return retVal;
        }
    }
    public void indexChanged()
    {
        synchronized (this.searcherLock) {
            //will be re-initialized on next read/search
            R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_SEARCHER);
        }
    }

    //-----PUBLIC STATIC METHODS-----
    //exactly the same code as QueryParserBase.escape(), but with the sb.append('\\'); line commented and added an else-part
    public static String removeEscapedChars(String s, String replacement)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
                || c == '*' || c == '?' || c == '|' || c == '&' || c == '/') {
                //sb.append('\\');
                sb.append(replacement);
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void reinit() throws IOException
    {
        R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_SEARCHER);
        R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_WRITER);

        this.indexFolder = Paths.get(Settings.instance().getPageMainIndexFolder());

        if (!Files.exists(this.indexFolder)) {
            Files.createDirectories(this.indexFolder);
        }
        if (!Files.isWritable(this.indexFolder)) {
            throw new IOException("Lucene index directory is not writable, please check the permissions of: " + this.indexFolder);
        }

        try (IndexWriter indexWriter = buildNewLuceneIndexWriter(indexFolder)) {
            //just open and close the writer once to instance the necessary files,
            // else we'll get a "no segments* file found" exception on first search
        }
    }
    /**
     * From the Lucene JavaDoc:
     * "IndexWriter instances are completely thread safe, meaning multiple threads can call any of its methods, concurrently."
     * so I hope it's ok to keep this open.
     * Note: switched to instance-generation because an open writer seemed to block access to the directory with a .lock file?
     * <p/>
     * Reading here, it seems to be an OK usecase:
     * http://stackoverflow.com/questions/8878448/lucene-good-practice-and-thread-safety
     *
     * @return
     * @throws IOException
     */
    private IndexWriter buildNewLuceneIndexWriter(Path docDir) throws IOException
    {
        IndexWriterConfig iwc = new IndexWriterConfig(LucenePageIndexer.DEFAULT_ANALYZER);

        // Add new documents to an existing index:
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        return new IndexWriter(FSDirectory.open(docDir, luceneLockFactory), iwc);
    }
    private void printLuceneIndex() throws IOException
    {
        Directory dir = FSDirectory.open(this.indexFolder);

        try (IndexReader reader = DirectoryReader.open(dir)) {
            int numDocs = reader.numDocs();
            for (int i = 0; i < numDocs; i++) {
                Document d = reader.document(i);
                System.out.println(i + ") " + d);
            }
        }
    }
}

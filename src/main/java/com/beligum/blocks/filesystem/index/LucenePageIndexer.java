package com.beligum.blocks.filesystem.index;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexConnection;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;

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
    public static final String CUSTOM_FIELD_ALL = CUSTOM_FIELD_PREFIX+"all";
    //keeps a list of all fields in this doc, to be able to search for non-existence of a field
    public static final String CUSTOM_FIELD_FIELDS = CUSTOM_FIELD_PREFIX+"fields";

    protected static final Analyzer STANDARD_ANALYZER = new StandardAnalyzer();
    protected static final Analyzer KEYWORD_ANALYZER = new KeywordAnalyzer();
    protected static final Analyzer WHITESPACE_ANALYZER = new WhitespaceAnalyzer();
    public static final Analyzer DEFAULT_ANALYZER = STANDARD_ANALYZER;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public LucenePageIndexer() throws IOException
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public synchronized PageIndexConnection connect() throws IOException
    {
        return this.connect(StorageFactory.getCurrentRequestTx());
    }
    @Override
    public synchronized PageIndexConnection connect(TX tx) throws IOException
    {
        return new LucenePageIndexConnection(this, tx);
    }
    @Override
    public synchronized void shutdown()
    {
        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_SEARCHER)) {
            IndexSearcher indexSearcher = (IndexSearcher) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_SEARCHER);
            try (IndexReader indexReader = indexSearcher.getIndexReader()) {
                indexReader.close();
            }
            catch (Exception e) {
                Logger.error("Exception caught while closing Lucene reader", e);
            }

            R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_SEARCHER);
        }

        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_WRITER)) {
            try (IndexWriter indexWriter = (IndexWriter) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_WRITER)) {
                indexWriter.close();
            }
            catch (Exception e) {
                Logger.error("Exception caught while closing Lucene writer", e);
            }

            R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_WRITER);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

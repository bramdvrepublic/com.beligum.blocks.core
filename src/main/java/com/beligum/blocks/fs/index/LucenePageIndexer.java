package com.beligum.blocks.fs.index;

import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.index.ifaces.PageIndexer;

import java.io.IOException;

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
public class LucenePageIndexer implements PageIndexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public LucenePageIndexer() throws IOException
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexConnection connect() throws IOException
    {
        return new LucenePageIndexerConnection();
    }
    @Override
    public void shutdown()
    {
        //TODO
//        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_READER)) {
//            try (IndexReader reader = this.getLuceneIndexReader()) {
//                reader.close();
//            }
//            catch (Exception e) {
//                Logger.error("Exception caught while closing Lucene reader", e);
//            }
//        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

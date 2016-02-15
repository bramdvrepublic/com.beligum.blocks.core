package com.beligum.blocks.fs.indexes;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.HdfsPathInfo;
import com.beligum.blocks.fs.indexes.ifaces.PageIndexer;
import com.beligum.blocks.fs.indexes.stubs.PageStub;
import com.beligum.blocks.fs.indexes.stubs.Stub;
import com.beligum.blocks.fs.pages.DefaultPageImpl;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.spi.SearchIntegratorBuilder;

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
public class LucenePageIndexer implements PageIndexer
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
    public void indexPage(Page page) throws IOException
    {
        PageStub stub = new PageStub(page);
        PageStub stub2 = new PageStub(new DefaultPageImpl(new HdfsPathInfo(page.getPathInfo().getFileContext(), URI.create("http://tweakers.net"))));
        stub.addChild(stub2);

        IndexWriter luceneWriter = this.getLuceneIndexWriter();
        Document doc = this.toDoc(stub);
        Term idTerm = new Term(Stub.ID_FIELD_NAME, stub.getId().toString());

        //let's not mix-and-mingle writes (even though the IndexWriter is thread-safe),
        // so we can do a clean commit/rollback on our own
        //TODO this should probably be synchronized with the transaction methods in some way
        synchronized (this.indexLock) {
            //note: there's not such thing as a .begin(); the begin is just where the last .commit() left off
            luceneWriter.updateDocument(idTerm, doc);
            this.printLuceneIndex();
        }
    }
    @Override
    public void beginTransaction() throws IOException
    {
        //note: there's not such thing as a .begin(); the begin is just where the last .commit() left off
    }
    @Override
    public void commitTransaction() throws IOException
    {
        IndexWriter luceneWriter = this.getLuceneIndexWriter();
        if (luceneWriter.isOpen()) {
            luceneWriter.commit();
        }
    }
    @Override
    public void rollbackTransaction() throws IOException
    {
        IndexWriter luceneWriter = this.getLuceneIndexWriter();
        if (luceneWriter.isOpen()) {
            luceneWriter.rollback();
        }
    }
    @Override
    public void shutdown()
    {
        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_WRITER)) {
            try {
                this.getLuceneIndexWriter().close();
            }
            catch (Exception e) {
                Logger.error("Exception caught while closing Lucene writer", e);
            }
        }
        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.HIBERNATE_SEARCH_SEARCH_INTEGRATOR)) {
            try {
                this.getHBSearchIntegrator().close();
            }
            catch (Exception e) {
                Logger.error("Exception caught while closing Hibernate Search integrator", e);
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
                System.out.println(i+") " + d);
            }
        }
    }
    /**
     * Good start: org.hibernate.search.query.dsl.impl.MoreLikeThisBuilder
     * This method uses the handy Hibernate Search annotations to convert a POJO to a Lucene document.
     */
    private Document toDoc(Stub stub)
    {
        Document retVal = null;

        if (stub != null) {
            DocumentBuilderIndexedEntity docBuilder = this.getHBSearchIntegrator().getIndexBinding(stub.getClass()).getDocumentBuilder();
            retVal = docBuilder.getDocument(null, stub, stub.getId(), this.fieldToAnalyzerMap, null, new ContextualExceptionBridgeHelper(), null);
        }

        return retVal;
    }
    private ExtendedSearchIntegrator getHBSearchIntegrator()
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.HIBERNATE_SEARCH_SEARCH_INTEGRATOR)) {
            SearchIntegratorBuilder searchIntegratorBuilder = new SearchIntegratorBuilder();

            //TODO maybe change the reflectionManager in LuceneSearchConfiguration?
            SearchConfiguration config = new LuceneSearchConfiguration();
            searchIntegratorBuilder.configuration(config);

            //the extended version seems to be used throughout all Hibernate code, so we cast it to mimic the HB code as close as possible
            ExtendedSearchIntegrator searchIntegrator = (ExtendedSearchIntegrator) searchIntegratorBuilder.buildSearchIntegrator();

            R.cacheManager().getApplicationCache().put(CacheKeys.HIBERNATE_SEARCH_SEARCH_INTEGRATOR, searchIntegrator);
        }

        return (ExtendedSearchIntegrator) R.cacheManager().getApplicationCache().get(CacheKeys.HIBERNATE_SEARCH_SEARCH_INTEGRATOR);
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
    private IndexWriter getLuceneIndexWriter() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_WRITER)) {

            final java.nio.file.Path docDir = Settings.instance().getPageMainIndexFolder().toPath();
            if (!Files.exists(docDir)) {
                Files.createDirectories(docDir);
            }
            if (!Files.isWritable(docDir)) {
                throw new IOException("Lucene index directory is not writable, please check the path; " + docDir);
            }

            Directory dir = FSDirectory.open(docDir);
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            // Add new documents to an existing index:
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            IndexWriter writer = new IndexWriter(dir, iwc);

            R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_WRITER, writer);
        }

        return (IndexWriter) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_WRITER);
    }
}

package com.beligum.blocks.fs.indexes;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.indexes.ifaces.PageIndex;
import com.beligum.blocks.fs.indexes.stubs.PageStub;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.impl.MutableSearchFactory;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Started with this:
 * http://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/IndexFiles.html
 *
 * Created by bram on 1/26/16.
 */
public class LucenePageIndex implements PageIndex
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public LucenePageIndex() throws IOException
    {
        final Path docDir = Settings.instance().getPageMainIndexFolder().toPath();
        if (!Files.exists(docDir)) {
            Files.createDirectories(docDir);
        }
        if (!Files.isWritable(docDir)) {
            throw new IOException("Lucene index directory is not writable, please check the path; " + docDir);
        }

        //TODO check .close()
        Directory dir = FSDirectory.open(docDir);
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        final boolean create = false;
        if (create) {
            // Create a new index in the directory, removing any
            // previously indexed documents:
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        }
        else {
            // Add new documents to an existing index:
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        // Optional: for better indexing performance, if you
        // are indexing many documents, increase the RAM
        // buffer.  But if you do this, increase the max heap
        // size to the JVM (eg add -Xmx512m or -Xmx1g):
        //
        // iwc.setRAMBufferSizeMB(256.0);

        IndexWriter writer = new IndexWriter(dir, iwc);

        MutableSearchFactory mutableSearchFactory = new MutableSearchFactory();
        //SearchIntegratorBuilder searchIntegratorBuilder = new SearchIntegratorBuilder();
        SearchIntegratorBuilder searchIntegratorBuilder = new SearchIntegratorBuilder().currentSearchIntegrator(mutableSearchFactory);
        //searchIntegratorBuilder.configuration(new ReflectionReplacingSearchConfiguration());
        SearchIntegrator searchIntegrator = searchIntegratorBuilder.buildSearchIntegrator();

        searchIntegrator.addClasses(PageStub.class);

        //See MoreLikeThisBuilder
        //TODO should we keep the fieldToAnalyzerMap around to pass to the analyzer?
        Map<String,String> fieldToAnalyzerMap = new HashMap<String, String>( );
        //FIXME by calling documentBuilder we don't honor .comparingField("foo").ignoreFieldBridge(): probably not a problem in practice though
        Document doc = searchIntegrator.getIndexBinding(PageStub.class).getDocumentBuilder().getDocument(null, new PageStub(), null, fieldToAnalyzerMap, null, new ContextualExceptionBridgeHelper(), null);

        writer.addDocument(doc);

//        SearchFactoryBuilder searchFactoryBuilder = new SearchFactoryBuilder();
//        MutableEntityIndexBinding binding = EntityIndexBindingFactory.buildEntityIndexBinding();
//        DefaultMutableEntityIndexBinding entityIndexBinding = new DefaultMutableEntityIndexBinding();





        // NOTE: if you want to maximize search performance,
        // you can optionally call forceMerge here.  This can be
        // a terribly costly operation, so generally it's only
        // worth it when your index is relatively static (ie
        // you're done adding documents to it):
        //
        // writer.forceMerge(1);

        writer.close();
    }

    //-----PUBLIC METHODS-----
    @Override
    public void indexPage(Page page) throws IOException
    {
        try {

        }
        finally {

        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

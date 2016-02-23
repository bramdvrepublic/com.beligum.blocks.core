//package com.beligum.blocks.fs.index;
//
//import com.beligum.base.server.R;
//import com.beligum.blocks.caching.CacheKeys;
//import com.beligum.blocks.config.Settings;
//import com.beligum.blocks.fs.index.entries.PageIndexEntry;
//import com.beligum.blocks.fs.index.ifaces.PageIndexer;
//import com.beligum.blocks.fs.pages.ifaces.Page;
//import com.beligum.blocks.rdf.ifaces.Importer;
//import com.beligum.blocks.rdf.importers.JenaImporter;
//import org.apache.hadoop.fs.FileContext;
//import org.apache.jena.arq.querybuilder.SelectBuilder;
//import org.apache.jena.query.*;
//import org.apache.jena.query.text.EntityDefinition;
//import org.apache.jena.query.text.TextDatasetFactory;
//import org.apache.jena.rdf.model.Model;
//import org.apache.jena.tdb.TDBFactory;
//import org.apache.jena.vocabulary.RDFS;
//import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.SimpleFSDirectory;
//import org.apache.lucene.util.Version;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URI;
//
///**
// * Created by bram on 1/26/16.
// * <p/>
// * Some interesting reads:
// * <p/>
// * https://jena.apache.org/documentation/tdb/java_api.html
// */


//<dependency>
//<groupId>org.apache.jena</groupId>
//<artifactId>jena-arq</artifactId>
//<version>${jena.version}</version>
//<exclusions>
//<exclusion>
//<groupId>org.slf4j</groupId>
//<artifactId>slf4j-log4j12</artifactId>
//</exclusion>
//</exclusions>
//</dependency>
//<dependency>
//<groupId>org.apache.jena</groupId>
//<artifactId>jena-tdb</artifactId>
//<version>${jena.version}</version>
//<exclusions>
//<exclusion>
//<groupId>org.slf4j</groupId>
//<artifactId>slf4j-log4j12</artifactId>
//</exclusion>
//</exclusions>
//</dependency>
//<dependency>
//<groupId>org.apache.jena</groupId>
//<artifactId>jena-querybuilder</artifactId>
//<version>${jena.version}</version>
//<exclusions>
//<exclusion>
//<groupId>org.slf4j</groupId>
//<artifactId>slf4j-log4j12</artifactId>
//</exclusion>
//</exclusions>
//</dependency>
//<dependency>
//<groupId>org.apache.jena</groupId>
//<artifactId>jena-text</artifactId>
//<version>${jena.version}</version>
//<exclusions>
//<exclusion>
//<groupId>org.slf4j</groupId>
//<artifactId>slf4j-log4j12</artifactId>
//</exclusion>
//</exclusions>
//</dependency>

//public class JenaPageIndexer implements PageIndexer<SelectBuilder, Query, QueryExecution>
//{
//    //-----CONSTANTS-----
//
//    //-----VARIABLES-----
//    private Object datasetLock;
//
//    //-----CONSTRUCTORS-----
//    public JenaPageIndexer()
//    {
//        this.datasetLock = new Object();
//    }
//
//    //-----PUBLIC METHODS-----
//    @Override
//    public PageIndexEntry get(URI key) throws IOException
//    {
//        //TODO
//        return null;
//    }
//    @Override
//    public void delete(Page page) throws IOException
//    {
//        //TODO
//    }
//    @Override
//    public void update(Page page) throws IOException
//    {
//        Dataset dataset = this.getRDFDataset();
//
//        FileContext fc = page.getResourcePath().getFileContext();
//        //note: we can't re-use the Sesame-based page importer here, because sesame expects the stored N-Triples
//        // to be ASCII, while Jena (our default exporter) exports them as UTF-8,
//        // so let's manually create a specific Jena importer here.
//        //Importer rdfImporter = page.createImporter(page.getRdfExportFileFormat());
//        Importer rdfImporter = new JenaImporter(page.getRdfExportFileFormat());
//        Model model = null;
//        try (InputStream is = fc.open(page.getRdfExportFile())) {
//            model = rdfImporter.importDocument(is, page.buildAddress());
//        }
//
//        dataset.getDefaultModel().add(model);
//
//        //dataset.addNamedModel(modelName.toString(), model);
////        dataset.getDefaultModel().add(page.getRDFModel());
//
//        //just testing...
//        String qs1 = "SELECT * {?s ?p ?o} LIMIT 10" ;
//        try(QueryExecution qExec = QueryExecutionFactory.create(qs1, dataset)) {
//            ResultSet rs = qExec.execSelect() ;
//            ResultSetFormatter.out(rs) ;
//        }
//    }
//    @Override
//    public SelectBuilder getNewQueryBuilder() throws IOException
//    {
//        return new SelectBuilder();
//    }
//    /**
//     * Note that this returns an autoclosable!!!
//     * @param query
//     * @return
//     * @throws IOException
//     */
//    @Override
//    public QueryExecution executeQuery(Query query) throws IOException
//    {
//        return QueryExecutionFactory.create(query, this.getRDFDataset());
//    }
//    @Override
//    public void beginTransaction() throws IOException
//    {
//        this.getRDFDataset().begin(ReadWrite.WRITE);
//    }
//    @Override
//    public void commitTransaction() throws IOException
//    {
//        Dataset dataset = this.getRDFDataset();
//        if (dataset.isInTransaction()) {
//            dataset.commit();
//            dataset.end();
//        }
//    }
//    @Override
//    public void rollbackTransaction() throws IOException
//    {
//        Dataset dataset = this.getRDFDataset();
//        if (dataset.isInTransaction()) {
//            dataset.abort();
//            dataset.end();
//        }
//    }
//    @Override
//    public void shutdown() throws IOException
//    {
//        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_DATASET)) {
//            this.getRDFDataset().close();
//        }
//    }
//
//    //-----PROTECTED METHODS-----
//
//    //-----PRIVATE METHODS-----
//    private Dataset getRDFDataset() throws IOException
//    {
//        synchronized (this.datasetLock) {
//            if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_DATASET)) {
//                Dataset dataset = TDBFactory.createDataset(Settings.instance().getPageTripleStoreFolder().getAbsolutePath());
//
//
//                //TextIndexConfig config = new TextIndexConfig(def);
//                //EntityDefinition entDef = new EntityDefinition("uri", "text", ResourceFactory.createProperty(URI, indexedProperty ) );
//                //EntityDefinition entDef = new EntityDefinition("uri", "text", RDFS.label);
//                EntityDefinition entDef = new EntityDefinition("uri", "text");
//                entDef.setPrimaryPredicate(RDFS.label.asNode());
//
//                Directory luceneDir = new SimpleFSDirectory(new File("/home/bram/test/triplestore_index/"));
//
//                Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
//                Dataset indexedDataset = TextDatasetFactory.createLucene(dataset, luceneDir, entDef, analyzer);
//
//                R.cacheManager().getApplicationCache().put(CacheKeys.RDF_DATASET, indexedDataset);
//            }
//
//            return (Dataset) R.cacheManager().getApplicationCache().get(CacheKeys.RDF_DATASET);
//        }
//    }
//}

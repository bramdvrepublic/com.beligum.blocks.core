package com.beligum.blocks.endpoints;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.FieldValueQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static gen.com.beligum.base.core.constants.base.core.ADMIN_ROLE_NAME;

@Path("debug")
@RequiresRoles(ADMIN_ROLE_NAME)
public class DebugEndpoint
{
    //    @Path("sparql")
    //    public Response testSparql() throws IOException
    //    {
    //        PageIndexer<SelectBuilder, Query, QueryExecution> tripleStore = StorageFactory.getTriplestoreIndexer();
    //
    //        boolean success = false;
    //        try {
    //            tripleStore.beginTransaction();
    //
    //            SelectBuilder selectBuilder = tripleStore.getNewQueryBuilder();
    //            selectBuilder.addVar("*").addWhere("?s", "?p", "?o");
    //
    //            try (QueryExecution qExec = tripleStore.executeQuery(selectBuilder.build())) {
    //                ResultSet rs = qExec.execSelect();
    //                ResultSetFormatter.out(rs);
    //            }
    //
    //            success = true;
    //        }
    //        finally {
    //            if (success) {
    //                tripleStore.commitTransaction();
    //            }
    //            else {
    //                tripleStore.rollbackTransaction();
    //            }
    //        }
    //
    //        return Response.ok().build();
    //    }
    //
    //    @Path("lucene")
    //    public Response testLucene() throws IOException
    //    {
    //        final java.nio.file.Path docDir = Settings.instance().getPageMainIndexFolder().toPath();
    //        if (!Files.exists(docDir)) {
    //            Files.createDirectories(docDir);
    //        }
    //        if (!Files.isWritable(docDir)) {
    //            throw new IOException("Lucene index directory is not writable, please check the path; " + docDir);
    //        }
    //
    //        //TODO check .close()
    //        Directory dir = FSDirectory.open(docDir);
    //        Analyzer analyzer = new StandardAnalyzer();
    //        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
    //
    //        final boolean create = false;
    //        if (create) {
    //            // Create a new index in the directory, removing any
    //            // previously indexed documents:
    //            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
    //        }
    //        else {
    //            // Add new documents to an existing index:
    //            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    //        }
    //
    //        try {
    //            SearchIntegratorBuilder searchIntegratorBuilder = new SearchIntegratorBuilder();
    //            SearchConfiguration config = new LuceneSearchConfiguration();
    //            searchIntegratorBuilder.configuration(config);
    //            ExtendedSearchIntegrator searchIntegrator = (ExtendedSearchIntegrator) searchIntegratorBuilder.buildSearchIntegrator();
    //
    //            PageStub entity = new PageStub();
    //            //DocumentId idAnn = entity.getClass().getAnnotation(DocumentId.class);
    //            Serializable id = entity.getId();
    //
    //            //See MoreLikeThisBuilder
    //            //TODO should we keep the fieldToAnalyzerMap around to pass to the analyzer?
    //            Map<String, String> fieldToAnalyzerMap = new HashMap<String, String>();
    //            //FIXME by calling documentBuilder we don't honor .comparingField("foo").ignoreFieldBridge(): probably not a problem in practice though
    //            DocumentBuilderIndexedEntity docBuilder = searchIntegrator.getIndexBinding(PageStub.class).getDocumentBuilder();
    //            Document doc = docBuilder.getDocument(null, entity, id, fieldToAnalyzerMap, null, new ContextualExceptionBridgeHelper(), null);
    //
    //            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
    //                writer.updateDocument(new Term("id", id.toString()), doc);
    //                //writer.addDocument(doc);
    //            }
    //
    //
    //            try (IndexReader reader = DirectoryReader.open(dir)) {
    //                IndexSearcher searcher = new IndexSearcher(reader);
    //
    //                int numDocs = reader.numDocs();
    //                for ( int i = 0; i < numDocs; i++) {
    //                    Document d = reader.document(i);
    //                    System.out.println( "d=" +d);
    //                }
    //
    //                Query q = new QueryParser("firstName", analyzer).parse("TEST");
    //                int hitsPerPage = 10;
    //                TopDocs docs = searcher.search(q, hitsPerPage);
    //                TopDocs docs2 = searcher.search(new FieldValueQuery("firstName"), hitsPerPage);
    //                ScoreDoc[] hits = docs.scoreDocs;
    //                ScoreDoc[] hits2 = docs2.scoreDocs;
    //                System.out.println("Found " + hits.length + " hits.");
    //                for (int i = 0; i < hits.length; ++i) {
    //                    int docId = hits[i].doc;
    //                    Document d = searcher.doc(docId);
    //                    System.out.println((i + 1) + ". " + d.get("id") + "\t" + d.get("firstName"));
    //                }
    //            }
    //        }
    //        catch (Exception e) {
    //            Logger.setRollbackOnly("Error ", e);
    //        }
    //
    //        return Response.ok().build();
    //    }

    //    @Path("infinispan")
    //    public Response testInfinispan()
    //    {
    //        Cache<String, PageStub> m_cache = MyObjectCacheFactory.getMyObjectCache();
    //
    //        int searchNumber = 7;
    //        SearchManager searchManager = Search.getSearchManager(m_cache );
    //        QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(PageStub.class).get();
    //        Query luceneQuery = queryBuilder.keyword().onField("searchNumber").matching(searchNumber).createQuery();
    //        CacheQuery cacheQuery = searchManager.getQuery(luceneQuery, PageStub.class );
    //
    //        //noinspection unchecked
    //        List result = (List)cacheQuery.list();
    //
    //
    //        return Response.ok().build();
    //    }
    //    @Path("hibernate-search")
    //    public Response testHibernateSearch()
    //    {
    //        EntityManager entityManager = new BlocksEntityManager();
    //        FullTextEntityManager fullTextEntityManager = org.hibernate.search.jpa.Search.getFullTextEntityManager(entityManager);
    //
    //        fullTextEntityManager.getTransaction().begin();
    //
    //        fullTextEntityManager.index(new PageStub());
    //
    //        fullTextEntityManager.getTransaction().commit();
    //        fullTextEntityManager.close();
    //
    //
    ////        Configuration cfg = new Configuration();
    ////        cfg.setProperty("hibernate.dialect", "com.beligum.blocks.fs.indexes.hibernate.BlocksDialect");
    ////        cfg.setProperty("hibernate.search.default.directory_provider", "filesystem");
    ////        cfg.setProperty("hibernate.search.default.indexBase", Settings.instance().getPageMainIndexFolder().getAbsolutePath());
    ////
    ////        List classes = Collections.singletonList(PageStub.class);
    ////        SessionFactory sessionFactory = cfg.buildSessionFactory();
    ////        Session session = sessionFactory.openSession();
    ////
    ////        session.close();
    //
    //        return Response.ok().build();
    //    }

}

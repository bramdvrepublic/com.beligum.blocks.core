package com.beligum.blocks.filesystem.index;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.entries.IndexEntry;
import com.beligum.blocks.filesystem.index.entries.pages.IndexSearchResult;
import com.beligum.blocks.filesystem.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.filesystem.index.entries.pages.SparqlIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.Indexer;
import com.beligum.blocks.filesystem.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexConnection;
import com.beligum.blocks.filesystem.index.ifaces.SparqlQueryConnection;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.utils.RdfTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.TermQuery;
import org.openrdf.model.*;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.QueryResults;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailGraphQuery;
import org.openrdf.repository.sail.SailRepositoryConnection;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by bram on 2/21/16.
 */
public class SesamePageIndexConnection extends AbstractIndexConnection implements PageIndexConnection, SparqlQueryConnection
{
    //-----CONSTANTS-----
    public static final String SPARQL_SUBJECT_BINDING_NAME = "s";
    public static final String SPARQL_PREDICATE_BINDING_NAME = "p";
    public static final String SPARQL_OBJECT_BINDING_NAME = "o";

    //decide if we build a simple boolean lucene query or build a bitmap (by using TermsQuery) when searching for subjectURIs
    //Note: I think it's better to disable this for smaller (expected) sets and enable it when you're expecting large results...
    //Note2: pfff, need to try with bigger sets before making up my mind, true seems to be a bit faster in the end...
    private enum FetchPageMethod
    {
        BULK_BOOLEAN_QUERY,
        BULK_BITMAP_QUERY,
        SINGLE_TERM_QUERY
    }

    //-----VARIABLES-----
    private SesamePageIndexer pageIndexer;
    private TX transaction;
    private boolean registeredTransaction;
    private SailRepositoryConnection connection;
    private FetchPageMethod fetchPageMethod;
    private ExecutorService fetchPageExecutor;
    private boolean active;

    //-----CONSTRUCTORS-----
    public SesamePageIndexConnection(SesamePageIndexer pageIndexer, TX transaction) throws IOException
    {
        this.pageIndexer = pageIndexer;
        this.transaction = transaction;
        this.registeredTransaction = false;

        try {
            this.connection = pageIndexer.getRDFRepository().getConnection();
        }
        catch (RepositoryException e) {
            throw new IOException("Error occurred while booting sesame page indexer transaction", e);
        }

        this.fetchPageMethod = FetchPageMethod.SINGLE_TERM_QUERY;

        //we'll fetch the single lucene results in a separate thread
        if (this.fetchPageMethod == FetchPageMethod.SINGLE_TERM_QUERY) {
            //this.fetchPageExecutor = Executors.newSingleThreadExecutor();
            //this.fetchPageExecutor = Executors.newCachedThreadPool();
            //this.fetchPageExecutor = Executors.newWorkStealingPool();
            this.fetchPageExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }

        this.active = true;
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexEntry get(URI key) throws IOException
    {
        this.assertActive();

        SparqlIndexEntry retVal = null;

        URI subject = key;
        if (!subject.isAbsolute()) {
            subject = R.configuration().getSiteDomain().resolve(subject);
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("PREFIX ").append(Settings.instance().getRdfOntologyPrefix()).append(": <").append(Settings.instance().getRdfOntologyUri()).append("> \n");
        queryBuilder.append("\n");
        queryBuilder.append("CONSTRUCT")
                    .append(" WHERE {\n")
                    .append("\t").append("<").append(subject.toString()).append(">")
                    .append(" ?").append(SPARQL_PREDICATE_BINDING_NAME)
                    .append(" ?").append(SPARQL_OBJECT_BINDING_NAME)
                    .append(" .\n")
                    .append("}")
        ;

        //TODO maybe it's better to iterate the predicates once and build a HashMap instead of pointing to the model?
        //Depends on where we're going with the other functions below, we need to implemente a general OO RDF Mapper
        SailGraphQuery query = connection.prepareGraphQuery(QueryLanguage.SPARQL, queryBuilder.toString(), R.configuration().getSiteDomain().toString());
        Model resultModel = QueryResults.asModel(query.evaluate());
        if (!resultModel.isEmpty()) {
            retVal = new SparqlIndexEntry(subject.toString(), resultModel);
        }

        // SELECT alternative...
        //
        //        StringBuilder queryBuilder = new StringBuilder();
        //        queryBuilder.append("PREFIX ").append(Settings.instance().getRdfOntologyPrefix()).append(": <").append(Settings.instance().getRdfOntologyUri()).append("> \n");
        //        queryBuilder.append("\n");
        //        queryBuilder.append("SELECT")
        //                    .append(" ?").append(SPARQL_PREDICATE_BINDING_NAME)
        //                    .append(" ?").append(SPARQL_OBJECT_BINDING_NAME)
        //                    .append(" WHERE {\n")
        //                    .append("\t").append("<").append(subject.toString()).append(">")
        //                    .append(" ?").append(SPARQL_PREDICATE_BINDING_NAME)
        //                    .append(" ?").append(SPARQL_OBJECT_BINDING_NAME)
        //                    .append(" .\n")
        //                    .append("}")
        //        ;
        //
        //        TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder.toString(), R.configuration().getSiteDomain().toString());
        //        try (TupleQueryResult result = query.evaluate()) {
        //            while (result.hasNext()) {
        //                Value predicate = result.next().getValue(SPARQL_PREDICATE_BINDING_NAME);
        //                Value object = result.next().getValue(SPARQL_OBJECT_BINDING_NAME);
        //            }
        //        }

        return retVal;
    }
    @Override
    public void delete(Resource resource) throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        Page page = resource.unwrap(Page.class);

        //we'll be deleting all the triples in the model of the page,
        //except the ones that are present in another language.
        Model pageModel = this.queryModel(page, false);
        for (Page p : page.getTranslations().values()) {
            Model model = p.readRdfModel();
            if (model != null) {
                pageModel.removeAll(model);
            }
        }

        this.connection.remove(pageModel);
    }
    @Override
    public void update(Resource resource) throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        Page page = resource.unwrap(Page.class);

        this.delete(page);
        this.connection.add(page.readRdfModel());
    }
    @Override
    public void deleteAll() throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        this.connection.clear();
    }
//    Commented out because the Lucene Sail is disabled for now...
//    @Override
//    public IndexSearchResult search(RdfClass type, String luceneQuery, Map fieldValues, RdfProperty sortField, boolean sortAscending, int pageSize, int pageOffset, Locale language) throws IOException
//    {
//        this.assertActive();
//
//        //see http://rdf4j.org/doc/4/programming.docbook?view#The_Lucene_SAIL
//        //and maybe the source code: org.openrdf.sail.lucene.LuceneSailSchema
//        StringBuilder queryBuilder = new StringBuilder();
//        final String searchPrefix = "search";
//        queryBuilder.append("PREFIX ").append(Settings.instance().getRdfOntologyPrefix()).append(": <").append(Settings.instance().getRdfOntologyUri()).append("> \n");
//        queryBuilder.append("PREFIX ").append(searchPrefix).append(": <").append(LuceneSailSchema.NAMESPACE).append("> \n");
//        queryBuilder.append("\n");
//
//        //links the resource to be found with the following query statements (required)
//        queryBuilder.append("SELECT DISTINCT ?").append(SPARQL_SUBJECT_BINDING_NAME).append(" WHERE {\n");
//
//        //TODO implement the type
//        if (type != null) {
//            queryBuilder.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" a <").append(type.getFullName().toString()).append("> . \n");
//        }
//
//        //---Lucene---
//        if (!StringUtils.isEmpty(luceneQuery)) {
//            queryBuilder.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ").append(searchPrefix).append(":matches [\n")
//                        //specifies the Lucene query (required)
//                        .append("\t").append("\t").append(searchPrefix).append(":query \"").append(QueryParser.escape(luceneQuery)).append("*").append("\";\n")
//                        //specifies the property to search. If omitted all properties are searched (optional)
//                        //                    .append("\t").append("\t").append(searchPrefix).append(":property ").append(Settings.instance().getRdfOntologyPrefix()).append(":").append("streetName").append(";\n")
//                        //specifies a variable for the score (optional)
//                        //                    .append("\t").append("\t").append(searchPrefix).append(":score ?score;\n")
//                        //specifies a variable for a highlighted snippet (optional)
//                        //.append("\t").append("\t").append(searchPrefix).append(":snippet ?snippet;\n")
//                        .append("\t").append("] .\n");
//        }
//
//        //---triple selection---
//        queryBuilder.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ?").append(SPARQL_PREDICATE_BINDING_NAME).append(" ?").append(SPARQL_OBJECT_BINDING_NAME).append(" .\n");
//
//        //---Filters---
//        if (fieldValues != null) {
//            Set<Map.Entry<RdfProperty, String>> entries = fieldValues.entrySet();
//            for (Map.Entry<RdfProperty, String> filter : entries) {
//                queryBuilder.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ").append(filter.getKey().getCurieName().toString()).append(" ")
//                            .append(filter.getValue()).append(" .\n");
//            }
//        }
//
//        //---Save the sort field---
//        if (sortField != null) {
//            queryBuilder.append("\t").append("OPTIONAL{ ?").append(SPARQL_SUBJECT_BINDING_NAME).append(" <").append(sortField.getFullName().toString()).append("> ")
//                        .append("?sortField").append(" . }\n");
//        }
//
//        //---Closes the inner SELECT---
//        queryBuilder.append("}\n");
//
//        //---Sorting---
//        if (sortField != null) {
//            queryBuilder.append("ORDER BY ").append(sortAscending ? "ASC(" : "DESC(").append("?sortField").append(")").append("\n");
//        }
//        //note that, for pagination to work properly, we need to sort the results, so always add a sort field.
//        // eg see here: https://lists.w3.org/Archives/Public/public-rdf-dawg-comments/2011Oct/0024.html
//        else {
//            queryBuilder.append("ORDER BY ").append(sortAscending ? "ASC(" : "DESC(").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(")").append("\n");
//        }
//
//        //---Paging---
//        queryBuilder.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(pageOffset).append("\n");
//
//        return this.search(queryBuilder.toString(), language);
//    }
    @Override
    public IndexSearchResult search(String sparqlQuery, Locale language) throws IOException
    {
        this.assertActive();

        long searchStart = System.currentTimeMillis();
        long sparqlTime = 0;
        long parseTime = 0;
        long luceneTime = 0;
        long end = 0;

        IndexSearchResult retVal = new IndexSearchResult(new ArrayList<>());

        List<Term> ids = null;
        org.apache.lucene.search.BooleanQuery luceneIdQuery = null;
        switch (fetchPageMethod) {
            case BULK_BOOLEAN_QUERY:
                luceneIdQuery = new org.apache.lucene.search.BooleanQuery();
                break;
            case BULK_BITMAP_QUERY:
                ids = new ArrayList<>();
                break;
            case SINGLE_TERM_QUERY:
                //NOOP: results go straight to retVal
                break;
        }

        int count = 0;
        String siteDomain = R.configuration().getSiteDomain().toString();
        String resourceField = PageIndexEntry.Field.resource.name();
        String languageStr = language == null ? null : language.getLanguage();
        //Connect with the page indexer here so we can re-use the connection for all results
        LuceneQueryConnection luceneConnection = StorageFactory.getMainPageQueryConnection();

        TupleQuery query = this.query(sparqlQuery);
        try (TupleQueryResult result = query.evaluate()) {
            sparqlTime = System.currentTimeMillis();

            //watch out: order must be preserved (eg. because we're likely to use sorting or scoring)
            while (result.hasNext()) {

                //we expect only one binding: the subject URI
                Value subject = result.next().getValue(SPARQL_SUBJECT_BINDING_NAME);

                if (subject instanceof IRI) {
                    //small optimization to avoid all the URI parsing, hope I didn't break something...
                    //String subjectStr = this.toUri((IRI)subjectIRI, false, true).toString();
                    String subjectStr = subject.stringValue().substring(siteDomain.length());
                    if (subjectStr.charAt(0) != '/') {
                        subjectStr = "/" + subjectStr;
                    }

                    switch (fetchPageMethod) {
                        case BULK_BOOLEAN_QUERY:
                            luceneIdQuery.add(new TermQuery(new Term(PageIndexEntry.Field.resource.name(), subjectStr)), BooleanClause.Occur.SHOULD);
                            break;
                        case BULK_BITMAP_QUERY:
                            ids.add(new Term(resourceField, subjectStr));
                            break;
                        case SINGLE_TERM_QUERY:
                            this.fetchPageExecutor.submit(new IndexLoader(subjectStr, languageStr, luceneConnection, retVal.getResults(), count));
                            break;
                    }

                    count++;
                }
                else {
                    Logger.warn("Skipping incompatible sparql result because it's subject is no IRI; " + subject);
                }
            }
        }
        parseTime = System.currentTimeMillis();

        if (fetchPageMethod != FetchPageMethod.SINGLE_TERM_QUERY) {
            if (count > 0) {
                org.apache.lucene.search.BooleanQuery luceneQuery = new org.apache.lucene.search.BooleanQuery();
                switch (fetchPageMethod) {
                    case BULK_BOOLEAN_QUERY:
                        luceneQuery.add(luceneIdQuery, BooleanClause.Occur.MUST);
                        break;
                    case BULK_BITMAP_QUERY:
                        luceneQuery.add(new TermsQuery(ids), BooleanClause.Occur.MUST);
                        break;
                }
                if (languageStr != null) {
                    luceneQuery.add(new TermQuery(new Term(PageIndexEntry.Field.language.name(), languageStr)), BooleanClause.Occur.MUST);
                }

                retVal = luceneConnection.search(luceneQuery, count);
            }
        }
        else {
            if (this.fetchPageExecutor != null) {
                //request a shutdown
                this.fetchPageExecutor.shutdown();

                //allow for some time to end
                try {
                    this.fetchPageExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e) {
                    Logger.warn("Watch out: wait time for index-fetcher timed out; this shouldn't happen", e);
                }
                finally {
                    //NOOP: force close check now done in close()
                }
            }
        }

        luceneTime = System.currentTimeMillis();
        end = System.currentTimeMillis();

        //        Logger.info("Search took " + (end - searchStart) + "ms to complete with " + this.fetchPageMethod);
        //        Logger.info("SPARQL took " + (sparqlTime - searchStart) + "ms to complete.");
        //        Logger.info("Parse took " + (parseTime - sparqlTime) + "ms to complete.");
        //        Logger.info("Lucene took " + (luceneTime - parseTime) + "ms to complete.");
        //        Logger.info("\n");

        return retVal;
    }
    @Override
    public TupleQuery query(String sparqlQuery) throws IOException
    {
        this.assertActive();

        return this.connection.prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery, R.configuration().getSiteDomain().toString());
    }
    @Override
    public void close() throws IOException
    {
        if (this.fetchPageExecutor != null) {
            //graceful wait-time is implemented in the search
            this.fetchPageExecutor.shutdownNow();
            this.fetchPageExecutor = null;
        }

        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }

        this.registeredTransaction = false;
        this.transaction = null;
    }

    //-----PROTECTED METHODS-----
    @Override
    protected void begin() throws IOException
    {
        if (this.connection != null) {
            this.connection.begin();
        }
    }
    @Override
    protected void prepareCommit() throws IOException
    {
        if (this.connection != null) {
            //Note: see connection.commit() for the nitty-gritty of where I got this from
            this.connection.getSailConnection().flush();
            this.connection.getSailConnection().prepare();
        }
    }
    @Override
    protected void commit() throws IOException
    {
        if (this.connection != null) {
            this.connection.getSailConnection().commit();
        }
    }
    @Override
    protected void rollback() throws IOException
    {
        if (this.connection != null) {
            this.connection.getSailConnection().rollback();
        }
    }
    @Override
    protected Indexer getResourceManager()
    {
        return this.pageIndexer;
    }

    //-----PRIVATE METHODS-----
    private synchronized void assertActive() throws IOException
    {
        if (!this.active) {
            throw new IOException("Can't proceed, an active Sesame index connection was asserted");
        }
    }
    private synchronized void assertTransaction() throws IOException
    {
        if (this.transaction == null) {
            throw new IOException("Transaction asserted, but none was initialized, can't continue");
        }
        else {
            //only need to do it once (at the beginning of a method using a tx)
            if (!this.registeredTransaction) {
                //attach this connection to the transaction manager
                this.transaction.registerResource(this);
                this.registeredTransaction = true;
            }
        }
    }
    private URI toUri(Value value, boolean tryLocalRdfCurie, boolean tryLocalDomain)
    {
        URI retVal = null;

        if (value != null) {
            if (value instanceof IRI) {
                retVal = URI.create(value.stringValue());
                if (tryLocalRdfCurie) {
                    //if it's not absolute (eg. it doesn't start with http://..., this means the relativize 'succeeded' and the retVal starts with the RDF ontology URI)
                    URI curie = RdfTools.fullToCurie(retVal);
                    if (curie != null) {
                        retVal = curie;
                    }
                }
                else if (tryLocalDomain) {
                    retVal = RdfTools.relativizeToLocalDomain(retVal);
                }
            }
        }

        return retVal;
    }
    /**
     * Note: untested (saved for future reference)
     */
    private Model queryModel(Page page, boolean excludeTranslations) throws IOException
    {
        IRI publicPageIri = this.connection.getValueFactory().createIRI(page.getPublicAbsoluteAddress().toString());
        //note that RDF requires absolute urls
        URI resourceUri = URI.create(page.createAnalyzer().getHtmlAbout().value);
        if (!resourceUri.isAbsolute()) {
            resourceUri = R.configuration().getSiteDomain().resolve(resourceUri);
        }
        IRI resourceIri = this.connection.getValueFactory().createIRI(resourceUri.toString());

        //find all statements that have the public URI as a subject
        Model model = QueryResults.asModel(this.connection.getStatements(publicPageIri, null, null));
        //find all statements that have the internal resource URI as a subject
        model.addAll(QueryResults.asModel(this.connection.getStatements(resourceIri, null, null)));

        //remove all literals in other languages
        if (excludeTranslations) {
            Iterator<Statement> iter = model.iterator();
            while (iter.hasNext()) {
                Statement stmt = iter.next();
                if (stmt.getObject() instanceof Literal) {
                    Literal literal = (Literal) stmt.getObject();
                    if (literal.getLanguage().isPresent() && !literal.getLanguage().get().equals(page.getLanguage().getLanguage())) {
                        iter.remove();
                    }
                }
            }
        }

        return model;
    }

    //-----INNER CLASSES-----
    private class IndexLoader implements Runnable
    {
        private String subject;
        private String language;
        private LuceneQueryConnection luceneConnection;
        private final List<IndexEntry> retVal;
        private final int index;

        public IndexLoader(String subject, String language, LuceneQueryConnection luceneConnection, List<IndexEntry> retVal, int index)
        {
            this.subject = subject;
            this.language = language;
            this.luceneConnection = luceneConnection;
            this.retVal = retVal;
            this.index = index;
        }

        @Override
        public void run()
        {
            try {
                org.apache.lucene.search.BooleanQuery pageQuery = new org.apache.lucene.search.BooleanQuery();
                pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.resource.name(), this.subject)), BooleanClause.Occur.MUST);
                if (this.language != null) {
                    pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.language.name(), this.language)), BooleanClause.Occur.MUST);
                }
                List<IndexEntry> page = luceneConnection.search(pageQuery, 1).getResults();

                if (!page.isEmpty()) {
                    retVal.add(page.iterator().next());
                }
                else {
                    Logger.warn("Watch out: encountered a SPARQL result without a matching page index entry; this shouldn't happen; " + this.subject);
                }
            }
            catch (IOException e) {
                Logger.error("Error while fetching page from Lucene index; " + this.subject, e);
            }
        }
    }
}
package com.beligum.blocks.fs.index;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.fs.index.entries.pages.IndexSearchResult;
import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.Indexer;
import com.beligum.blocks.fs.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.index.ifaces.SparqlQueryConnection;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.utils.RdfTools;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsQuery;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.TermQuery;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.lucene.LuceneSailSchema;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by bram on 2/21/16.
 */
public class SesamePageIndexerConnection extends AbstractIndexConnection implements PageIndexConnection, SparqlQueryConnection
{
    //-----CONSTANTS-----
    public static final String SPARQL_SUBJECT_BINDING_NAME = "s";

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
    private SailRepositoryConnection connection;
    private FetchPageMethod fetchPageMethod;
    private ExecutorService fetchPageExecutor;

    //-----CONSTRUCTORS-----
    public SesamePageIndexerConnection(SesamePageIndexer pageIndexer) throws IOException
    {
        this.pageIndexer = pageIndexer;

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
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexEntry get(URI key) throws IOException
    {
        //TODO
        String queryString = "PREFIX search:   <" + LuceneSailSchema.NAMESPACE + "> \n" +
                             "SELECT ?x ?score ?snippet WHERE {?x search:matches [\n" +
                             "search:query \"aimé\"; \n" +
                             "search:property <http://www.mot.be/ontology/comment>; \n" +
                             "search:score ?score; \n" +
                             "search:snippet ?snippet ] }";

        TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

        try (TupleQueryResult result = query.evaluate()) {
            if (!result.hasNext()) {
                System.out.println("-------- NO MATCHES!! ---------");
            }
            else {
                System.out.println("-------- FOUND MATCHES ---------");
                // print the results
                while (result.hasNext()) {
                    BindingSet bindings = result.next();
                    System.out.println("found match: ");
                    for (Binding binding : bindings) {
                        System.out.println(" " + binding.getName() + ": " + binding.getValue());
                    }
                }
            }
        }

        return null;
    }
    @Override
    public void delete(Page page) throws IOException
    {
        this.assertTransaction();

        //TODO tricky stuff, what do we delete?
        //First idea: we should delete all statements in the page, excluding the statements that are present in translations...
    }
    @Override
    public void update(Page page) throws IOException
    {
        this.assertTransaction();

        this.connection.add(page.readRdfModel());
    }
    @Override
    public void deleteAll() throws IOException
    {
        this.assertTransaction();

        this.connection.clear();
    }
    @Override
    public IndexSearchResult search(RdfClass type, String luceneQuery, Map fieldValues, RdfProperty sortField, boolean sortAscending, int pageSize, int pageOffset, Locale language) throws IOException
    {
        //see http://rdf4j.org/doc/4/programming.docbook?view#The_Lucene_SAIL
        //and maybe the source code: org.openrdf.sail.lucene.LuceneSailSchema
        StringBuilder queryBuilder = new StringBuilder();
        final String searchPrefix = "templates/search";
        queryBuilder.append("PREFIX ").append(Settings.instance().getRdfOntologyPrefix()).append(": <").append(Settings.instance().getRdfOntologyUri()).append("> \n");
        queryBuilder.append("PREFIX ").append(searchPrefix).append(": <").append(LuceneSailSchema.NAMESPACE).append("> \n");
        queryBuilder.append("\n");

        //links the resource to be found with the following query statements (required)
        queryBuilder.append("SELECT DISTINCT ?").append(SPARQL_SUBJECT_BINDING_NAME).append(" WHERE {\n");

        //TODO implement the type
        if (type != null) {
            queryBuilder.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" a <").append(type.getFullName().toString()).append("> . \n");
        }

        //---Lucene---
        if (!StringUtils.isEmpty(luceneQuery)) {
            queryBuilder.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ").append(searchPrefix).append(":matches [\n")
                        //specifies the Lucene query (required)
                        .append("\t").append("\t").append(searchPrefix).append(":query \"").append(QueryParser.escape(luceneQuery)).append("*").append("\";\n")
                        //specifies the property to search. If omitted all properties are searched (optional)
                        //                    .append("\t").append("\t").append(searchPrefix).append(":property ").append(Settings.instance().getRdfOntologyPrefix()).append(":").append("streetName").append(";\n")
                        //specifies a variable for the score (optional)
                        //                    .append("\t").append("\t").append(searchPrefix).append(":score ?score;\n")
                        //specifies a variable for a highlighted snippet (optional)
                        //.append("\t").append("\t").append(searchPrefix).append(":snippet ?snippet;\n")
                        .append("\t").append("] .\n");
        }

        //---triple selection---
        queryBuilder.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ?p ?o").append(" .\n");

        //---Filters---
        if (fieldValues != null) {
            Set<Map.Entry<RdfProperty, String>> entries = fieldValues.entrySet();
            for (Map.Entry<RdfProperty, String> filter : entries) {
                queryBuilder.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ").append(filter.getKey().getCurieName().toString()).append(" ")
                            .append(filter.getValue()).append(" .\n");
            }
        }

        //---Save the sort field---
        if (sortField != null) {
            queryBuilder.append("\t").append("OPTIONAL{ ?").append(SPARQL_SUBJECT_BINDING_NAME).append(" <").append(sortField.getFullName().toString()).append("> ")
                        .append("?sortField").append(" . }\n");
        }

        //---Closes the inner SELECT---
        queryBuilder.append("}\n");

        //---Sorting---
        if (sortField != null) {
            queryBuilder.append("ORDER BY ").append(sortAscending ? "ASC(" : "DESC(").append("?sortField").append(")").append("\n");
        }
        //note that, for pagination to work properly, we need to sort the results, so always add a sort field.
        // eg see here: https://lists.w3.org/Archives/Public/public-rdf-dawg-comments/2011Oct/0024.html
        else {
            queryBuilder.append("ORDER BY ").append(sortAscending ? "ASC(" : "DESC(").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(")").append("\n");
        }

        //---Paging---
        queryBuilder.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(pageOffset).append("\n");

        return this.search(queryBuilder.toString(), type, language);
    }
    @Override
    public IndexSearchResult search(String sparqlQuery, RdfClass type, Locale language) throws IOException
    {
        long searchStart = System.currentTimeMillis();
        long sparqlTime = 0;
        long parseTime = 0;
        long luceneTime = 0;

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
        String languageStr = language.getLanguage();
        //Connect with the page indexer here so we can re-use the connection for all results
        LuceneQueryConnection luceneConnection = StorageFactory.getMainPageQueryConnection();

        TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery, siteDomain);
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
                luceneQuery.add(new TermQuery(new Term(PageIndexEntry.Field.language.name(), languageStr)), BooleanClause.Occur.MUST);

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

        long end = System.currentTimeMillis();
        Logger.info("Search took " + (end - searchStart) + "ms to complete with " + this.fetchPageMethod);
        Logger.info("SPARQL took " + (sparqlTime - searchStart) + "ms to complete.");
        Logger.info("Parse took " + (parseTime - sparqlTime) + "ms to complete.");
        Logger.info("Lucene took " + (luceneTime - parseTime) + "ms to complete.");
        Logger.info("\n");

        return retVal;
    }
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
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void assertTransaction() throws IOException
    {
        //attach this connection to the transaction manager
        StorageFactory.getCurrentRequestTx().registerResource(this);
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
                    if (curie!=null) {
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
                pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.language.name(), this.language)), BooleanClause.Occur.MUST);
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

/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.index.sparql;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.server.R;
import com.beligum.blocks.index.AbstractIndexConnection;
import com.beligum.blocks.index.ifaces.*;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.templating.blocks.analyzer.HtmlAnalyzer;
import com.beligum.blocks.utils.RdfTools;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailGraphQuery;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailTupleQuery;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by bram on 2/21/16.
 */
public class SesamePageIndexConnection extends AbstractIndexConnection implements IndexConnection
{
    //-----CONSTANTS-----
    public enum QueryFormat implements IndexConnection.QueryFormat
    {
        /**
         * A SPARQL (v1.1) SELECT query
         */
        SPARQL11_SELECT,
        /**
         * A SPARQL (v1.1) CONSTRUCT query
         */
        SPARQL11_CONSTRUCT
    }

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
    private SailRepositoryConnection connection;
    private FetchPageMethod fetchPageMethod;
    private ExecutorService fetchPageExecutor;

    //-----CONSTRUCTORS-----
    SesamePageIndexConnection(SesamePageIndexer pageIndexer, String txResourceName, boolean forceTx) throws IOException
    {
        super(pageIndexer.getTxFactory(), txResourceName);

        // note: bootConnection() will call begin() on success, so make sure everything is in place before calling it
        this.pageIndexer = pageIndexer;

        // this is the 'real' connection we're wrapping
        try {
            this.connection = this.pageIndexer.getRDFRepository().getConnection();
        }
        catch (RepositoryException e) {
            throw new IOException(e);
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

        // if we need to force a TX (for tread lock-owning reasons), boot one right here, right now
        if (forceTx) {
            this.assertTransactional();
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public ResourceIndexEntry get(URI key) throws IOException
    {
        this.assertActive();

        ResourceIndexEntry retVal = null;

        URI subject = key;
        if (!subject.isAbsolute()) {
            subject = R.configuration().getSiteDomain().resolve(subject);
        }

        StringBuilder queryBuilder = new StringBuilder();

        SparqlIndexSearchRequest.addOntologyPrefixes(queryBuilder);

        queryBuilder.append("CONSTRUCT")
                    .append(" WHERE {\n")
                    .append("\t").append("<").append(subject.toString()).append(">").append(" ?").append(SPARQL_PREDICATE_BINDING_NAME).append(" ?").append(SPARQL_OBJECT_BINDING_NAME).append(" .\n")
                    .append("}")
        ;

        //TODO maybe it's better to iterate the predicates once and build a HashMap instead of pointing to the model?
        //Depends on where we're going with the other functions below, we need to implemente a general OO RDF Mapper
        SparqlIndexConstructResult results = this.constructSearch(queryBuilder.toString());

        //let's return null if nothing was found
        if (!results.isEmpty()) {
            //TODO we should refactor this so it uses the iterator of the results
            retVal = new SparqlConstructIndexEntry(subject, results.getModel());
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
    public synchronized void delete(Resource resource) throws IOException
    {
        this.assertActive();
        this.assertTransactional();

        Page page = resource.unwrap(Page.class);

        //we'll create one analyzer, so we can re-use it instead of re-creating it for all uses
        HtmlAnalyzer pageAnalyzer = page.createAnalyzer();

        //we'll be deleting all the triples in the model of the page,
        //except the ones that are present in another language.
        //Note that by relying on the entries of the triple store itself (instead of reading in the page model from disk)
        // we ensure all occurrences of this page in the DB will be deleted, regardless of what is present in the proxy model
        Model pageModel = this.queryModel(page, pageAnalyzer, false);
        for (Page p : page.getTranslations(pageAnalyzer).values()) {
            Model model = p.readRdfModel();
            if (model != null) {
                pageModel.removeAll(model);
            }
        }

        //TODO we should probably consider deleting the dependency triples if we're the last resource that uses them,
        // but for now, this isn't a major issue since the links to it will be deleted by the code below

        this.connection.remove(pageModel);
    }
    @Override
    public synchronized void update(Resource resource) throws IOException
    {
        this.assertActive();
        this.assertTransactional();

        Page page = resource.unwrap(Page.class);

        //Make sure the update of this page wipes all previous triples first
        //Note: this needs some further explanation, because it has some tricky side-effects:
        // The delete method works like this: read the RDF model from disk, substract it with the
        // RDF models from all existing translations and what is left is safe to delete.
        // However, to search all translations of a page, we need access to the information in the
        // lucene index, because the pages are tied using a back-end resource URI, not using the filesystem
        // (eg. indeed, sometimes, we want to translate the public URI of a page for SEO purposes).
        // This means, eg. during reindexing, we need a lucene-indexed page to be able to search for
        // it's translations...
        // The bottom line is that we always need to (re)index the Lucene index before we (re)index
        // the triplestore.
        this.delete(page);

        //add the base model to the triple store
        Model rdfModel = page.readRdfModel();
        this.connection.add(rdfModel);

        //add the dependency model to the triple store
        Model rdfDepModel = page.readRdfDependenciesModel();
        if (rdfDepModel != null) {
            this.connection.add(rdfDepModel);
        }

        //Note: old code, can be used to detect the presence of a single subject IRI
        //        StringBuilder queryBuilder = new StringBuilder();
        //        queryBuilder.append("PREFIX ").append(Settings.instance().getRdfOntologyPrefix()).append(": <").append(Settings.instance().getRdfOntologyUri()).append("> \n");
        //        queryBuilder.append("\n");
        //        queryBuilder.append("SELECT")
        //                    //.append(" ?").append(SPARQL_SUBJECT_BINDING_NAME)
        //                    .append(" ?").append(SPARQL_PREDICATE_BINDING_NAME)
        //                    .append(" ?").append(SPARQL_OBJECT_BINDING_NAME)
        //                    .append(" WHERE {\n");
        //        //filter on subject
        //        queryBuilder.append("\t").append("<").append(rdfUri).append(">").append(" ?").append(SPARQL_PREDICATE_BINDING_NAME).append(" ?").append(SPARQL_OBJECT_BINDING_NAME).append(" . \n");
        //        //close the where statement
        //        queryBuilder.append("}\n");
        //
        //        TupleQuery query = StorageFactory.getSparqlQueryConnection().query(queryBuilder.toString());
        //        boolean exists;
        //        try (TupleQueryResult queryResult = query.evaluate()) {
        //            exists = queryResult.hasNext();
        //        }
        //
        //        //this means there's no single subject with the selected URI in our local triplestore
        //        if (!exists) {
    }
    @Override
    public synchronized void deleteAll() throws IOException
    {
        this.assertActive();
        this.assertTransactional();

        this.connection.clear();
    }
    @Override
    public synchronized void close() throws IOException
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
    @Override
    public IndexSearchResult<ResourceIndexEntry> search(IndexSearchRequest indexSearchRequest) throws IOException
    {
        this.assertActive();

        if (!(indexSearchRequest instanceof SparqlIndexSearchRequest)) {
            throw new IOException("Encountered unsupported index search request object; this shouldn't happen" + indexSearchRequest);
        }
        else {
            try {
                return this.search(((SparqlIndexSearchRequest) indexSearchRequest).buildSparqlQuery(), QueryFormat.SPARQL11_SELECT);
            }
            catch (Exception e) {
                throw new IOException("Error while executing a Solr search; " + indexSearchRequest, e);
            }
        }
    }
    @Override
    public <T extends IndexSearchResult> T search(String query, IndexConnection.QueryFormat format) throws IOException
    {
        this.assertActive();

        T retVal = null;

        if (format instanceof QueryFormat) {

            // From the RDF4j docs:
            // Three types of SPARQL queries are distinguished:
            //   1) tuple queries The result of a tuple query is a set of tuples (or variable bindings),
            //      where each tuple represents a solution of a query. This type of query is commonly used
            //      to get specific values (URIs, blank nodes, literals) from the stored RDF data. SPARQL
            //      SELECT queries are tuple queries.
            //   2) graph queries The result of graph queries is an RDF graph (or set of statements).
            //      This type of query is very useful for extracting sub-graphs from the stored RDF data,
            //      which can then be queried further, serialized to an RDF document, etc. SPARQL
            //      CONSTRUCT and DESCRIBE queries are graph queries.
            //   3) boolean queries The result of boolean queries is a simple boolean value, i.e. true or
            //      false. This type of query can be used to check if a repository contains specific
            //      information. SPARQL ASK queries are boolean queries.
            switch ((QueryFormat) format) {
                case SPARQL11_SELECT:

                    retVal = (T) this.selectSearch(query);

                    break;
                case SPARQL11_CONSTRUCT:

                    retVal = (T) this.constructSearch(query);

                    break;
                default:
                    throw new IOException("Encountered unsupported query format; " + format);
            }
        }
        else {
            throw new IOException("Encountered unsupported query format; " + format);
        }

        return retVal;
    }
    //    @Override
    //    public IndexSearchResult search(String sparqlQuery, Locale language) throws IOException
    //    {
    //        this.assertActive();
    //
    //        long searchStart = System.currentTimeMillis();
    //        long sparqlTime = 0;
    //        long parseTime = 0;
    //        long luceneTime = 0;
    //        long end = 0;
    //
    //        List<Term> ids = null;
    //        List<IndexEntry> tempResults = null;
    //        org.apache.lucene.search.BooleanQuery luceneIdQuery = null;
    //        switch (fetchPageMethod) {
    //            case BULK_BOOLEAN_QUERY:
    //                luceneIdQuery = new org.apache.lucene.search.BooleanQuery();
    //                break;
    //            case BULK_BITMAP_QUERY:
    //                ids = new ArrayList<>();
    //                break;
    //            case SINGLE_TERM_QUERY:
    //                tempResults = new ArrayList<>();
    //                break;
    //        }
    //
    //        int count = 0;
    //        String siteDomain = R.configuration().getSiteDomain().toString();
    //        String resourceField = PageIndexEntry.resource.name();
    //        String languageStr = language == null ? null : language.getLanguage();
    //        //Connect with the page indexer here so we can re-use the connection for all results
    //        JsonPageIndexerConnection luceneConnection = StorageFactory.getJsonQueryConnection();
    //
    //        TupleQuery query = this.query(sparqlQuery);
    //        try (TupleQueryResult result = query.evaluate()) {
    //            sparqlTime = System.currentTimeMillis();
    //
    //            //watch out: order must be preserved (eg. because we're likely to use sorting or scoring)
    //            while (result.hasNext()) {
    //
    //                //we expect only one binding: the subject URI
    //                Value subject = result.next().getValue(SPARQL_SUBJECT_BINDING_NAME);
    //
    //                if (subject instanceof IRI) {
    //                    //small optimization to avoid all the URI parsing, hope I didn't break something...
    //                    //String subjectStr = this.toUri((IRI)subjectIRI, false, true).toString();
    //                    String subjectStr = subject.stringValue().substring(siteDomain.length());
    //                    if (subjectStr.charAt(0) != '/') {
    //                        subjectStr = "/" + subjectStr;
    //                    }
    //
    //                    switch (fetchPageMethod) {
    //                        case BULK_BOOLEAN_QUERY:
    //                            luceneIdQuery.add(new TermQuery(new Term(PageIndexEntry.resource.name(), subjectStr)), BooleanClause.Occur.SHOULD);
    //                            break;
    //                        case BULK_BITMAP_QUERY:
    //                            ids.add(new Term(resourceField, subjectStr));
    //                            break;
    //                        case SINGLE_TERM_QUERY:
    //                            this.fetchPageExecutor.submit(new IndexLoader(subjectStr, languageStr, luceneConnection, tempResults, count));
    //                            break;
    //                    }
    //
    //                    count++;
    //                }
    //                else {
    //                    Logger.warn("Skipping incompatible sparql result because it's subject is no IRI; " + subject);
    //                }
    //            }
    //        }
    //        parseTime = System.currentTimeMillis();
    //
    //        IndexSearchResult retVal;
    //        if (fetchPageMethod != FetchPageMethod.SINGLE_TERM_QUERY) {
    //            if (count > 0) {
    //                org.apache.lucene.search.BooleanQuery luceneQuery = new org.apache.lucene.search.BooleanQuery();
    //                switch (fetchPageMethod) {
    //                    case BULK_BOOLEAN_QUERY:
    //                        luceneQuery.add(luceneIdQuery, BooleanClause.Occur.MUST);
    //                        break;
    //                    case BULK_BITMAP_QUERY:
    //                        luceneQuery.add(new TermsQuery(ids), BooleanClause.Occur.MUST);
    //                        break;
    //                }
    //                if (languageStr != null) {
    //                    luceneQuery.add(new TermQuery(new Term(PageIndexEntry.language.name(), languageStr)), BooleanClause.Occur.MUST);
    //                }
    //
    //                retVal = luceneConnection.search(luceneQuery, count);
    //            }
    //            else {
    //                retVal = new SimpleIndexSearchResult(new ArrayList<>());
    //            }
    //        }
    //        else {
    //            if (this.fetchPageExecutor != null) {
    //                //request a shutdown
    //                this.fetchPageExecutor.shutdown();
    //
    //                //allow for some time to end
    //                try {
    //                    this.fetchPageExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
    //                }
    //                catch (InterruptedException e) {
    //                    Logger.warn("Watch out: wait time for index-fetcher timed out; this shouldn't happen", e);
    //                }
    //                finally {
    //                    retVal = new SimpleIndexSearchResult(tempResults);
    //                    //NOOP: force close check now done in close()
    //                }
    //            }
    //            else {
    //                retVal = new SimpleIndexSearchResult(new ArrayList<>());
    //            }
    //        }
    //
    //        luceneTime = System.currentTimeMillis();
    //        end = System.currentTimeMillis();
    //
    //        //        Logger.info("Search took " + (end - searchStart) + "ms to complete with " + this.fetchPageMethod);
    //        //        Logger.info("SPARQL took " + (sparqlTime - searchStart) + "ms to complete.");
    //        //        Logger.info("Parse took " + (parseTime - sparqlTime) + "ms to complete.");
    //        //        Logger.info("Lucene took " + (luceneTime - parseTime) + "ms to complete.");
    //        //        Logger.info("\n");
    //
    //        return retVal;
    //    }

    //-----PROTECTED METHODS-----
    @Override
    protected void begin() throws IOException
    {
        if (this.isTransactional()) {
            this.connection.begin();
        }
    }
    @Override
    protected void prepareCommit() throws IOException
    {
        if (this.isTransactional()) {
            //Note: see connection.commit() for the nitty-gritty of where I got this from
            this.connection.getSailConnection().flush();
            this.connection.getSailConnection().prepare();
        }
    }
    @Override
    protected void commit() throws IOException
    {
        if (this.isTransactional()) {
            this.connection.getSailConnection().commit();
        }
    }
    @Override
    protected void rollback() throws IOException
    {
        if (this.isTransactional()) {
            this.connection.getSailConnection().rollback();
        }
    }
    @Override
    protected Indexer getResourceManager()
    {
        return this.pageIndexer;
    }

    //-----PRIVATE METHODS-----
    private SparqlIndexSelectResult selectSearch(String query) throws IOException
    {
        SparqlIndexSelectResult retVal = null;

        long startStamp = System.currentTimeMillis();

        SailTupleQuery selectQuery = this.connection.prepareTupleQuery(QueryLanguage.SPARQL, query, R.configuration().getSiteDomain().toString());
        try (TupleQueryResult result = selectQuery.evaluate()) {
            retVal = new SparqlIndexSelectResult(result, System.currentTimeMillis() - startStamp);
        }

        return retVal;
    }
    private SparqlIndexConstructResult constructSearch(String query) throws IOException
    {
        SparqlIndexConstructResult retVal = null;

        long startStamp = System.currentTimeMillis();

        SailGraphQuery constructQuery = this.connection.prepareGraphQuery(QueryLanguage.SPARQL, query, R.configuration().getSiteDomain().toString());
        try (GraphQueryResult result = constructQuery.evaluate()) {
            retVal = new SparqlIndexConstructResult(result, System.currentTimeMillis() - startStamp);
        }

        return retVal;
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
     * Search all relevant triples that have something to do with a certain page (only used for deletion for now)
     */
    private Model queryModel(Page page, HtmlAnalyzer pageAnalyzer, boolean excludeTranslations) throws IOException
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

        //Since we support sub-resources, we need to add all sub-resource triples as well
        //Note that we can do this a couple of ways: by querying the index, by analyzing the html, ...
        //Because the index is also queried to find the translations, we opted for the fastest method
        //and use the index to find all sub-resources for this page.
        for (URI subResourceUri : page.getSubResources(pageAnalyzer)) {
            if (!subResourceUri.isAbsolute()) {
                subResourceUri = R.configuration().getSiteDomain().resolve(subResourceUri);
            }
            IRI subResourceIri = this.connection.getValueFactory().createIRI(subResourceUri.toString());

            //this will search the store for all references TO our sub resource (generally only yielding one result that links the parent and the sub together)
            model.addAll(QueryResults.asModel(this.connection.getStatements(null, null, subResourceIri)));
            //search for all information IN our sub resource
            model.addAll(QueryResults.asModel(this.connection.getStatements(subResourceIri, null, null)));
        }

        return model;
    }

    //-----INNER CLASSES-----
    //    private class IndexLoader implements Runnable
    //    {
    //        private String subject;
    //        private String language;
    //        private JsonPageIndexerConnection luceneConnection;
    //        private final List<IndexEntry> retVal;
    //        private final int index;
    //
    //        public IndexLoader(String subject, String language, JsonPageIndexerConnection luceneConnection, List<IndexEntry> retVal, int index)
    //        {
    //            this.subject = subject;
    //            this.language = language;
    //            this.luceneConnection = luceneConnection;
    //            this.retVal = retVal;
    //            this.index = index;
    //        }
    //
    //        @Override
    //        public void run()
    //        {
    //            try {
    //                org.apache.lucene.search.BooleanQuery pageQuery = new org.apache.lucene.search.BooleanQuery();
    //                pageQuery.add(new TermQuery(new Term(PageIndexEntry.resource.name(), this.subject)), BooleanClause.Occur.MUST);
    //                if (this.language != null) {
    //                    pageQuery.add(new TermQuery(new Term(PageIndexEntry.language.name(), this.language)), BooleanClause.Occur.MUST);
    //                }
    //                IndexSearchResult results = luceneConnection.search(pageQuery, 1);
    //                if (results.size() > 0) {
    //                    retVal.add(results.iterator().next());
    //                }
    //                else {
    //                    Logger.warn("Watch out: encountered a SPARQL result without a matching page index entry; this shouldn't happen; " + this.subject);
    //                }
    //            }
    //            catch (IOException e) {
    //                Logger.error("Error while fetching page from Lucene index; " + this.subject, e);
    //            }
    //        }
    //    }
}

package com.beligum.blocks.fs.index;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.fs.index.entries.resources.ResourceIndexEntry;
import com.beligum.blocks.fs.index.entries.resources.SimpleResourceIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.index.ifaces.SparqlQueryConnection;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfResource;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.openrdf.model.*;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.lucene.LuceneSailSchema;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

/**
 * Created by bram on 2/21/16.
 */
public class SesamePageIndexerConnection extends AbstractIndexConnection implements PageIndexConnection, SparqlQueryConnection
{
    //-----CONSTANTS-----
    private static final URI ROOT = URI.create("/");

    //-----VARIABLES-----
    private SailRepositoryConnection connection;
    private boolean transactional;

    //-----CONSTRUCTORS-----
    public SesamePageIndexerConnection(SailRepository repository) throws IOException
    {
        try {
            this.connection = repository.getConnection();
        }
        catch (RepositoryException e) {
            throw new IOException("Error occurred while booting sesame page indexer transaction", e);
        }

        this.transactional = false;
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
    }
    @Override
    public void update(Page page) throws IOException
    {
        this.assertTransaction();

        Model model = null;

        //explicitly read the model from disk so we can use this indexer stand alone
        Importer rdfImporter = page.createImporter(page.getRdfExportFileFormat());
        try (InputStream is = page.getResourcePath().getFileContext().open(page.getRdfExportFile())) {
            model = rdfImporter.importDocument(is, page.buildRelativeAddress());
        }

        this.connection.add(model);
    }
    @Override
    public List<ResourceIndexEntry> search(RdfClass type, String luceneQuery, Map fieldValues, String sortField, boolean sortAscending, int pageSize, int pageOffset, Locale language) throws IOException
    {
        //see http://rdf4j.org/doc/4/programming.docbook?view#The_Lucene_SAIL
        //and maybe the source code: org.openrdf.sail.lucene.LuceneSailSchema
        StringBuilder queryBuilder = new StringBuilder();
        final String searchPrefix = "search";
        queryBuilder.append("PREFIX ").append(Settings.instance().getRdfOntologyPrefix()).append(": <").append(Settings.instance().getRdfOntologyUri()).append("> \n");
        queryBuilder.append("PREFIX ").append(searchPrefix).append(": <").append(LuceneSailSchema.NAMESPACE).append("> \n");
        queryBuilder.append("\n");

        //links the resource to be found with the following query statements (required)
        queryBuilder.append("SELECT DISTINCT ?s").append(" WHERE {\n");

        //TODO implement the type
        if (type != null) {
            queryBuilder.append("\t").append("?s a <").append(type.getFullName().toString()).append("> . \n");
        }

        //---Lucene---
        if (!StringUtils.isEmpty(luceneQuery)) {
            queryBuilder.append("\t").append("?s ").append(searchPrefix).append(":matches [\n")
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
        queryBuilder.append("\t").append("?s ?p ?o").append(" .\n");

        //---Filters---
        if (fieldValues != null) {
            Set<Map.Entry<String, String>> entries = fieldValues.entrySet();
            for (Map.Entry<String, String> filter : entries) {
                queryBuilder.append("\t").append("?s ").append(Settings.instance().getRdfOntologyPrefix()).append(":").append(filter.getKey()).append(" ").append(filter.getValue()).append(" .\n");
            }
        }

        //---Save the sort field---
        if (!StringUtils.isEmpty(sortField)) {
            queryBuilder.append("\t").append("?s ").append(Settings.instance().getRdfOntologyPrefix()).append(":").append(sortField).append(" ").append("?sortField").append(" .\n");
        }

        //---Closes the inner SELECT---
        queryBuilder.append("}\n");

        //---Sorting---
        if (!StringUtils.isEmpty(sortField)) {
            queryBuilder.append("ORDER BY ").append(sortAscending ? "ASC(" : "DESC(").append("?sortField").append(")").append("\n");
        }
        //note that, for pagination to work properly, we need to sort the results, so always add a sort field.
        // eg see here: https://lists.w3.org/Archives/Public/public-rdf-dawg-comments/2011Oct/0024.html
        else {
            queryBuilder.append("ORDER BY ").append(sortAscending ? "ASC(" : "DESC(").append("?s").append(")").append("\n");
        }

        //---Paging---
        queryBuilder.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(pageOffset).append("\n");

        //        int testLimit = 10;
        //        String testQuery = "CONSTRUCT {\n" +
        //                           "    ?s ?p ?o\n" +
        //                           "}\n" +
        //                           "where {\n" +
        //                           "    ?s ?p ?o .\n" +
        //                           "    {\n" +
        //                           "        select distinct ?s where {?s ?p ?o} limit "+testLimit+"\n" +
        //                           "    }\n" +
        //                           "}";
        //        GraphQueryResult graphResult = connection.prepareGraphQuery(QueryLanguage.SPARQL, testQuery).evaluate();
        //        Logger.info("---------------------");
        //        while (graphResult.hasNext()) {
        //            Statement st = graphResult.next();
        //            Logger.info(st.getObject().toString());
        //        }
        //        Logger.info("---------------------");

        //        // wrap in an object repository
        //        ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
        //        ObjectRepository repository = factory.createRepository(this.connection.getRepository());
        //
        //        ObjectConnection con = repository.getConnection();
        //
        //        // retrieve a Document by id
        //        ValueFactory vf = con.getValueFactory();
        //        org.openrdf.model.URI id = vf.createURI("http://example.com/data/2012/getting-started");
        //        AlibabaTest doc = con.getObject(AlibabaTest.class, id);
        //
        //        // close everything down
        //        con.close();
        //        repository.shutDown();

        return this.search(queryBuilder.toString(), type, language);
    }
    @Override
    public List<ResourceIndexEntry> search(String sparqlQuery, RdfClass type, Locale language) throws IOException
    {
        List<ResourceIndexEntry> retVal = new ArrayList<>();

        TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery, Settings.instance().getSiteDomain().toString());

        try (TupleQueryResult result = query.evaluate()) {
            List<String> bindingNames = result.getBindingNames();

            //watch out: order must be preserved (eg. because we're likely to use sorting or scoring)
            while (result.hasNext()) {

                BindingSet bindingSet = result.next();
                //we expect only one binding: the subject URI
                Value subject = bindingSet.getValue(bindingNames.get(0));

                if (subject instanceof IRI) {
                    //this will query the triplestore to build up the properties list
                    retVal.add(this.buildResourceEntry((IRI) subject, type, language));
                }
                else {
                    Logger.warn("Skipping incompatible sparql result because it's subject is no IRI; " + subject);
                }
            }
        }

        return retVal;
    }
    @Override
    protected void begin() throws IOException
    {
        if (this.transactional && this.connection != null) {
            this.connection.begin();
        }
    }
    @Override
    protected void prepareCommit() throws IOException
    {
        if (this.transactional && this.connection != null) {
            //Note: see connection.commit() for the nitty-gritty of where I got this from
            this.connection.getSailConnection().flush();
            this.connection.getSailConnection().prepare();
        }
    }
    @Override
    protected void commit() throws IOException
    {
        if (this.transactional && this.connection != null) {
            this.connection.getSailConnection().commit();
        }
    }
    @Override
    protected void rollback() throws IOException
    {
        if (this.transactional && this.connection != null) {
            this.connection.getSailConnection().rollback();
        }
    }
    @Override
    public void close() throws IOException
    {
        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void assertTransaction() throws IOException
    {
        if (!this.transactional) {
            //attach this connection to the transaction manager
            StorageFactory.getCurrentRequestTx().registerResource(this);
            this.transactional = true;
        }
    }
    private URI toUri(Value value, boolean tryLocalRdfCurie, boolean tryLocalDomain)
    {
        URI retVal = null;

        if (value != null) {
            if (value instanceof IRI) {
                retVal = URI.create(value.stringValue());
                if (tryLocalRdfCurie) {
                    URI relative = Settings.instance().getRdfOntologyUri().relativize(retVal);
                    //if it's not absolute (eg. it doesn't start with http://..., this means the relativize 'succeeded' and the retVal starts with the RDF ontology URI)
                    if (!relative.isAbsolute()) {
                        retVal = URI.create(Settings.instance().getRdfOntologyPrefix() + ":" + relative.toString());
                    }
                }
                else if (tryLocalDomain) {
                    retVal = relativizeLocalDomain(retVal);
                }
            }
        }

        return retVal;
    }
    private URI relativizeLocalDomain(URI uri)
    {
        URI retVal = uri;

        URI relative = Settings.instance().getSiteDomain().relativize(retVal);
        //if it's not absolute (eg. it doesn't start with http://..., this means the relativize 'succeeded' and the retVal starts with the RDF ontology URI)
        if (!relative.isAbsolute()) {
            retVal = ROOT.resolve(relative);
        }

        return retVal;
    }
    private ResourceIndexEntry buildResourceEntry(IRI resourceId, RdfClass type, Locale language) throws IOException
    {
        Class<? extends ResourceIndexEntry> resourceIndexClass = type.getResourceIndexClass();
        if (resourceIndexClass==null) {
            resourceIndexClass = SimpleResourceIndexEntry.class;
        }

        ResourceIndexEntry retVal = null;
        try {
            //we convert to a uniform Java-URI so it's compatible with existing interfaces later on
            retVal = resourceIndexClass.getConstructor(URI.class).newInstance(this.toUri(resourceId, false, true));
        }
        catch (Exception e) {
            throw new IOException("Error while initializing a resource index entry class '"+resourceIndexClass.getCanonicalName()+"', you probably want to check it's constructor signature", e);
        }

        RepositoryResult<Statement> properties = this.connection.getStatements(resourceId, null, null);
        while (properties.hasNext()) {
            Statement statement = properties.next();

            Logger.info("\t"+statement);

            //Note that we need a CURIE for RdfFactory.getForResourceType(), so first convert the absolute predicate URI to a curie URI
            RdfVocabulary vocab = RdfFactory.getVocabularies().get(URI.create(statement.getPredicate().getNamespace()));
            if (vocab!=null) {
                URI predicateResourceUri = vocab.resolveCurie(statement.getPredicate().getLocalName());
                //RdfResource predicateResource = RdfFactory.getForResourceType(this.toUri(statement.getPredicate(), true, false));
                RdfResource predicateResource = RdfFactory.getForResourceType(predicateResourceUri);
                if (predicateResource != null) {
                    //we only add literals that match the passed-down language (disabled when null)
                    boolean skip = false;
                    if (language != null && statement.getObject() instanceof Literal) {
                        Literal object = (Literal) statement.getObject();
                        if (object.getLanguage().isPresent()) {
                            skip = !object.getLanguage().get().equals(language.getLanguage());
                        }
                    }

                    if (!skip) {
                        Value oldProperty = retVal.getProperties().put(predicateResource, statement.getObject());
                        if (oldProperty != null) {
                            Logger.warn("Watch out, overwriting an existing (and thus double) predicate '" + statement.getPredicate() + "' for resource; " + resourceId);
                        }
                    }
                }
                else {
                    Logger.warn("Skipping incompatible sparql result because it's predicate is not a known RDF class; " + statement.getPredicate());
                }
            }
            else {
                Logger.warn("Skipping incompatible sparql result because it's namespace is not in a known RDF vocabulary; " + statement.getPredicate());
            }
        }

        return retVal;
    }
}

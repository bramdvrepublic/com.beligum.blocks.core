package com.beligum.blocks.fs.index;

import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Importer;
import org.apache.lucene.search.Query;
import org.openrdf.model.Model;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.lucene.LuceneSailSchema;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bram on 2/21/16.
 */
public class SesamePageIndexerConnection extends AbstractIndexConnection implements PageIndexConnection
{
    //-----CONSTANTS-----

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
    public List search(FieldQuery[] fieldQueries, int maxResults) throws IOException
    {
        List<PageIndexEntry> retVal = new ArrayList<>();

        //TODO

        return retVal;
    }
    @Override
    public List search(Query luceneQuery, int maxResults) throws IOException
    {
        List<PageIndexEntry> retVal = new ArrayList<>();

        //TODO

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
    private void doTest()
    {
        //JUST TESTING
        // search for all resources that mention "person"
        String queryString = "PREFIX search:   <" + LuceneSailSchema.NAMESPACE + "> \n" +
                             "PREFIX mot:   <http://www.mot.be/ontology/> \n" +
                             "SELECT ?x ?score ?snippet WHERE {?x search:matches [\n" +
                             "search:query \"aimé\"; \n" +
                             "search:property mot:comment; \n" +
                             "search:score ?score; \n" +
                             "search:snippet ?snippet ] }";
        System.out.println("Running query: \n" + queryString);
        TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = query.evaluate();
        try {
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
        finally {
            result.close();
        }
    }
}

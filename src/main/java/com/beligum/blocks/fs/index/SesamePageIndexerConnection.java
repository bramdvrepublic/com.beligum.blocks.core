package com.beligum.blocks.fs.index;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Importer;
import org.openrdf.model.Model;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by bram on 2/21/16.
 */
public class SesamePageIndexerConnection extends AbstractIndexConnection implements PageIndexConnection
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private SailRepositoryConnection connection;

    //-----CONSTRUCTORS-----
    public SesamePageIndexerConnection(SailRepository repository) throws IOException
    {
        try {
            this.connection = repository.getConnection();
        }
        catch (RepositoryException e) {
            throw new IOException("Error occurred while booting sesame page indexer transaction", e);
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexEntry get(URI key) throws IOException
    {
        return null;
    }
    @Override
    public void delete(Page page) throws IOException
    {

    }
    @Override
    public void indexPage(Page page) throws IOException
    {
        Model model = null;

        Importer rdfImporter = page.createImporter(page.getRdfExportFileFormat());
        try (InputStream is = page.getResourcePath().getFileContext().open(page.getRdfExportFile())) {
            model = rdfImporter.importDocument(is, page.buildAddress());
        }

        Logger.info("done");

        //
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
        //        try {
        //            Repository rep = new SailRepository(new MemoryStore());
        //            rep.initialize();
        //
        //
        //        }
        //        catch (RepositoryException e) {
        //            throw new IOException(e);
        //        }
    }
    @Override
    protected void begin() throws IOException
    {
        this.connection.begin();
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
    public void close() throws IOException
    {
        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

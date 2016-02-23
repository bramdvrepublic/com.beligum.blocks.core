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

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;
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
    private Object transactionDummy;

    //-----CONSTRUCTORS-----
    public SesamePageIndexerConnection(SailRepository repository) throws IOException
    {
        try {
            this.connection = repository.getConnection();
            this.connection.begin();

            this.transactionDummy = new Object();
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
    public void prepareCommit(Xid xid) throws XAException
    {
        if (this.connection != null && this.transactionDummy != null) {
            try {
                //Note: see connection.commit() for the nitty-gritty of where I got this from
                this.connection.getSailConnection().flush();
                this.connection.getSailConnection().prepare();

                //prevent the transaction from bein used again
                this.transactionDummy = null;
            }
            catch (Exception e) {
                throw new XAException("Error occurred while preparing a commit for a sesame page indexer transaction; " + (e == null ? null : e.getMessage()));
            }
        }
    }
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException
    {
        if (this.connection != null && this.transactionDummy != null) {
            try {
                this.connection.getSailConnection().commit();
                //prevent the transaction from bein used again
                this.transactionDummy = null;
            }
            catch (Exception e) {
                throw new XAException("Error occurred while committing sesame page indexer transaction; " + (e == null ? null : e.getMessage()));
            }
        }
    }
    @Override
    public void rollback(Xid xid) throws XAException
    {
        if (this.connection != null && this.transactionDummy != null) {
            try {
                this.connection.getSailConnection().rollback();
                //prevent the transaction from bein used again
                this.transactionDummy = null;
            }
            catch (Exception e) {
                throw new XAException("Error occurred while rolling back sesame page indexer transaction" + (e == null ? null : e.getMessage()));
            }
        }
    }
    @Override
    public void close() throws Exception
    {
        if (this.transactionDummy != null) {
            this.rollback(null);
            throw new IOException("Open transaction found while closing infinispan page index connection; rolled back the transaction just to be safe...");
        }
        else {
            if (this.connection != null) {
                this.connection.close();
                this.connection = null;
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

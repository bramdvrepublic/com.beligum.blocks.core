package com.beligum.blocks.fs.index;

import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.hadoop.fs.FileContext;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 2/21/16.
 */
public class SesamePageIndexConnection implements PageIndexConnection
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private RepositoryConnection connection;
    private Object transactionDummy;

    //-----CONSTRUCTORS-----
    public SesamePageIndexConnection(Repository repository) throws IOException
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
        FileContext fc = page.getResourcePath().getFileContext();
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
    public void commit() throws IOException
    {
        if (this.connection!=null && this.transactionDummy!=null) {
            try {
                this.connection.commit();
                //prevent the transaction from bein used again
                this.transactionDummy = null;
            }
            catch (Exception e) {
                throw new IOException("Error occurred while committing sesame page indexer transaction", e);
            }
        }
    }
    @Override
    public void rollback() throws IOException
    {
        if (this.connection!=null && this.transactionDummy!=null) {
            try {
                this.connection.rollback();
                //prevent the transaction from bein used again
                this.transactionDummy = null;
            }
            catch (Exception e) {
                throw new IOException("Error occurred while rolling back sesame page indexer transaction", e);
            }
        }
    }
    @Override
    public void close() throws Exception
    {
        if (this.transactionDummy!=null) {
            this.rollback();
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

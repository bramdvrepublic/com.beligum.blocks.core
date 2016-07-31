package com.beligum.blocks.fs.index;

import com.beligum.base.server.R;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.index.ifaces.PageIndexer;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.lucene.LuceneSail;
import org.openrdf.sail.nativerdf.NativeStore;

import java.io.IOException;

/**
 * Created by bram on 1/26/16.
 * <p/>
 * Some interesting reads:
 * <p/>
 * https://jena.apache.org/documentation/tdb/java_api.html
 */
public class SesamePageIndexer implements PageIndexer
{
    //-----CONSTANTS-----
    private static final String DATA_SUBDIR = "data";
    private static final String INDEX_SUBDIR = "index";

    //-----VARIABLES-----
    private Object repositoryLock;

    //-----CONSTRUCTORS-----
    public SesamePageIndexer()
    {
        this.repositoryLock = new Object();
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexConnection connect() throws IOException
    {
        return new SesamePageIndexerConnection(this);
    }
    @Override
    public void shutdown() throws IOException
    {
        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.TRIPLESTORE_ENGINE)) {
            try {
                this.getRDFRepository().shutDown();
            }
            catch (RepositoryException e) {
                throw new IOException("Error while shutting down sesame page indexer", e);
            }
        }
    }
    public SailRepository getRDFRepository() throws IOException
    {
        synchronized (this.repositoryLock) {
            if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.TRIPLESTORE_ENGINE)) {

                try {
                    //create the repository for the linked data
                    NativeStore dataRepo = new NativeStore(Settings.instance().getPageTripleStoreFolder().resolve(DATA_SUBDIR).toFile());

                    //create the repository for the lucene index
                    LuceneSail indexRepo = new LuceneSail();
                    indexRepo.setParameter(LuceneSail.LUCENE_DIR_KEY, Settings.instance().getPageTripleStoreFolder().resolve(SesamePageIndexer.INDEX_SUBDIR).toFile().getAbsolutePath());

                    //link both together
                    indexRepo.setBaseSail(dataRepo);

                    //build and init the main, wrapped repository
                    SailRepository mainRepo = new SailRepository(indexRepo);
                    mainRepo.initialize();

                    R.cacheManager().getApplicationCache().put(CacheKeys.TRIPLESTORE_ENGINE, mainRepo);
                }
                catch (RepositoryException e) {
                    throw new IOException("Error while initializing the sesame page indexer", e);
                }
            }

            return (SailRepository) R.cacheManager().getApplicationCache().get(CacheKeys.TRIPLESTORE_ENGINE);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

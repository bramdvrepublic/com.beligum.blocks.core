package com.beligum.blocks.fs.index;

import com.beligum.base.server.R;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.index.ifaces.PageIndexer;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
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
        return new SesamePageIndexerConnection(this.getRDFRepository());
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

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private SailRepository getRDFRepository() throws IOException
    {
        synchronized (this.repositoryLock) {
            if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.TRIPLESTORE_ENGINE)) {

                try {
                    SailRepository repo = new SailRepository(new NativeStore(Settings.instance().getPageTripleStoreFolder()));
                    repo.initialize();

                    R.cacheManager().getApplicationCache().put(CacheKeys.TRIPLESTORE_ENGINE, repo);
                }
                catch (RepositoryException e) {
                    throw new IOException("Error while initializing the sesame page indexer", e);
                }
            }

            return (SailRepository) R.cacheManager().getApplicationCache().get(CacheKeys.TRIPLESTORE_ENGINE);
        }
    }
}

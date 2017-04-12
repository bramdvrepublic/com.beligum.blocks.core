package com.beligum.blocks.filesystem.index;

import com.beligum.base.server.R;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexConnection;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexer;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.lucene.LuceneSail;
import org.openrdf.sail.nativerdf.NativeStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    public synchronized PageIndexConnection connect() throws IOException
    {
        return this.connect(StorageFactory.getCurrentRequestTx());
    }
    @Override
    public synchronized PageIndexConnection connect(TX tx) throws IOException
    {
        return new SesamePageIndexConnection(this, tx);
    }
    @Override
    public synchronized void shutdown() throws IOException
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
                    final java.nio.file.Path tsDir = Paths.get(Settings.instance().getPageTripleStoreFolder());
                    if (!Files.exists(tsDir)) {
                        Files.createDirectories(tsDir);
                    }

                    //create the repository for the linked data
                    final java.nio.file.Path dataDir = tsDir.resolve(DATA_SUBDIR);
                    if (!Files.exists(dataDir)) {
                        Files.createDirectories(dataDir);
                    }
                    NativeStore dataRepo = new NativeStore(dataDir.toFile());

                    //create the repository for the lucene index
                    LuceneSail indexRepo = new LuceneSail();
                    final java.nio.file.Path indexDir = tsDir.resolve(INDEX_SUBDIR);
                    if (!Files.exists(indexDir)) {
                        Files.createDirectories(indexDir);
                    }
                    indexRepo.setParameter(LuceneSail.LUCENE_DIR_KEY, indexDir.toFile().getAbsolutePath());

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

package com.beligum.blocks.filesystem.index.reindex;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.utils.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by bram on 11/05/17.
 */
public abstract class ReindexTask implements Runnable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI resourceUri;
    private ResourceRepository repository;
    private ResourceRepository.IndexOption indexConnectionsOption;
    private List<String> deleteQueries;
    private String dbTableName;
    private String dbIdColumn;
    private long dbId;
    private long[] reindexCounter;
    private AtomicBoolean cancelThread;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public void init(URI resourceUri, ResourceRepository repository, ResourceRepository.IndexOption indexConnectionsOption, List<String> deleteQueries, String dbTableName, String dbIdColumn, long dbId, long[] reindexCounter, AtomicBoolean cancelThread)
    {
        this.resourceUri = resourceUri;
        this.repository = repository;
        this.indexConnectionsOption = indexConnectionsOption;
        this.deleteQueries = deleteQueries;
        this.dbTableName = dbTableName;
        this.dbIdColumn = dbIdColumn;
        this.dbId = dbId;
        this.reindexCounter = reindexCounter;
        this.cancelThread = cancelThread;
    }
    @Override
    public void run()
    {
        if (!cancelThread.get()) {
            try {
                //Logger.info("Reindexing " + pageUri);

                //request the page directly, so we don't go through the resource cache
                Resource resource = this.repository.get(this.repository.request(this.resourceUri, null));

                //execute the real work
                this.runTaskFor(resource, this.indexConnectionsOption);

                //if reindexation succeeded, we add the delete stmt to the list
                deleteQueries.add("DELETE FROM " + this.dbTableName +
                                  " WHERE " + this.dbIdColumn + "=" + this.dbId + ";");
            }
            catch (Throwable e) {
                //let's signal we should end the processing as soon one error occurs
                cancelThread.set(true);
                Logger.error("Error while reindexing " + resourceUri, e);
            }
            finally {
                this.reindexCounter[0]++;
            }
        }
    }

    //-----PROTECTED METHODS-----
    protected abstract void runTaskFor(Resource resource, ResourceRepository.IndexOption indexConnectionsOption) throws IOException;

    //-----PRIVATE METHODS-----

}

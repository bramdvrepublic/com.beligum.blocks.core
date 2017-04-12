package com.beligum.blocks.filesystem.index.ifaces;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.blocks.filesystem.index.entries.IndexEntry;

import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 2/21/16.
 */
public interface IndexConnection extends XAResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * Fetch the index entry for the supplied URI from the underlying index store
     */
    IndexEntry get(URI key) throws IOException;

    /**
     * (re-)index the supplied resource into the underlying index store
     */
    void update(Resource resource) throws IOException;

    /**
     * Remove the index entry of the supplied resource from the underlying index store
     */
    void delete(Resource resource) throws IOException;

    /**
     * Remove all entries from the underlying index store and start over
     */
    void deleteAll() throws IOException;

    /**
     * Free all resources held by this connection. Called at the very end of each request transaction.
     * Note that we explicitly don't implement AutoClosable because we don't want to hint users to close
     * the connections themselves.
     */
    void close() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

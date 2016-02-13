package com.beligum.blocks.fs.pages.ifaces;

import com.beligum.base.auth.models.Person;
import com.beligum.blocks.rdf.sources.HtmlSource;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 1/14/16.
 */
public interface PageStore
{
    //-----PUBLIC METHODS-----
    /**
     * This should walk the file system and initialize it so we always start with
     * a clean file system. Only called once during startup.
     *
     * @throws IOException
     */
    void init() throws IOException;

    /**
     * Saves the supplied source to the page store.
     * This method is fully transactional and rolls back automatically if an error happens.
     * Note that the entire page is stored, including all proxies and metadata, so it can take a while to complete (depending on the size/complexity).
     *
     * @param source the html of the page to store
     * @param creator the logged-in user that is creating this page
     * @return the newly created page or null if the page already existed, but nothing changed
     * @throws IOException
     */
    Page save(HtmlSource source, Person creator) throws IOException;

    /**
     * Deletes the page with the supplied address.
     * This method is fully transactional and rolls back automatically if an error happens.
     * Note that in the current implementation, the meta folder is kept for future reference (and possible undo)
     *
     * @param uri the address of the page to delete
     * @param deleter the logged-in user that is deleting this page
     * @return the page that was deleted
     * @throws IOException
     */
    Page delete(URI uri, Person deleter) throws IOException;
}

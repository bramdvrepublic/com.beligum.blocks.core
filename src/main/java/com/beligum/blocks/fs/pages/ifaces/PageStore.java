package com.beligum.blocks.fs.pages.ifaces;

import com.beligum.base.auth.models.Person;
import com.beligum.blocks.rdf.sources.HtmlSource;
import org.apache.hadoop.fs.PathFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

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
     * Search the page with the supplied address.
     *
     * @param publicAddress the address of the page to fetch
     * @param readOnly should the page be opened read-only or read-write?
     * @return the page or null if no such page was found.
     * @throws IOException
     */
    Page get(URI publicAddress, boolean readOnly) throws IOException;

    /**
     * Get an iterator over all the stored pages in this system.
     *
     * @param readOnly should the pages be opened read-only or read-write?
     * @return the iterator over all the stored pages in this system
     * @throws IOException
     */
    Iterator<Page> getAll(boolean readOnly, String relativeStartFolder, PathFilter filter) throws IOException;

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
     * @param publicAddress the address of the page to delete
     * @param deleter the logged-in user that is deleting this page
     * @return the page that was deleted
     * @throws IOException
     */
    Page delete(URI publicAddress, Person deleter) throws IOException;
}

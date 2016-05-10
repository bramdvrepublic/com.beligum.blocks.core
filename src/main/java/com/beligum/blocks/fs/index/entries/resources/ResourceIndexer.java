package com.beligum.blocks.fs.index.entries.resources;

import org.openrdf.model.Model;

/**
 * Class used to pull some general information (like title, description, image, link, ...) from a resource class to be used to render search results.
 * This is used while indexing a page to be able to render search results from the index without having to look up the whole page.
 *
 * Created by bram on 5/9/16.
 */
public interface ResourceIndexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Parse the supplied model and return the generated index that was extracted from it
     */
    ResourceIndexEntry index(Model model);

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

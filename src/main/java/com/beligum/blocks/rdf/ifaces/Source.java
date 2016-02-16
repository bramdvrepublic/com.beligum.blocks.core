package com.beligum.blocks.rdf.ifaces;

import org.apache.hadoop.fs.FileContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by bram on 1/23/16.
 */
public interface Source
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * @return the main URI where this source came from. It's also the base context for all semantic operations. Should be as specific as possible.
     */
    URI getBaseUri();

    /**
     * Prepare this source (perform all required processing) for saving to it's final destination.
     */
    void prepareForSaving(boolean adjustLanguage, boolean compact) throws IOException;

    /**
     * Search and update the parent page of this source (using the given filesystem) and save the result.
     */
    void updateParent(FileContext fs) throws IOException;

    /**
     * Process all translations of this source (using the given filesystem) and save the resulting translation mapping.
     */
    void updateTranslations(FileContext fs) throws IOException;

    /**
     * @return a newly created stream for reading the contents of this source
     */
    InputStream openNewInputStream() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

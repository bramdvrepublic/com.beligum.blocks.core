package com.beligum.blocks.fs.pages.ifaces;

import com.beligum.base.auth.models.Person;
import com.beligum.blocks.rdf.ifaces.Source;

import java.io.IOException;

/**
 * Created by bram on 1/14/16.
 */
public interface PageStore
{
    //-----PUBLIC METHODS-----
    void init() throws IOException;
    Page save(Source source, Person creator) throws IOException;
}

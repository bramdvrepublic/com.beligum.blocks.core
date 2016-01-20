package com.beligum.blocks.pages.ifaces;

import com.beligum.base.auth.models.Person;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 1/14/16.
 */
public interface PageStore
{
    //-----PUBLIC METHODS-----
    void init() throws IOException;
    void save(URI page, String content, Person creator) throws IOException;
}

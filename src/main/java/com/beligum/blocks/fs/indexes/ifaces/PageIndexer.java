package com.beligum.blocks.fs.indexes.ifaces;

import com.beligum.blocks.fs.indexes.entries.PageIndexEntry;
import com.beligum.blocks.fs.pages.ifaces.Page;

import java.io.IOException;
import java.net.URI;

/**
 * Generic superclass for the page indexer
 *
 * B = QueryBuilder
 * Q = Query
 * R = QueryResult
 *
 * Created by bram on 1/26/16.
 */
public interface PageIndexer<B, Q, R> extends Indexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    PageIndexEntry get(URI key) throws IOException;
    void delete(Page page) throws IOException;
    void indexPage(Page page) throws IOException;
    B getNewQueryBuilder() throws IOException;
    R executeQuery(Q query) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

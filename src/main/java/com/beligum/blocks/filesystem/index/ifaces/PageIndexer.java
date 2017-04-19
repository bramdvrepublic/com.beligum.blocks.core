package com.beligum.blocks.filesystem.index.ifaces;

import com.beligum.blocks.filesystem.hdfs.TX;

import java.io.IOException;

/**
 * Created by bram on 12/04/17.
 */
public interface PageIndexer extends Indexer
{
    /**
     * This is the overridden general connect() method from the super interface to be able to be more specific in it's return type.
     */
    @Override
    PageIndexConnection connect(TX tx) throws IOException;
}

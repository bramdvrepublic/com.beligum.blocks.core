package com.beligum.blocks.fs.pages;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by bram on 8/28/16.
 */
public class WalkPagesIterator implements Iterator<Page>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Iterator<Path> hdfsIterator;
    private final boolean readOnly;

    //-----CONSTRUCTORS-----
    public WalkPagesIterator(Iterator<Path> hdfsIterator, boolean readOnly)
    {
        this.hdfsIterator = hdfsIterator;
        this.readOnly = readOnly;
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean hasNext()
    {
        return this.hdfsIterator.hasNext();
    }
    @Override
    public Page next()
    {
        Page retVal = null;

        Path next = this.hdfsIterator.next();
        if (next!=null) {
            try {
                retVal = this.readOnly ? new ReadOnlyPage(next.toUri()) : new ReadWritePage(next.toUri());
            }
            catch (IOException e) {
                Logger.error("Error while building the page from the URI returned by the HDFS iterator (returning null); "+next, e);
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

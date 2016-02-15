package com.beligum.blocks.fs.indexes.stubs;

import com.beligum.blocks.fs.pages.ifaces.Page;
import org.hibernate.search.annotations.*;

/**
 * Created by bram on 2/13/16.
 */
@Indexed
public class PageStub extends AbstractStub
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public PageStub()
    {
        this(null);
    }
    public PageStub(Page page)
    {
        super(page.getPathInfo().getUri());
    }

    //-----PUBLIC METHODS-----
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
    public String getFirstName()
    {
        return "TESTJE!";
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

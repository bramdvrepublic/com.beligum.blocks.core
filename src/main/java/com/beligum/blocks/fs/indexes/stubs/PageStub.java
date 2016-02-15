package com.beligum.blocks.fs.indexes.stubs;

import com.beligum.blocks.fs.pages.ifaces.Page;
import org.hibernate.search.annotations.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bram on 2/13/16.
 */
@Indexed
public class PageStub extends AbstractStub
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Set<PageStub> children;

    //-----CONSTRUCTORS-----
    public PageStub()
    {
        this(null);
    }
    public PageStub(Page page)
    {
        super(page.getPathInfo().getUri());

        this.children = new HashSet<>();
    }

    //-----PUBLIC METHODS-----
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
    public String getFirstName()
    {
        return "TESTJE: "+this.getId();
    }
    @IndexedEmbedded(depth = 1, includeEmbeddedObjectId = true)
    public Set<PageStub> getChildren()
    {
        return children;
    }
    public void addChild(PageStub child)
    {
        this.children.add(child);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
}

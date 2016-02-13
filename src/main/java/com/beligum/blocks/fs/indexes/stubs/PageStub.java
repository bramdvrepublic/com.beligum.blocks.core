package com.beligum.blocks.fs.indexes.stubs;

import com.beligum.blocks.fs.pages.ifaces.Page;
import org.hibernate.search.annotations.*;

import java.net.URI;

/**
 * Created by bram on 2/13/16.
 */
@Indexed
public class PageStub
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI id;
    private String firstName;

    //-----CONSTRUCTORS-----
    public PageStub()
    {
        this(null);
    }
    public PageStub(Page page)
    {
        firstName = "TEST VALUE";
        id = URI.create("https://github.com/DmitryKey/luke");
    }

    //-----PUBLIC METHODS-----
    @DocumentId
    public URI getId()
    {
        return id;
    }
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
    public String getFirstName()
    {
        return firstName;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

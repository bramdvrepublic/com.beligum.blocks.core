package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.AbstractPage;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.ifaces.Storable;

import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by bas on 07.10.14.
 * Class representing a concrete instance of a html-page filtered from a template-file
 */
public class Page extends AbstractPage implements Storable
{
    //the page-class this page is an instance of
    private PageClass pageClass;


    /**
     *
     * Constructor for a new page-instance of a certain page-class, which will be filled with the default rows and blocks from the page-class.
     * It's UID will be the of the form "<url>:<version>"
     * @param url the url to this page
     * @param pageClass the class of which this page is a page-instance
     */
    public Page(URL url, PageClass pageClass) throws URISyntaxException
    {
        super(new RedisID(url));
        this.pageClass = pageClass;
        this.setRows(pageClass.getRows());
        this.setBlocks(pageClass.getBlocks());
    }

    public PageClass getPageClass()
    {
        return pageClass;
    }
    public void setPageClass(PageClass pageClass)
    {
        this.pageClass = pageClass;
    }

    @Override
    public RedisID getId(){
        return (RedisID) this.id;
    }
    @Override
    /**
     * returns the version of this page, which is the time it was created in milliseconds
     */
    public long getVersion(){
        return getId().getVersion();
    }

    /**
     *
     * @return a url to the latest version of this page
     */
    public URL getUrl(){
        return getId().getURL();
    }


}

package com.beligum.blocks.core.models;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bas on 07.10.14.
 * Class representing a concrete instance of a html-page filtered from a template-file
 */
public class Page extends AbstractPage
{
    //the page-class this page is an instance of
    private PageClass pageClass;


    /**
     *
     * Constructor for a new page-instance of a certain page-class, which will be filled with the default rows and blocks from the page-class
     * @param url the url to this page, this will be the UID for this page in the db
     * @param pageClass the class of which this page is a page-instance
     */
    public Page(String url, PageClass pageClass){
        //TODO BAS: check for url-format
        super(url);
        this.pageClass = pageClass;
    }

    public PageClass getPageClass()
    {
        return pageClass;
    }
    public void setPageClass(PageClass pageClass)
    {
        this.pageClass = pageClass;
    }
    /**
     *
     * @return the uid of this page (which is it's url)
     */
    public String getUrl()
    {
        return uid;
    }

}

package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.identifiers.PageID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.AbstractPage;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.ifaces.Storable;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
     * It's UID will be the of the form "[url]:[version]"
     * @param id the id of this page
     * @param pageClass the class of which this page is a page-instance
     * @throw URISyntaxException if a url is specified not formatted strictly according to to RFC2396
     */
    public Page(PageID id, PageClass pageClass)
    {
        super(id);
        this.pageClass = pageClass;
        this.setRows(pageClass.getRows());
        this.setBlocks(pageClass.getBlocks());
    }

//    public Page(URL url, long version, PageClass pageClass) throws URISyntaxException
//    {
//        super(new RedisID(url, version));
//        this.pageClass = pageClass;
//        this.setRows(pageClass.getRows());
//        this.setBlocks(pageClass.getBlocks());
//    }

    public PageClass
    getPageClass()
    {
        return pageClass;
    }
    public void setPageClass(PageClass pageClass)
    {
        this.pageClass = pageClass;
    }

    /**
     *
     * @return a url to the latest version of this page
     */
    public URL getUrl(){
        return getId().getURL();
    }

    /**
     *
     * @return the id of the hash containing the info of this page in the db
     */
    public String getInfoId(){
        return ((PageID) this.getId()).getPageInfoId();
    }


    public Map<String, String> getInfo(){
        return this.toHash();
    }

    /**
     *
     * @return the most outer velocity-string for this page's block- and row-tree
     */
    public String getVelocity(){
        return this.getPageClass().getVelocity();
    }


    //_______________IMPLEMENTATION OF STORABLE____________________//
    @Override
    public RedisID getId(){
        return (RedisID) this.id;
    }
    @Override
    public long getVersion(){
        return getId().getVersion();
    }
    @Override
    public String getUnversionedId(){
        return this.getId().getUnversionedId();
    }
    @Override
    public String getVersionedId(){
        return this.getId().getVersionedId();
    }
    @Override
    public Map<String, String> toHash()
    {
        Map<String, String> hash = new HashMap<>();
        //TODO BAS: this version should be fetched from pom.xml
        hash.put("appVersion", "test");
        //TODO: logged in user should be added here
        hash.put("creator", "me");
        hash.put("pageClass", this.getPageClass().getName());
        return hash;
    }
}

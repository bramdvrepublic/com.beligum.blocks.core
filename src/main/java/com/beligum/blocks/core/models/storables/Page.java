package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.caching.PageClassCache;
import com.beligum.blocks.core.config.DatabaseFieldNames;
import com.beligum.blocks.core.exceptions.PageClassCacheException;
import com.beligum.blocks.core.identifiers.PageID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.AbstractPage;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.ifaces.Storable;
import com.beligum.blocks.core.models.ifaces.StorableElement;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 07.10.14.
 * Class representing a concrete instance of a html-page filtered from a template-file
 */
public class Page extends AbstractPage implements Storable
{
    /**the page-class this page is an instance of*/
    private PageClass pageClass;
    /**the version of the application this block is supposed to interact with*/
    private String applicationVersion;
    /**the creator of this block*/
    private String creator;

    /**
     *
     * Constructor for a new page-instance of a certain page-class, which will be filled with the default rows and blocks from the page-class.
     * It's UID will be the of the form "[url]:[version]". It used the current application version and the currently logged in user for field initialization.
     * @param id the id of this page
     * @param pageClass the class of which this page is a page-instance
     * @throw URISyntaxException if a url is specified not formatted strictly according to to RFC2396
     */
    public Page(PageID id, PageClass pageClass)
    {
        super(id);
        this.pageClass = pageClass;
        this.addRows(pageClass.getRows());
        this.addBlocks(pageClass.getBlocks());
        //TODO BAS: this version should be fetched from pom.xml
        this.applicationVersion = "test";
        //TODO: logged in user should be added here
        this.creator = "me";
    }

    /**
     *
     * Constructor for a new page-instance of a certain page-class, which will be filled with the default rows and blocks from the page-class.
     * It's UID will be the of the form "[url]:[version]"
     * @param id the id of this page
     * @param pageClass the class of which this page is a page-instance
     * @param applicationVersion the version of the app this page was saved under
     * @param creator the creator of this page
     * @throw URISyntaxException if a url is specified not formatted strictly according to to RFC2396
     */
    public Page(PageID id, PageClass pageClass, String applicationVersion, String creator)
    {
        super(id);
        this.pageClass = pageClass;
        this.addRows(pageClass.getRows());
        this.addBlocks(pageClass.getBlocks());
        this.applicationVersion = applicationVersion;
        this.creator = creator;
    }

    /**
     * Constructor for a new page-instance taking blocks, rows and a pageclass. The rows and blocks of the pageClass are NOT copied to this page.
     * @param id the id of this page
     * @param blocks the blocks of the page
     * @param rows the rows of the page
     * @param pageClassName the name of the page-class this page is an instance of
     * @throws PageClassCacheException when the page-class cannot be found in the application-cache
     */
    public Page(PageID id, Set<Block> blocks, Set<Row> rows, String pageClassName) throws PageClassCacheException
    {
        super(id);
        this.addBlocks(blocks);
        this.addRows(rows);
        PageClass pageClass = PageClassCache.getInstance().getPageClassCache().get(pageClassName);
        this.pageClass = pageClass;
        //TODO BAS: this version should be fetched from pom.xml
        this.applicationVersion = "test";
        //TODO: logged in user should be added here
        this.creator = "me";
    }

    /**
     * Constructor for a new page-instance taking elements fetched from db and a pageclass (fetched from application cache).
     * The rows and blocks are added to this page in the following order:
     * 1. final elements of page-class, 2. blocks and rows from database specified in the set, 3. non-final elements of page-class, whose element-id's are not yet present in the page
     * @param id the id of this page
     * @param elementsFromDB the blocks of the page
     * @param pageClass the page-class this page is an instance of
     * @param applicationVersion the version of the app this page was saved under
     * @param creator the creator of this page
     *
     */
    public Page(PageID id, Set<StorableElement> elementsFromDB, PageClass pageClass, String applicationVersion, String creator)
    {
        super(id);
        this.addElements(pageClass.getFinalElements().values());
        this.addElements(elementsFromDB);
        this.addElements(pageClass.getNonFinalElements());
        this.pageClass = pageClass;
        this.applicationVersion = applicationVersion;
        this.creator = creator;
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
     * @return a url to the latest version of this page
     */
    public URL getUrl(){
        return getId().getUrl();
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
     * @return the most outer template-string for this page's block- and row-tree
     */
    public String getTemplate(){
        return this.getPageClass().getTemplate();
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
        hash.put(DatabaseFieldNames.APP_VERSION, this.applicationVersion);
        hash.put(DatabaseFieldNames.CREATOR, this.creator);
        hash.put(DatabaseFieldNames.PAGE_CLASS, this.getPageClass().getName());
        return hash;
    }
    @Override
    public String getApplicationVersion()
    {
        return this.applicationVersion;
    }
    @Override
    public String getCreator()
    {
        return this.creator;
    }
}

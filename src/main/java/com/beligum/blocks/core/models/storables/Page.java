package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.config.DatabaseFieldNames;
import com.beligum.blocks.core.exceptions.ElementException;
import com.beligum.blocks.core.identifiers.ElementID;
import com.beligum.blocks.core.identifiers.PageID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.AbstractPage;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.ifaces.Storable;
import com.beligum.blocks.core.models.ifaces.StorableElement;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
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
     * It's UID will be the of the form "[url]:[version]"
     * @param id the id of this page
     * @param pageClass the class of which this page is a page-instance
     * @throw URISyntaxException if a url is specified not formatted strictly according to to RFC2396
     */
    public Page(PageID id, PageClass pageClass)
    {
        super(id);
        this.pageClass = pageClass;
        //TODO BAS: this version should be fetched from pom.xml
        this.applicationVersion = "test";
        //TODO: logged in user should be added here
        this.creator = "me";
    }

    /**
     * Constructor for a new page-instance taking blocks, rows and a pageclass. The rows and blocks of the pageClass are not copied to the page.
     * @param id the id of this page
     * @param blocks the blocks of the page
     * @param rows the rows of the page
     * @param pageClass the page-class this page is an instance of
     * @throws ElementException if a block or row cannot be added to the page
     */
    public Page(PageID id, Set<Block> blocks, Set<Row> rows, PageClass pageClass) throws ElementException
    {
        super(id);
        //TODO BAS: HIER BEGINNEN: je hebt net deze constructor helemaal herordend, zodanig dat een pagina altijd de final elementen bezit (met een pagina-specifieke id), dan worden de nieuwe rijen en blocken toegevoegd, daarna de overschot uit de page-class (ook met een pagina-specifieke id), dit alles zou het algoritme bij Redis.save(page) zo goed als af moeten maken.
        this.addElementsFromPageClass(pageClass.getFinalElements());
        this.addBlocks(blocks);
        this.addRows(rows);
        this.addElementsFromPageClass(pageClass.getNonFinalElements());
        this.pageClass = pageClass;
        //TODO BAS: this version should be fetched from pom.xml
        this.applicationVersion = "test";
        //TODO: logged in user should be added here
        this.creator = "me";
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
     * @return the most outer velocity-string for this page's block- and row-tree
     */
    public String getVelocity(){
        return this.getPageClass().getVelocity();
    }


    /**
     *
     * @return a set of elements of this page present in the application-cache //TODO BAS: write comments
     */
    public HashSet<StorableElement> getCachedElements(){
        return this.getPageClass().getNonFinalElements();
    }

    public HashSet<StorableElement> getFinalElementsFromPageClass(){
        return this.getPageClass().getFinalElements();
    }

    /**
     * Add elements present in the page-class. For each element it is checked if no such element is already present in this page.
     * Since a page can only hold elements with id's that start with it's own page-id (of the form "[pageId]#[elementId]"), we actually use a copy of the elements in the page-class.
     * This is done because the elements in the page-class have id's of the form "[pageClassId]#[elementId]".
     * @param pageClassBlocks the elements retrieved from a pageClass to be added to this page
     */
    private void addElementsFromPageClass(Set<StorableElement> pageClassBlocks) throws ElementException
    {
        for(StorableElement pageClassElement : pageClassBlocks){
            try{
                /*
                 * To transform a element from the page-class into one that this page can contain, it's id "[pageClassId]#[elementId]" must be altered to "[pageId]#[elementId]"
                 * However the same content is used for the copy of the page-class-element
                 */
                URL elementUrl = new URL(this.getUrl().toString() + "#" + pageClassElement.getId().getElementId());
                ElementID id = new ElementID(elementUrl, pageClassElement.getId().getVersion());
                StorableElement element = null;
                //TODO BAS: should we get Blocks and Rows at once with StorableElement element = pageClassElement.getClass().getConstructor(URL.class, String.class, Boolean.class).newInstance(id, pageClassElement.getContent(), pageClassElement.isFinal()); ???
                if(pageClassElement instanceof Block) {
                    element = new Block(id, pageClassElement.getContent(), pageClassElement.isFinal());
                }
                else if(pageClassElement instanceof Row){
                    element = new Row(id, pageClassElement.getContent(), pageClassElement.isFinal());
                }
                else{
                    throw new ElementException("Unsupported element-type detected: " + pageClassElement.getClass().getName());
                }
                this.addElement(element);
            }
            catch(ElementException e){
                throw e;
            }
            catch(Exception e){
                throw new ElementException("Could not add element '" + pageClassElement.getId() + "' from page-class to page '" + this.getId() + "'.", e);
            }
        }
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

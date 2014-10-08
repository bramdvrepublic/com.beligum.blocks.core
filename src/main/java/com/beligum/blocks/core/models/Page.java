package com.beligum.blocks.core.models;

import com.beligum.blocks.core.config.BlocksConfig;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by bas on 07.10.14.
 * Class representing a html-page
 */
public class Page extends IdentifiableObject
{
    //set with all blocks of this page
    private Set<Block> pageBlocks;
    //set with all rows of this page
    private Set<Row> pageRows;
    //string with the most outer velocity template of this page
    private String outerVelocityTemplate;
    //string representing the page-class this page is an instance of
    private String pageClass;


    /**
     *
     * @param url this should be the url to this page, which is the uid for this IdentifiableObject
     * @param pageClass the class of which this page is a page-instance
     */
    public Page(String url, String pageClass, String outerVelocityTemplate){
        //TODO BAS: check for url-format
        super(url);
        this.pageClass = pageClass;
        this.pageBlocks = new HashSet<Block>();
        this.pageRows = new HashSet<Row>();
        this.outerVelocityTemplate = outerVelocityTemplate;
    }

    /**
     * Constructor for a new page-instance, which will have the same pageclass, rows and blocks as a specified page
     * @param url this should be the url to this new page, which is the uid for this IdentifiableObject
     * @param page the page to 'copy' into this new page
     */
    public Page(String url, Page page){
        this(url, page.getPageClass(), page.getOuterVelocityTemplate());
        this.pageBlocks = page.getPageBlocks();
        this.pageRows = page.pageRows;
    }

    public Set<Block> getPageBlocks()
    {
        return pageBlocks;
    }
    public void setPageBlocks(Set<Block> pageBlocks)
    {
        this.pageBlocks = pageBlocks;
    }
    public Set<Row> getPageRows()
    {
        return pageRows;
    }
    public void setPageRows(Set<Row> pageRows)
    {
        this.pageRows = pageRows;
    }
    public String getOuterVelocityTemplate()
    {
        return outerVelocityTemplate;
    }
    public void setOuterVelocityTemplate(String outerVelocityTemplate)
    {
        this.outerVelocityTemplate = outerVelocityTemplate;
    }
    public String getPageClass()
    {
        return pageClass;
    }
    public void setPageClass(String pageClass)
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

    public void addBlock(Block block){
        this.pageBlocks.add(block);
    }
    public void addRow(Row row){
        this.pageRows.add(row);
    }
    public void addElement(AbstractIdentifiableElement element)
    {
        if(element instanceof Row){
            this.pageRows.add((Row) element);
        }
        else if(element instanceof Block){
            this.pageBlocks.add((Block) element);
        }
        else{
            throw new RuntimeException("Could not add element to page, it has an unknown AbstractIdentifialbeElement-type: " + element.getClass().getName());
        }
    }
    public Set<AbstractIdentifiableElement> getElements(){
        Set<AbstractIdentifiableElement> elements = new HashSet<>();
        elements.addAll(this.pageBlocks);
        elements.addAll(this.pageRows);
        return elements;
    }

    /*___________________STATIC_METHODS___________________*/ //see these static methods as on 'class'-level of a page

    /**
     * Method for getting a new randomly determined page-uid, with versioning
     * @param pageClass the page-class for which a new page-id must be rendered
     * @return a randomly generated page-id of the form "/pages/<pageClass>/<randomInt>:<version>"
     */
    public static String getNewUniqueID(String pageClass){
        //TODO BAS: site-identifier should be added to pageclass-ids: f.i. MOT/'pageClass' should be used instead of only 'pageClass'
        long currentTime = System.currentTimeMillis();
        return getNewUnversionnedID(pageClass) + ":" + currentTime;
    }

    /**
     * Method for getting a new randomly determined page-id, without versioning attached
     * @param pageClass the page-class for which a new page-id must be rendered
     * @return a randomly generated page-id of the form "/pages/<pageClass>/<randomInt>"
     */
    public static String getNewUnversionnedID(String pageClass){
        //TODO BAS: site-identifier should be added to pageclass-ids: f.i. MOT/'pageClass' should be used instead of only 'pageClass'
        Random randomGenerator = new Random();
        int positiveNumber = Math.abs(randomGenerator.nextInt());
        return pageClass + "/" + positiveNumber;
    }

    /**
     * returns the path to the template of the page-class 'pageClass'
     * f.i. returns "/home/user/.../templates/pages/default/index.html" for pageClass: default
     * @param pageClass the name of the page template
     */
    public static String getTemplatePath(String pageClass){
        return BlocksConfig.getTemplateFolder() + "/pages/" + pageClass + "/index.html";
    }

}

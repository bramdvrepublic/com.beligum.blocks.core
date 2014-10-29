package com.beligum.blocks.core.models;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.identifiers.PageID;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Row;
import org.apache.commons.configuration.ConfigurationRuntimeException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;
import java.util.Set;

/**
 * Created by bas on 08.10.14.
 * A representation of a html page-template. It has an id of the form "page/<pageClassName>"
 */
public class PageClass extends AbstractPage
{
    //TODO BAS SH: start with implementing BlockClass, BlockClassCache (and BlockParser?)

    /**the prefix that is given to page-class-id's*/
    private static final String ID_PREFIX = "pages";
    /**string the name of this page-class*/
    private String name;
    /**string holding the template-content of this pageClass*/
    private String template;
    /**the doctype of this pageclass*/
    private String docType;

    /**
     *
     * @param name the name of this page-class
     * @param blocks the (default) blocks this page-class contains
     * @param rows the (default) rows this page-class contains
     * @param template the template-string corresponding to the most outer layer of the element-tree in this page
     * @param docType the doctype of this page-class
     */
    public PageClass(String name, Set<Block> blocks, Set<Row> rows, String template, String docType)
    {
        super(makeID(name));
        this.name = name;
        this.template = template;
        this.addBlocks(blocks);
        this.addRows(rows);
        this.docType = docType;
    }

    public String getName(){
        return this.name;
    }
    public String getTemplate(){
        return this.template;
    }
    public String getDocType()
    {
        return docType;
    }

    /**
     * Method for getting a new randomly determined page-uid (with versioning) for a pageInstance of this pageClass
     * @return a randomly generated page-id of the form "[site-domain]/[pageClassName]/[randomInt]"
     */
    public PageID renderNewPageID(){
        try {
            Random randomGenerator = new Random();
            int positiveNumber = Math.abs(randomGenerator.nextInt());
            return new PageID(new URL(BlocksConfig.getSiteDomain() + "/" + this.name + "/" + positiveNumber));
        }catch(MalformedURLException e){
            throw new ConfigurationRuntimeException("Specified site-domain doesn't seem to be a correct url: " + BlocksConfig.getSiteDomain(), e);
        }catch(URISyntaxException e){
            throw new ConfigurationRuntimeException("Cannot use this site-domain for id-rendering: " + BlocksConfig.getSiteDomain(), e);
        }
    }

    /**
     * returns the base-url for the page-class
     * @param pageClassName the name of the page-class (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
     * @return
     */
    public static URL getBaseUrl(String pageClassName){
        try{
            return new URL(BlocksConfig.getSiteDomain() + "/" + ID_PREFIX + "/" + pageClassName);
        }catch(MalformedURLException e){
            throw new ConfigurationRuntimeException("Specified site-domain doesn't seem to be a correct url:  " + BlocksConfig.getSiteDomain(), e);
        }
    }

    /**
     * Return an ID for the pageclass with a (unique) name, the id for all pageclasses will be "page/<pageClassName>"
     * @param pageClassName the unique name of the pageClass
     * @return an ID for the pageclass
     */
    private static ID makeID(String pageClassName)
    {
        try {
            return new ID(new URI(ID_PREFIX + "/" + pageClassName));
        }catch(URISyntaxException e){
            throw new RuntimeException("The pageClassName seems to be incorrectly formed, can't construct page-class-id with it: " + pageClassName, e);
        }
    }
}

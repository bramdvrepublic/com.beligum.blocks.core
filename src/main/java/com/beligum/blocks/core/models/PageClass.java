package com.beligum.blocks.core.models;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.AbstractPage;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Row;
import com.beligum.blocks.core.parsing.PageParserException;
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
    //the prefix that is given to page-class-id's
    private static final String ID_PREFIX = "page";
    //string the name of this page-class
    private String name;
    //string holding the velocity-content of this pageClass
    private String velocity;

    /**
     *
     * @param name the name of this page-class
     * @param elements the elements (rows and blocks) this page-class contains
     * @param velocity the velocity-string corresponding to the most outer layer of the element-tree in this page
     */
    public PageClass(String name, Set<AbstractElement> elements, String velocity)
    {
        super(makeID(name));
        this.name = name;
        this.velocity = velocity;
        for(AbstractElement element : elements){
            if(element instanceof Block){
                this.blocks.add((Block) element);
            }
            else if(element instanceof Row){
                this.rows.add((Row) element);
            }
            else{
                throw new RuntimeException("Unsupported AbstractElement-type: " + element.getClass().getName());
            }
        }
    }

    public String getName(){
        return this.name;
    }
    public String getVelocity(){
        return this.velocity;
    }


    /**
     * Method for getting a new randomly determined page-uid (with versioning) for a pageInstance of this pageClass
     * @return a randomly generated page-url of the form "/<pageClass>/<randomInt>"
     */
    public String renderNewPageURL(){
        Random randomGenerator = new Random();
        int positiveNumber = Math.abs(randomGenerator.nextInt());
        return BlocksConfig.getSiteDomain() + "/" + this.name + "/" + positiveNumber;
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

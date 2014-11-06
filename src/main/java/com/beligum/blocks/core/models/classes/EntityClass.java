package com.beligum.blocks.core.models.classes;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CSSClasses;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.identifiers.EntityID;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.storables.Row;
import org.apache.commons.configuration.ConfigurationRuntimeException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;
import java.util.Set;

/**
 * Created by bas on 05.11.14.
 */
public class EntityClass extends AbstractViewableClass
{
    /**the doctype of this pageclass*/
    private String docType;
    /**string the name of this page-class*/
    private String name;

    /**
     *
     * @param name the name of this page-class
     * @param directChildren the direct children of this page-class
     * @param template the template-string corresponding to the most outer layer of the element-tree in this page
     * @param docType the doctype of this page-class
     */
    public EntityClass(String name, Set<Row> directChildren, String template, String docType) throws URISyntaxException
    {
        super(makeId(name), directChildren, template);
        this.name = name;
        this.docType = docType;
    }

    /**
     *
     * @return
     */
    @Override
    public String getName()
    {
        return name;
    }
    public String getDocType()
    {
        return docType;
    }
    /**
     * @return the prefix used for a page-entity-class in the class-attribute of the html-template
     */
    @Override
    public String getCssClassPrefix()
    {
        return CSSClasses.ENTITY_CLASS_PREFIX;
    }

    /**
     * Method for getting a new randomly determined page-uid (with versioning) for a pageInstance of this pageClass
     * @return a randomly generated page-id of the form "[site-domain]/[pageClassName]/[randomInt]"
     */
    public EntityID renderNewPageID(){
        try {
            Random randomGenerator = new Random();
            int positiveNumber = Math.abs(randomGenerator.nextInt());
            return new EntityID(new URL(BlocksConfig.getSiteDomain() + "/" + this.name + "/" + positiveNumber));
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
    public static URL getBaseUrl(String pageClassName) throws MalformedURLException
    {
        return new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.PAGE_ENTITY_CLASS_ID_PREFIX + "/" + pageClassName);
    }

    /**
     * Return an ID for the pageclass with a (unique) name, the id for all pageclasses will be "page/<pageClassName>"
     * @param pageClassName the unique name of the pageClass
     * @return an ID for the pageclass
     */
    private static ID makeId(String pageClassName) throws URISyntaxException
    {
        return new ID(new URI(CacheConstants.PAGE_ENTITY_CLASS_ID_PREFIX + "/" + pageClassName));
    }

}

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
    /**the doctype of this entityclass*/
    private String docType;
    /**string the name of this entity-class*/
    private String name;

    /**
     *
     * @param name the name of this entity-class
     * @param allChildren all children of this entity-class
     * @param template the template-string corresponding to the most outer layer of the element-tree in this entity
     * @param docType the doctype of this entity-class
     */
    public EntityClass(String name, Set<Row> allChildren, String template, String docType) throws URISyntaxException
    {
        super(makeId(name), allChildren, template);
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
     * @return the prefix used for a entity-entity-class in the class-attribute of the html-template
     */
    @Override
    public String getCssClassPrefix()
    {
        return CSSClasses.ENTITY_CLASS_PREFIX;
    }

    /**
     * Method for getting a new randomly determined entity-uid (with versioning) for a entityInstance of this entityClass
     * @return a randomly generated entity-id of the form "[site-domain]/[entityClassName]/[randomInt]"
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
     * returns the base-url for the entity-class
     * @param entityClassName the name of the entity-class (f.i. "default" for a entityClass filtered from the file "entities/default/index.html")
     * @return
     */
    public static URL getBaseUrl(String entityClassName) throws MalformedURLException
    {
        return new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.PAGE_ENTITY_CLASS_ID_PREFIX + "/" + entityClassName);
    }

    /**
     * Return an ID for the entityclass with a (unique) name, the id for all entityclasses will be "entity/<entityClassName>"
     * @param entityClassName the unique name of the entityClass
     * @return an ID for the entityclass
     */
    private static ID makeId(String entityClassName) throws URISyntaxException
    {
        return new ID(new URI(CacheConstants.PAGE_ENTITY_CLASS_ID_PREFIX + "/" + entityClassName));
    }

}

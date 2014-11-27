package com.beligum.blocks.core.models.classes;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.storables.Entity;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
    public EntityClass(String name, Set<Entity> allChildren, String template, String docType) throws URISyntaxException
    {
        super(new ID(new URI(name)), allChildren, template);
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
        return ParserConstants.ENTITY_CLASS_PREFIX;
    }



    /**
     * returns the base-url for the entity-class
     * @param entityClassName the name of the entity-class (f.i. "default" for a entityClass filtered from the file "entities/default/index.html")
     * @return
     */
    public static URL getBaseUrl(String entityClassName) throws MalformedURLException
    {
        return new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.ENTITY_CLASS_ID_PREFIX + "/" + entityClassName);
    }

}

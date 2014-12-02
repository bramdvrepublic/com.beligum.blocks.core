package com.beligum.blocks.core.models.templates;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.identifiers.RedisID;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by bas on 05.11.14.
 */
public class EntityTemplateClass extends AbstractTemplate
{
    /**the doctype of this entityclass*/
    private String docType;
    /**string the name of this entity-class*/
    private String name;

    /**
     *
     * @param name the name of this entity-class
     * @param template the template-string corresponding to the most outer layer of the element-tree in this entity
     * @param docType the doctype of this entity-class
     */
    public EntityTemplateClass(String name, String template, String docType) throws URISyntaxException
    {
        super(RedisID.renderNewEntityTemplateClassID(name), template);
        this.name = name;
        this.docType = docType;
    }

    /**
     *
     * @return the name of this entity-class
     */
    public String getName()
    {
        return name;
    }
    public String getDocType()
    {
        return docType;
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

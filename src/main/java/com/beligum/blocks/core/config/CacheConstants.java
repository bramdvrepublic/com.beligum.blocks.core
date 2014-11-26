package com.beligum.blocks.core.config;

import com.beligum.blocks.core.models.IdentifiableObject;
import com.beligum.blocks.core.models.PageTemplate;
import com.beligum.blocks.core.models.classes.EntityClass;

/**
 * Created by bas on 03.11.14.
 */
public class CacheConstants
{
    /**the prefix that is given to entity-class-id's*/
    public static final String ENTITY_CLASS_ID_PREFIX = "entities";
    /**the prefix that is given to page-template-id's*/
    public static final String PAGE_TEMPLATE_ID_PREFIX = "pageTemplates";
    /**the name of the default entity-class*/
    public static final String DEFAULT_ENTITY_CLASS_NAME = "default";

    /**
     *
     * @param identifiableObjectType
     * @return the id-prefix for a identifiable object-type, f.i. "entities" for EntityClass.class
     * @throws RuntimeException if a unsupported viewable-class-type is specified (only EntityClass.class and BlockClass.class are supported)
     */
    public static String getIdPrefix(Class<? extends IdentifiableObject> identifiableObjectType){
        if(identifiableObjectType.isInstance(EntityClass.class)){
            return ENTITY_CLASS_ID_PREFIX;
        }
        else if(identifiableObjectType.isInstance(PageTemplate.class)){
            return PAGE_TEMPLATE_ID_PREFIX;
        }
        else{
            throw new RuntimeException("Unsupported viewable-class type: '" + identifiableObjectType.getName() + "'.");
        }
    }
}

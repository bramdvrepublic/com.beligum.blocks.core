package com.beligum.blocks.core.config;

import com.beligum.blocks.core.models.redis.Storable;
import com.beligum.blocks.core.models.redis.templates.Blueprint;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;

/**
 * Created by bas on 03.11.14.
 */
public class CacheConstants
{
    /**the prefix that is given to entity-class-id's*/
    public static final String ENTITY_CLASS_ID_PREFIX = "entities";
    /**the prefix that is given to page-template-id's*/
    public static final String PAGE_TEMPLATE_ID_PREFIX = "pageTemplates";

    /**
     *
     * @param storableType
     * @return the id-prefix for a identifiable object-type, f.i. "entities" for EntityClass.class
     * @throws RuntimeException if a unsupported viewable-class-type is specified (only EntityClass.class and BlockClass.class are supported)
     */
    public static String getIdPrefix(Class<? extends Storable> storableType){
        if(storableType.isInstance(Blueprint.class)){
            return ENTITY_CLASS_ID_PREFIX;
        }
        else if(storableType.isInstance(PageTemplate.class)){
            return PAGE_TEMPLATE_ID_PREFIX;
        }
        else{
            throw new RuntimeException("Unsupported " + Storable.class.getSimpleName() + " type: '" + storableType.getName() + "'.");
        }
    }
}

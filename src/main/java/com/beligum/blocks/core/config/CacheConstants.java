package com.beligum.blocks.core.config;

import com.beligum.blocks.core.models.classes.AbstractViewableClass;
import com.beligum.blocks.core.models.classes.BlockClass;
import com.beligum.blocks.core.models.classes.EntityClass;

/**
 * Created by bas on 03.11.14.
 */
public class CacheConstants
{
    /**the prefix that is given to page-class-id's*/
    public static final String PAGE_ENTITY_CLASS_ID_PREFIX = "entities";
    /**the prefix that is given to block-class-id's*/
    public static final String BLOCK_CLASS_ID_PREFIX = "blocks";

    /**
     *
     * @param viewableClassType
     * @return the id-prefix for a viewable-class, f.i. "blocks" for BlockClass.class
     * @throws RuntimeException if a unsupported viewable-class-type is specified (only EntityClass.class and BlockClass.class are supported)
     */
    public static String getViewableClassIdPrefix(Class<? extends AbstractViewableClass> viewableClassType){
        if(viewableClassType.isInstance(EntityClass.class)){
            return PAGE_ENTITY_CLASS_ID_PREFIX;
        }
        else if(viewableClassType.isInstance(BlockClass.class)){
            return BLOCK_CLASS_ID_PREFIX;
        }
        else{
            throw new RuntimeException("Unsupported viewable-class type: '" + viewableClassType.getName() + "'.");
        }
    }
}

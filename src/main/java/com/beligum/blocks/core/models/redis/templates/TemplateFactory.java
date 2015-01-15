package com.beligum.blocks.core.models.redis.templates;

import com.beligum.blocks.core.exceptions.DeserializationException;
import com.beligum.blocks.core.identifiers.RedisID;

import java.util.Map;

/**
 * Created by bas on 07.01.15.
 */
public class TemplateFactory
{
    /**
     * The EntityTemplate-class can be used as a factory, to construct entity-templates from data found in a hash in the redis-db
     * @param hash a map, mapping field-names to field-values
     * @return an entity-template or throws an exception if no entity-template could be constructed from the specified hash
     * @throws DeserializationException when a bad hash is found
     */
    public static AbstractTemplate createInstanceFromHash(RedisID id, Map<String, String> hash, Class<? extends AbstractTemplate> type) throws DeserializationException
    {
        if(type.equals(EntityTemplate.class)){
            return EntityTemplate.createInstanceFromHash(id, hash);
        }
        else if(type.equals(EntityTemplateClass.class)){
            return EntityTemplateClass.createInstanceFromHash(id, hash);
        }
        else if(type.equals(PageTemplate.class)){
            return PageTemplate.createInstanceFromHash(id, hash);
        }
        else{
            throw new DeserializationException("Unknown " + AbstractTemplate.class.getSimpleName() + "-type: " + type);
        }
    }
}

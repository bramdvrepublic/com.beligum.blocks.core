package com.beligum.blocks.core.models.nosql;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.identifiers.MongoID;
import com.beligum.blocks.core.mongo.MongoEntity;
import com.beligum.core.framework.annotations.Meta;
import com.beligum.core.framework.models.AbstractJsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by wouter on 17/03/15.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
                @JsonSubTypes.Type(value=Entity.class, name="Entity"),
                @JsonSubTypes.Type(value=MongoEntity.class, name="MongoEntity")

})
public class Entity implements BlocksStorable
{

    private BlockId id;
    private META meta;
    private String name;
    private String language;
    private LinkedHashMap<String, Object> properties = new LinkedHashMap<>();

    public Entity() {
        this.meta = new META();
    }

    public Entity(String name) {
        this(name, BlocksConfig.getDefaultLanguage());
    }

    public Entity(String name, String language) {
        this.meta = new META();
        this.name = name;
        this.language = language;
    }

    public LinkedHashMap<String, Object> getProperties() {
        return this.properties;
    }

    public void addProperty(String name, Object property) {
        properties.put(getKey(name), property);
    }

    protected String getKey(String name) {
        int count = 0;
        String keyName = name;
        while (properties.containsKey(keyName)) {
            count++;
            keyName = name + "/" + count;
        }
        return keyName;
    }

    @Override
    public BlockId getId() {
        return id;
    }

    @Override
    public void setId(BlockId id) {
        this.id = id;
    }

    @Override
    public META getMeta()
    {
        return meta;
    }
    @Override
    public void setMeta(META meta)
    {
        this.meta = meta;
    }
    public String getLanguage() {
        return this.language;
    }

    public ArrayList<Entity> flatten(ArrayList<Entity> list) {
        for (Object o: this.properties.values()) {
            if (o instanceof Entity) {
                ((Entity)o).flatten(list);
            }
        }
        list.add(this);
        return list;
    }

    public void merge(Entity entity, boolean overwrite) {
        if (entity != null) {
            for (String property : entity.getProperties().keySet()) {
                if (overwrite || !this.getProperties().containsKey(property)) {
                    this.getProperties().put(property, entity.getProperties().get(property));
                }
            }
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof Entity))
            return false;

        Entity entity = (Entity) o;

        if (!name.equals(entity.name))
            return false;
        if (!properties.equals(entity.properties))
            return false;

        return true;
    }
    @Override
    public int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + properties.hashCode();
        return result;
    }
}

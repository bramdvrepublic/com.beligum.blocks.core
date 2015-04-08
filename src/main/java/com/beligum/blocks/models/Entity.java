package com.beligum.blocks.models;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.utils.PropertyFinder;
import com.beligum.blocks.utils.URLFactory;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by wouter on 17/03/15.
 */

public abstract class Entity extends AbstractEntity
{

    private HashMap<String, Object> entity = new HashMap<>();
    private ArrayList<EntityField> properties = new ArrayList<>();

    public Entity() {
        super();
    }

    public Entity(String name) {
        this(name, Blocks.config().getDefaultLanguage());
    }

    public Entity(String value, String language) {
        this(null, value, language);
    }

    public Entity(String name, String value, String language) {
        super(name, value, language);
        this.value = URLFactory.makeAbsoluteRdfValue(value);
        entity.put("type", URLFactory.makeAbsoluteRdfValue(value));
    }

    public ArrayList<EntityField> getProperties() {
        return this.properties;
    }

    public void addProperty(EntityField property) {
        properties.add(property);
//        if (entity.containsKey(property.getName())) {
//
//        } else {
//            entity
//        }
    }


    public ArrayList<Entity> flatten(ArrayList<Entity> list) {
        for (Object o: this.properties) {
            if (o instanceof Entity) {
                ((Entity)o).flatten(list);
            }
        }
        list.add(this);
        return list;
    }

    public void merge(Entity entity, boolean overwrite) {
        if (entity != null) {
            PropertyFinder<EntityField> propertyFinder = new PropertyFinder<>();
            for (int i=0; i < entity.getProperties().size(); i++) {
                EntityField property = entity.getProperties().get(i);
                String key = property.getName();
                EntityField otherProperty = propertyFinder.getProperty(key, this.getProperties());
                if (overwrite || otherProperty != null) {
                    this.getProperties().add(i, otherProperty);
                }
                propertyFinder.propertyFound(key);
            }
        }
    }

    @Override
    public void setValue(String value) {
        if (value != null) {
            this.value = URLFactory.makeAbsoluteRdfValue(value);
        } else {
            this.value = null;
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

        if (!value.equals(entity.value))
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

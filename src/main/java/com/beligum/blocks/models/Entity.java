package com.beligum.blocks.models;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.utils.PropertyFinder;
import com.beligum.blocks.utils.URLFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wouter on 17/03/15.
 */

public class Entity extends EntityField
{

    private HashMap<String, Object> properties = new HashMap<>();

    public Entity() {
        super();
    }

    public Entity(HashMap<String, Object> properties) {
        this.properties = properties;
    }

    public Entity(String value) {
        properties.put("name", URLFactory.makeAbsoluteRdfValue(value));
    }

    public BlockId getId() {
        return null;
    }

    public void setId(BlockId id) {
    }

    public void addProperty(String name, String value) {
        this.addProperty(name, value, EntityField.NO_LANGUAGE);
    }

    public void addProperty(String name, String value, String language) {
        if (name == null || value == null) return;
        if (language == null) language = EntityField.NO_LANGUAGE;


        name = URLFactory.makeAbsoluteRdfValue(name);
        if (!properties.containsKey(name)) {
            properties.put(name, new HashMap<String, Object>());
        }
        if (!(properties.get(name) instanceof Map)) return;

        HashMap<String, Object> property = ((HashMap<String, Object>)properties.get(name));
        if (!property.containsKey(NO_LANGUAGE)) {
            property.put(NO_LANGUAGE, new ArrayList<Object>());
        }
        if (!property.containsKey(language)) {
            property.put(language, new ArrayList<Object>());
        }
        ArrayList<Object> langValues = (ArrayList<Object>)property.get(language);
        ArrayList<Object> values = (ArrayList<Object>)property.get(NO_LANGUAGE);
        langValues.add(value);
        if (langValues.size() > values.size()) {
            for (int i = values.size(); i < langValues.size(); i++) {
//                if (i > values.size()) {
//                    values.set(i, langValues.get(i));
//                } else {
//                    values.add(i, langValues.get(i));
//                }
            }
        }

    }

    public void addEntity(String name, Entity value) {
        name = URLFactory.makeAbsoluteRdfValue(name);
        if (!properties.containsKey(name)) {
            properties.put(name, new ArrayList<Object>());
        }
        if (!(properties.get(name) instanceof List)) return;

        ArrayList<Object> property = (ArrayList<Object>)properties.get(name);

        property.add(value.getProperties());

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

//    public void merge(Entity entity, boolean overwrite) {
//        if (entity != null) {
//            PropertyFinder<EntityField> propertyFinder = new PropertyFinder<>();
//            for (int i=0; i < entity.getProperties().size(); i++) {
//                EntityField property = entity.getProperties().get(i);
//                String key = property.getName();
//                EntityField otherProperty = propertyFinder.getProperty(key, this.getProperties());
//                if (overwrite || otherProperty != null) {
//                    this.getProperties().add(i, otherProperty);
//                }
//                propertyFinder.propertyFound(key);
//            }
//        }
//    }



    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof Entity))
            return false;

        Entity entity = (Entity) o;

        if (!properties.equals(entity.properties))
            return false;

        return true;
    }
    @Override
    public int hashCode()
    {
        return properties.hashCode();
    }
    public HashMap<String, Object> getProperties()
    {
        return properties;
    }
    public void setProperties(HashMap<String, Object> entity)
    {
        this.properties = entity;
    }
}

package com.beligum.blocks.models;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.interfaces.BlocksStorable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wouter on 17/03/15.
 */
public abstract class Entity extends EntityField implements BlocksStorable
{
    private String createdBy;
    private String updatedBy;
    private String createdAt;
    private String updatedAt;

    private HashMap<String, Object> properties = new HashMap<>();

    public Entity()
    {
        super();
    }

    public Entity(HashMap<String, Object> properties)
    {
        this.properties = properties;
    }

    public Entity(String value)
    {
        this.addProperty(Blocks.rdfFactory().ensureAbsoluteRdfValue(RDF.type.toString()), Blocks.rdfFactory().ensureAbsoluteRdfValue(value));
    }

    public BlockId getId()
    {
        BlockId retVal = null;
        if (this.properties.containsKey("id")) {
            retVal = (BlockId) this.properties.get("id");
        }
        return retVal;
    }

    public void setId(BlockId id)
    {
        this.properties.put("id", id.toString());
    }

    //    public LinkedHashMap<String, Object> getContext() {
    //        if (!this.properties.containsKey("@context")) {
    //            this.properties.put("@context", new LinkedHashMap<String, Object>());
    //        }
    //        return (LinkedHashMap<String, Object>)this.properties.get("@context");
    //    }
    //
    //    public ArrayList<Object> getGraph() {
    //        if (!this.properties.containsKey("@graph")) {
    //            this.properties.put("@graph", new ArrayList<Object>());
    //        }
    //        return (ArrayList<Object>)this.properties.get("@graph");
    //    }

    public void addProperty(String name, String value)
    {
        this.addProperty(name, value, EntityField.NO_LANGUAGE);
    }

    public void addProperty(String name, String value, String language)
    {
        if (name == null || value == null)
            return;
        if (language == null)
            language = EntityField.NO_LANGUAGE;

        name = Blocks.rdfFactory().ensureAbsoluteRdfValue(name);
        if (!properties.containsKey(name)) {
            properties.put(name, new HashMap<String, Object>());
        }
        if (!(properties.get(name) instanceof Map))
            return;

        HashMap<String, Object> property = ((HashMap<String, Object>) properties.get(name));
        if (!property.containsKey(NO_LANGUAGE)) {
            property.put(NO_LANGUAGE, new ArrayList<Object>());
        }
        if (!property.containsKey(language)) {
            property.put(language, new ArrayList<Object>());
        }
        ArrayList<Object> langValues = (ArrayList<Object>) property.get(language);
        ArrayList<Object> defaultLangValues = (ArrayList<Object>) property.get(NO_LANGUAGE);
        langValues.add(value);
        if (defaultLangValues.size() < langValues.size()) {
            for (int i = defaultLangValues.size(); i < langValues.size(); i++) {
                defaultLangValues.add(langValues.get(i));
            }
        }
        property.put(NO_LANGUAGE, defaultLangValues);
        property.put(language, langValues);
    }

    public void setProperty(String name, String value, int index, String language)
    {
        if (name == null || value == null)
            return;
        if (language == null)
            language = EntityField.NO_LANGUAGE;

        name = Blocks.rdfFactory().ensureAbsoluteRdfValue(name);
        if (!properties.containsKey(name)) {
            properties.put(name, new HashMap<String, Object>());
        }
        if (!(properties.get(name) instanceof Map))
            return;

        HashMap<String, Object> property = ((HashMap<String, Object>) properties.get(name));
        if (!property.containsKey(NO_LANGUAGE)) {
            property.put(NO_LANGUAGE, new ArrayList<Object>());
        }
        if (!property.containsKey(language)) {
            property.put(language, new ArrayList<Object>());
        }
        ArrayList<Object> langValues = (ArrayList<Object>) property.get(language);
        ArrayList<Object> defaultLangValues = (ArrayList<Object>) property.get(NO_LANGUAGE);

        if (langValues.size() > index) {
            langValues.set(index, value);
        }
        else {
            langValues.add(value);
        }
        if (defaultLangValues.size() < langValues.size()) {
            for (int i = defaultLangValues.size(); i < langValues.size(); i++) {
                defaultLangValues.add(langValues.get(i));
            }
        }
        property.put(NO_LANGUAGE, defaultLangValues);
        property.put(language, langValues);

    }

    public void addEntity(String name, Entity value)
    {
        name = Blocks.rdfFactory().ensureAbsoluteRdfValue(name);
        if (!properties.containsKey(name)) {
            properties.put(name, new ArrayList<Object>());
        }
        if (!(properties.get(name) instanceof List))
            return;

        ArrayList<Object> property = (ArrayList<Object>) properties.get(name);

        property.add(value.getProperties());

    }

    public void setEntity(String name, Entity value, int index)
    {
        name = Blocks.rdfFactory().ensureAbsoluteRdfValue(name);
        if (!properties.containsKey(name)) {
            properties.put(name, new ArrayList<Object>());
        }
        if (!(properties.get(name) instanceof List))
            return;

        ArrayList<Object> property = (ArrayList<Object>) properties.get(name);
        if (property.size() > index) {
            property.set(index, value.getProperties());
        }
        else {
            property.add(value.getProperties());
        }

    }

    public Resource getRdfModel(Model model, String id)
    {
        if (model == null)
            return null;

        Resource retVal = null;
        //        EntityField field = PropertyFinder.findProperty(Blocks.rdfFactory().ensureAbsoluteRdfValue("resource"), this.properties,0, "nl");

        if (id != null) {
            retVal = model.createResource(Blocks.rdfFactory().ensureAbsoluteRdfValue(id));
        }
        else {
            retVal = model.createResource();
        }

        for (String property : this.properties.keySet()) {
            if (!property.endsWith("resource")) {
                Object value = this.properties.get(property);
                if (value instanceof List) {
                    // entity
                    for (HashMap<String, Object> entity : (ArrayList<HashMap<String, Object>>) value) {
                        Entity e = Blocks.factory().createEntity(entity);
                        retVal.addProperty(new PropertyImpl(Blocks.rdfFactory().ensureAbsoluteRdfValue(property)), e.getRdfModel(model, null));
                    }
                }
                else if (value instanceof Map) {
                    for (String lang : ((HashMap<String, ArrayList>) value).keySet()) {
                        ArrayList<String> values = ((HashMap<String, ArrayList<String>>) value).get(lang);
                        for (String propertyValue : values) {
                            if (lang.equals("default")) {
                                retVal.addProperty(new PropertyImpl(property), propertyValue);
                            }
                            else {
                                retVal.addProperty(new PropertyImpl(property), propertyValue, lang);
                            }
                        }
                    }
                }
            }
        }
        return retVal;
    }

    public ArrayList<Entity> flatten(ArrayList<Entity> list)
    {
        for (Object o : this.properties.values()) {
            if (o instanceof Entity) {
                ((Entity) o).flatten(list);
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
    @Override
    public String getCreatedBy()
    {
        return createdBy;
    }
    @Override
    public void setCreatedBy(String created_by)
    {
        this.createdBy = created_by;
    }
    @Override
    public String getUpdatedBy()
    {
        return updatedBy;
    }
    @Override
    public void setUpdatedBy(String updatedBy)
    {
        this.updatedBy = updatedBy;
    }
    @Override
    public String getCreatedAt()
    {
        return createdAt;
    }
    @Override
    public void setCreatedAt(String createdAt)
    {
        this.createdAt = createdAt;
    }
    @Override
    public String getUpdatedAt()
    {
        return updatedAt;
    }
    @Override
    public void setUpdatedAt(String updatedAt)
    {
        this.updatedAt = updatedAt;
    }
}

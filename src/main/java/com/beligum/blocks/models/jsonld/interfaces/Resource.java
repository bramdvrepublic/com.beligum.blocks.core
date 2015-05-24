package com.beligum.blocks.models.jsonld.interfaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by wouter on 24/04/15.
 */
public interface Resource extends Node
{
    public final String ID = "@id";
    public final String TYPE = "@rdftype";

    public final String ABSTRACT_CLASS = "AbstractResource";
    public final String DEFAULT_CLASS = "DefaultResource";
    public final String LOCALIZED_CLASS = "LocalizedResource";
    public final String VERSIONED_CLASS = "VersionedResource";

    // Name of the edges pointing to localized versions of the default resource
    // Name of the edge is LOCALIZED + "_" + {language}
    public final String LOCALIZED = "localized";
    public final String NOT_LOCALIZED = "NotLocalized";

    // Fieldname for the language of a localized resource
    public final String LANGUAGE = "@language";


    public void add(String key, Node node);

    public void set(String key, Node node);

    public Node getFirst(String key);

    public Node get(String key);

    public Object getDBId();

    public Object setDBId();

    public String getBlockId();

    public void setBlockId(String id);

    public Node getRdfType();

    public void setRdfType(Node node);

    public boolean isEmpty();

    public Node remove(String key);


    public Boolean getBoolean(String key);

    public Integer getInteger(String key);

    public Long getLong(String key);

    public Double getDouble(String key);

    public String getString(String key);

    public Iterable<Node> getIterable(String key);

    public Resource getResource(String key);

    public Set<String> getFields();

    public Resource copy();

    public HashMap<String, Node> unwrap();

    public void wrap(HashMap<String, Node> unwrappedResource);

    public void merge(Resource resource);

}

package com.beligum.blocks.models.jsonld;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by wouter on 24/04/15.
 */
public interface Resource extends Node
{

    public void add(String key, Node node);

    public void set(String key, Node node);

    public Node getFirst(String key);

    public Node get(String key);

    public String getId();

    public void setId(String id);

    public boolean isEmpty();

    public void remove(String key);

    public void addBoolean(String key, Boolean value);

    public void addInteger(String key, Integer value);

    public void addLong(String key, Long value);

    public void addDouble(String key, Double value);

    public void addString(String key, String value, String language);

    public void setBoolean(String key, Boolean value);

    public void setInteger(String key, Integer value);

    public void setLong(String key, Long value);

    public void setDouble(String key, Double value);

    public void setString(String key, String value, String language);

    public void setString(String key, String value);

    public Boolean getBoolean(String key);

    public Integer getInteger(String key);

    public Long getLong(String key);

    public Double getDouble(String key);

    public String getString(String key);

    public ArrayList<Node> getList(String key);

    public Resource getResource(String key);

    public Set<String> getFields();

    public Resource copy();

    public HashMap<String, Node> unwrap();

    public void wrap(HashMap<String, Node> unwrappedResource);



}

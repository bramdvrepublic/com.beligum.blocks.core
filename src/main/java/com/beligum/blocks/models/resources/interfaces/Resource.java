package com.beligum.blocks.models.resources.interfaces;

import java.util.Set;

/**
 * Created by wouter on 24/04/15.
 */
public interface Resource extends Node
{



    public void add(String key, Node node);

    public void set(String key, Node node);

    public Node remove(String key);

    public Node get(String key);



    public Object getDBId();

    public String getBlockId();

    public Node getRdfType();

    public void setRdfType(Node node);

    public boolean isEmpty();

    public Set<String> getFields();

    public Resource copy();

    public void wrap(Resource resource);

    public void merge(Resource resource);

}

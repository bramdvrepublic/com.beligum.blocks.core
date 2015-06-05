package com.beligum.blocks.resources.interfaces;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by wouter on 24/04/15.
 */
public interface Resource extends Node
{



    public void add(URI key, Node node);

    public void set(URI key, Node node);

    public Node remove(URI key);

    public Node get(URI key);

    public Node get(String field);

    public Object getDBId();

    public URI getBlockId();

    public Node getRdfType();

    public void setRdfType(Node node);

    public HashMap<String, String> getContext();

    public boolean isEmpty();

    public Set<URI> getFields();

    public void merge(Resource resource);

    public void setCreatedAt(Date date);

    public Calendar getCreatedAt();

    public void setCreatedBy(String user);

    public String getCreatedBy();

    public void setUpdatedAt(Date date);

    public Calendar getUpdatedAt();

    public void setUpdatedBy(String user);

    public String getUpdatedBy();

    public String toJson();

}

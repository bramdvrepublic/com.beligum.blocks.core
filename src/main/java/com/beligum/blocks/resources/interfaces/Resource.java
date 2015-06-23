package com.beligum.blocks.resources.interfaces;

import sun.util.resources.CalendarData_cs;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by wouter on 24/04/15.
 */
public interface Resource extends Node, DocumentInfo
{

    public void add(URI key, Node node);

    public void set(URI key, Node node);

    public Node remove(URI key);

    public Node get(URI key);

    public Node get(String field);

    public Object getDBId();

    public URI getBlockId();

    public Set<URI> getRdfType();

    public void setRdfType(Set<URI> uris);

    public HashMap<String, String> getContext();

    public boolean isEmpty();

    public Set<URI> getFields();

    public void merge(Resource resource);

    public String toJson();

}

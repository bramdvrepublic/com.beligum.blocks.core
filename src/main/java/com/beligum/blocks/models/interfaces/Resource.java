package com.beligum.blocks.models.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.URI;
import java.util.*;

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

    public void setDBId(Object id);

    public URI getBlockId();

    public void setBlockId(URI id);

    public Set<URI> getRdfType();

    public void setRdfType(Set<URI> uris);

    public HashMap<String, String> getContext();

    public boolean isEmpty();

    public Set<URI> getFields();

    public Set<URI> getLocalizedFields();

    public Set<URI> getRootFields();

    public void merge(Resource resource);

    public String toJson() throws JsonProcessingException;

    public void setLanguage(Locale locale);

}

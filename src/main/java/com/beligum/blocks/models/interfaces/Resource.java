package com.beligum.blocks.models.interfaces;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by wouter on 24/04/15.
 */
public interface Resource extends Node, DocumentInfo
{

    public void add(URI key, Node node);

    public void set(URI key, Node node);

    public Node remove(URI key);

    public Node remove(URI field, Locale locale);

    public Node get(URI key);

    public Node get(String field);

    public Node get(URI field, Locale locale);

    public Node get(String field, Locale locale);

    public Object getDBId();

    public void setDBId(Object id);

    public URI getBlockId();

    public void setBlockId(URI id);

    public Set<URI> getRdfType();

    public void setRdfType(URI uri);

    public void addRdfType(URI uri);

    public Map<String, String> getContext();

    public boolean isEmpty();

    public Set<Locale> getLocalesForField(URI field);

    public Set<URI> getFields();

    public Set<URI> getLocalizedFields();

    public Set<URI> getRootFields();

    public void setLanguage(Locale locale);

}

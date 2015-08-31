package com.beligum.blocks.models.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Created by wouter on 6/07/15.
 */
public interface ResourceFactory
{
    public Resource createResource(URI id, URI rdfType, Locale language);
    public WebPage createWebPage(URI id, Locale locale);
    public Node createNode(Object value, Locale language);
//    public WebPath createPath(URI masterPage, Path path, Locale locale);
//    public boolean isResource(Object value);
//    public Resource getResource(Object value, Locale language);
//    public URI getResourceId(Object value);

    public Resource deserializeResource(byte[] source, Locale locale) throws IOException;
    public WebPage deserializeWebpage(byte[] source, Locale locale) throws IOException;

    public String serializeResource(Resource resource, boolean makeReferences) throws JsonProcessingException;
    public String serializeWebpage(WebPage page, boolean makeReferences) throws JsonProcessingException;


}

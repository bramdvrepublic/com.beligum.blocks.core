package com.beligum.blocks.models.interfaces;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Created by wouter on 6/07/15.
 */
public interface ResourceFactory
{
    public Resource createResource(URI id, URI rdfType, Locale language);
    public WebPage createWebPage(URI masterWebPage, URI id, Locale locale);
    public Node createNode(Object value, Locale language);
    public WebPath createPath(URI masterPage, Path path, Locale locale);
}

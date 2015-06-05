package com.beligum.blocks.resources.interfaces;

import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 13/05/15.
 */
public interface ResourceController
{
    public Resource createResource(URI id, Locale language);

    public Resource createResource(URI id, URI rdfType, Locale language);

    public Node asNode(Object value, Locale language);

    public Resource getResourceWithBlockId(String id, Locale language);

}

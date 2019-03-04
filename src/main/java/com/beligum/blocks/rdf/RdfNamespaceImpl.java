package com.beligum.blocks.rdf;

import com.beligum.blocks.rdf.ifaces.RdfNamespace;

import java.net.URI;

public class RdfNamespaceImpl implements RdfNamespace
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final URI uri;
    private final String prefix;

    //-----CONSTRUCTORS-----
    public RdfNamespaceImpl(String uri, String prefix)
    {
        this(URI.create(uri), prefix);
    }
    public RdfNamespaceImpl(URI uri, String prefix)
    {
        this.uri = uri;
        this.prefix = prefix;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getUri()
    {
        return uri;
    }
    @Override
    public String getPrefix()
    {
        return prefix;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

package com.beligum.blocks.utils;

import java.net.URI;

/**
 * A simple wrapper around URI to augment it with a name/title for easier HTML generation.
 * Created by bram on Aug 20, 2019
 */
public class NamedUri
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI uri;
    private String name;

    //-----CONSTRUCTORS-----
    public NamedUri(URI uri)
    {
        this(uri, null);
    }
    public NamedUri(URI uri, String name)
    {
        this.uri = uri;
        this.name = name;
    }

    public URI getUri()
    {
        return uri;
    }
    public String getName()
    {
        return name;
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MGMT METHODS-----
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof NamedUri)) return false;

        NamedUri namedUri = (NamedUri) o;

        return getUri() != null ? getUri().equals(namedUri.getUri()) : namedUri.getUri() == null;
    }
    @Override
    public int hashCode()
    {
        return getUri() != null ? getUri().hashCode() : 0;
    }
    @Override
    public String toString()
    {
        return this.uri == null ? null : this.uri.toString();
    }
}

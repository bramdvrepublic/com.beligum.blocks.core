package com.beligum.blocks.utils;

import org.apache.commons.lang.StringUtils;

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
    /**
     * Create an instance by appending to the URI value: a space and the title
     */
    public NamedUri(String spaceDelimitedUri)
    {
        // Let's use space delimiters because it's one of the few illegal characters in URIs
        // see https://www.ietf.org/rfc/rfc2396.txt (2.4.3. Excluded US-ASCII Characters)
        if (spaceDelimitedUri.contains(" ")) {
            String[] s = StringUtils.split(spaceDelimitedUri, " ", 2);
            this.uri = URI.create(s[0]);
            this.name = s[1];
        }
        else {
            this.uri = URI.create(spaceDelimitedUri);
            this.name = null;
        }
    }
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
    public boolean hasName()
    {
        return name != null;
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

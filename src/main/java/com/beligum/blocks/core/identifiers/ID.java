package com.beligum.blocks.core.identifiers;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bas on 13.10.14.
 * Super class for identification of objects and resources. It is actually a wrapper for a URI.
 */
public class ID
{
    protected URI id;

    public ID(URI id){
        this.id = id;
    }

    /**
     *
     * @return the URI-representation of this ID
     */
    public URI toURI()
    {
        return id;
    }

    @Override
    public String toString(){
        return id.toString();
    }
}

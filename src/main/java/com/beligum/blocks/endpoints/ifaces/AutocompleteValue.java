package com.beligum.blocks.endpoints.ifaces;

import java.net.URI;

/**
 * Created by bram on 3/12/16.
 */
public interface AutocompleteValue
{
    URI getResourceUri();
    URI getResourceType();
    String getText();
    URI getLink();
}

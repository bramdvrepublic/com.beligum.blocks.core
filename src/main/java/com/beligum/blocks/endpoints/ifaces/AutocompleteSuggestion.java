package com.beligum.blocks.endpoints.ifaces;

import java.net.URI;

/**
 * Created by bram on 3/12/16.
 */
public interface AutocompleteSuggestion
{
    URI getResourceId();
    URI getResourceType();
    String getTitle();
    String getSubTitle();
}

package com.beligum.blocks.filesystem.pages.ifaces;

import com.beligum.blocks.filesystem.ifaces.ResourceMetadata;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

public interface PageMetadata extends ResourceMetadata
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * Returns the public, absolute addresses of the pages that act as translations for this page.
     * @return
     */
    Map<Locale, URI> getTranslations();
}

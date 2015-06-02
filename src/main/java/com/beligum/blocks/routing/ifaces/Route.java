package com.beligum.blocks.routing.ifaces;

import com.beligum.blocks.routing.ifaces.nodes.WebNode;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Created by wouter on 1/06/15.
 */
public interface Route
{
    public boolean exists();

    public void create();

    // Path without language
    public Path getPath();

    // Path with language
    public Path getLanguagedPath();

    public Locale getLocale();

    public URI getURI();

    public WebNode getNode();
}

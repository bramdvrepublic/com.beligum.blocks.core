package com.beligum.blocks.pages.ifaces;

import java.net.URI;
import java.nio.file.Path;

/**
 * Created by bram on 1/14/16.
 */
public interface Page
{
    Path getPath();
    URI getURI();
    Path getLockFile();
}

package com.beligum.blocks.pages.ifaces;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 1/14/16.
 */
public interface PageStore
{
    void save(URI page, String content) throws IOException;
}

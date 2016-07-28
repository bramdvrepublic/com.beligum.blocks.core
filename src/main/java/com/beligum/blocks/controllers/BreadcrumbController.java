package com.beligum.blocks.controllers;

import com.beligum.base.server.R;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.templating.blocks.DefaultTemplateController;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 11/08/15.
 */
public class BreadcrumbController extends DefaultTemplateController
{
    @Override
    public void created()
    {

    }

    /**
     * @return a breadcrumb map.entry list containing <url, title> entries.
     */
    public Iterator<Map.Entry<URI, String>> breadcrumbs() throws IOException
    {
        LinkedList<Map.Entry<URI, String>> retVal = new LinkedList<>();

        // get URI
        URI requestedUri = R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri();

        PageIndexConnection conn = StorageFactory.getMainPageIndexer().connect();

        URI uri = requestedUri;
        Set<String> encounteredIds = new HashSet<>();
        while (uri != null) {

            PageIndexEntry p = conn.get(uri);

            if (p!=null) {
                //we're working top-down, do only the first (most specific) uri will be added, hope that's ok (otherwise we have doubles, which is not ok at all)
                if (!encounteredIds.contains(p.getResource())) {
                    retVal.add(new DefaultMapEntry(URI.create(p.getId()), p.getTitle()));
                    encounteredIds.add(p.getResource());
                }
            }

            uri = uri.getPath().endsWith("/") ? uri.getPath().equals("/") ? null : uri.resolve("..") : uri.resolve(".");
        }

        return retVal.descendingIterator();
    }

}

package com.beligum.blocks.controllers;

import com.beligum.base.server.R;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.pages.ReadOnlyPage;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.templating.blocks.DefaultTemplateController;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.apache.hadoop.fs.FileContext;

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


    /**
     * Look in our file system and search for the parent of this document (with the same language).
     */
    private URI getParentUri(URI pageUri, FileContext fc) throws IOException
    {
        URI retVal = null;

        URI parentUri = StringFunctions.getParent(pageUri);
        while (retVal == null) {
            if (parentUri == null) {
                break;
            }
            else {
                //note: this is null proof
                Page parentPage = new ReadOnlyPage(pageUri);
                if (fc.util().exists(parentPage.getResourcePath().getLocalPath())) {
                    retVal = parentUri;
                }
                else {
                    parentUri = StringFunctions.getParent(parentUri);
                }

            }
        }

        return retVal;
    }
}

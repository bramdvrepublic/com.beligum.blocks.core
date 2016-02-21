package com.beligum.blocks.controllers;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.index.ifaces.PageIndexer;
import com.beligum.blocks.templating.blocks.DefaultTemplateController;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public List<Map.Entry<URI, String>> breadcrumbs() throws IOException
    {
        Map<URI, String> retVal = new LinkedHashMap<>();

        // get URI
        URI requestedUri = R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri();

        PageIndexer mainPageIndexer = StorageFactory.getMainPageIndexer();
        try (PageIndexConnection conn = mainPageIndexer.connect()) {
            PageIndexEntry p = conn.get(requestedUri);
            while (p != null) {
                retVal.put(p.getId(), p.getTitle());
                p = p.getParent() != null ? conn.get(p.getParent()) : null;
            }
        }
        catch (Exception e) {
            Logger.error("Exception caught while building breadcrumbs list; "+requestedUri, e);
        }

        return Lists.reverse(new ArrayList<>(retVal.entrySet()));
    }

}

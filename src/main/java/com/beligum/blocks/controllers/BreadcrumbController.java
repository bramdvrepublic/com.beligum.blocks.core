package com.beligum.blocks.controllers;

import com.beligum.base.server.R;
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

        //TODO
//        PageIndexConnection conn = StorageFactory.getMainPageIndexer().connect();
//        PageIndexEntry p = conn.get(requestedUri);
//        while (p != null) {
//            retVal.put(p.getId(), p.getTitle());
//            p = p.getParent() != null ? conn.get(URI.create(p.getParent())) : null;
//        }

        return Lists.reverse(new ArrayList<>(retVal.entrySet()));
    }

}

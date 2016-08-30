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
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    @Override
    public void created()
    {

    }

    //-----PUBLIC METHODS-----
    /**
     * @return a breadcrumb map.entry list containing <url, title> entries.
     */
    public Iterator<Map.Entry<URI, String>> breadcrumbs() throws IOException
    {
        LinkedList<Map.Entry<URI, String>> retVal = new LinkedList<>();

        PageIndexConnection conn = StorageFactory.getMainPageIndexer().connect();

        URI uri = R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri();
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

            //we go one level up
            //Note that this is a bit tricky in our case because if we build the paths to the root of eg. /nl/opzoeken/bakovens/inventaris
            // we should iterate all these:
            // - /nl/opzoeken/bakovens/
            // - /nl/opzoeken/bakovens
            // - /nl/opzoeken/
            // - /nl/opzoeken
            // - /nl/
            //Note that because 'all' (what about the resources with ?lang) pages are prefixed with a language, this should render in the right language

            //just chop off the last slash
            if (uri.getPath().endsWith("/")) {
                uri = URI.create(uri.getPath().substring(0, uri.getPath().length()-1));
            }
            //removes the name part
            else {
                uri = uri.resolve(".");
            }

            //this is where we draw the line
            if (uri.getPath().length()<=3) {
                uri = null;
            }
        }

        //the breadcrumb bar isn't really designed to be empty
        if (retVal.isEmpty()) {
            //give it one more shot and try to find the home page with a simple heuristic
            PageIndexEntry p = conn.get(URI.create("/"+R.i18nFactory().getOptimalLocale().getLanguage()+"/"));
            if (p!=null) {
                retVal.add(new DefaultMapEntry(URI.create(p.getId()), p.getTitle()));
            }

            //just add the root with a general terms as the final attempt
            if (retVal.isEmpty()) {
                retVal.add(new DefaultMapEntry(URI.create("/"), gen.com.beligum.blocks.core.messages.blocks.core.Entries.breadcrumbHomeTitle.getI18nValue()));
            }
        }

        return retVal.descendingIterator();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Look in our file system and search for the parent of this document (with the same language).
     * @deprecated old code, now using index instead
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

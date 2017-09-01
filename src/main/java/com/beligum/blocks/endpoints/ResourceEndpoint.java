package com.beligum.blocks.endpoints;

import com.beligum.base.filesystem.HtmlFile;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.server.R;
import com.beligum.blocks.templating.blocks.TemplateCache;
import gen.com.beligum.blocks.core.fs.html.templates.blocks.core.snippets.sidebar;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Some dynamic enpoints, dealing with the resources
 * <p>
 * Created by bas on 21.10.14.
 */
@Path("/")
public class ResourceEndpoint
{
    /**
     * Returns a css reset code of all known blocks in the system (resetting padding, margin and display)
     */
    @GET
    @Path("/assets/blocks/core/styles/imports/reset.css")
    public Resource getResources()
    {
        //note: by returning a resource instead of a response, we pass through the caching mechanisms
        return R.resourceManager().create(new StringSource(TemplateCache.instance().getCssReset(), MimeTypes.CSS, null));
    }

    /**
     * As JS plugin with all known block names in this system
     */
    @GET
    @Path("/assets/blocks/core/scripts/imports/all.js")
    public Resource getImportsArray()
    {
        //note: by returning a resource instead of a response, we pass through the caching mechanisms
        return R.resourceManager().create(new StringSource(TemplateCache.instance().getJsArray(), MimeTypes.JAVASCRIPT, null));
    }

    /**
     * The code for the JS-loaded sidebar
     */
    @GET
    @Path("/templates/blocks/core/snippets/sidebar.html")
    public HtmlFile getSidebar()
    {
        return sidebar.get();
    }
}

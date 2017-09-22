/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

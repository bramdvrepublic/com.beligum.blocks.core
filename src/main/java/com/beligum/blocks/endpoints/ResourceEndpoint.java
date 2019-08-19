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
import com.beligum.base.resources.repositories.AssetsRepository;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.server.R;
import com.beligum.blocks.templating.TemplateCache;
import gen.com.beligum.blocks.core.fs.html.templates.blocks.core.snippets.sidebar;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static gen.com.beligum.base.core.constants.base.core.*;

/**
 * Some dynamic endpoints, dealing with the resources
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
    @Path(AssetsRepository.PUBLIC_PATH_PREFIX + "blocks/core/styles/imports/styles.css")
    @RequiresPermissions(ASSET_VIEW_ALL_PERM)
    public Resource getImportsStyles()
    {
        //note: by returning a resource instead of a response, we pass through the caching mechanisms
        return R.resourceManager().create(new StringSource(TemplateCache.instance().getCssReset(), MimeTypes.CSS, null));
    }

    /**
     * As JS plugin with all known block names in this system
     */
    @GET
    @Path(AssetsRepository.PUBLIC_PATH_PREFIX + "blocks/core/scripts/imports/scripts.js")
    @RequiresPermissions(ASSET_VIEW_ALL_PERM)
    public Resource getImportsScripts()
    {
        //note: by returning a resource instead of a response, we pass through the caching mechanisms
        return R.resourceManager().create(new StringSource(TemplateCache.instance().getJsArray(), MimeTypes.JAVASCRIPT, null));
    }

    /**
     * The code for the JS-loaded sidebar
     */
    @GET
    @Path("/templates/blocks/core/snippets/sidebar.html")
    @RequiresPermissions(ASSET_VIEW_ALL_PERM)
    public HtmlFile getSidebar()
    {
        return sidebar.get();
    }
}

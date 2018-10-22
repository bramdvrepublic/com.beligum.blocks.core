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

import com.beligum.base.resources.ifaces.ResourceAction;
import com.beligum.base.server.R;
import com.beligum.base.utils.toolkit.NetworkFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.endpoints.utils.PageRdfResource;
import com.beligum.blocks.endpoints.utils.PageRouter;
import com.beligum.blocks.rdf.ifaces.Format;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import static gen.com.beligum.blocks.core.constants.blocks.core.*;

/**
 * Created by bram on 2/10/16.
 */
@Path("/")
public class ApplicationEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Note: when the favicon is requested (a lot), we don't need to boot up the entire page-lookup below,
     * but can skip it (because it needs to be below the /assets path anyway)
     */
    @GET
    @Path("/{path:favicon.*}")
    public Response getFavicon()
    {
        throw new NotFoundException();
    }

    /**
     * The general entry point for a saved page. This method is both called directly to fetch a page,
     * but also indirectly when redirected from PageAdminEndpoint.getPageTemplate()
     * Use the X-Anonymous header to signal you want this page to return as if the current user wasn't logged in.
     *
     * Note: don't delete the method rawPath argument; it enables us to construct hooks into this method via the reverse router
     */
    @GET
    @Path("/{path:.*}")
    @RequiresPermissions(PAGE_READ_ALL_HTML_PERM)
    public Response getPage(@PathParam("path") String rawPath, @HeaderParam(PAGE_ANONYMOUS_HEADER) @DefaultValue("false") boolean anonymous)
    {
        Response.ResponseBuilder retVal = null;

        PageRouter requestRouter = new PageRouter(anonymous);

        //if we need to redirect away, send a 303
        if (requestRouter.needsRedirection() && requestRouter.getTargetUri() != null) {

            retVal = Response.seeOther(requestRouter.getTargetUri());

            //if we're redirecting straight away, but we have some entities in the flash cache, we'll propagate them again,
            //otherwise we would lose eg. feedback messages (eg. when a successful login redirects automatically from "/" to "/en/")
            R.cacheManager().getFlashCache().resurrect();
        }
        //if we got a valid RDF type query parameter, return RDF instead,
        //but if RDFA was requested (it's also in the supported formats), keep on serving html
        else if (requestRouter.getTargetRdfType() != null && !requestRouter.getTargetRdfType().equals(Format.RDFA)) {
            //invoke the extra check for explicit RDF-ization of the page
            R.securityManager().checkPermission(PAGE_READ_ALL_RDF_PERM);

            retVal = Response.ok(new PageRdfResource(requestRouter.getRequestedPage(), requestRouter.getTargetRdfType()));
        }
        //if we found a page, create the template and check if we have permission to edit it
        else if (requestRouter.getRequestedPage() != null) {

            retVal = Response.ok(requestRouter.buildRequestedPageTemplate());
        }
        else if (requestRouter.getAdminTemplate() != null) {

            retVal = Response.ok(requestRouter.getAdminTemplate());
        }

        if (retVal == null) {
            throw new NotFoundException("Can't find this page and you have no rights to create it; " + requestRouter.getRequestedUri());
        }
        else {
            //when we edit or create pages, we disable caching to prevent stale page states from ending up in the
            //browser, causing trouble on save. This happens eg. when the user hits the back button by accident,
            //navigating away, then hitting forward again and hitting save, ending up with possibly good information being
            // overwritten by stale page html
            ResourceAction resourceAction = R.cacheManager().getRequestCache().get(CacheKeys.RESOURCE_ACTION);
            if (!(resourceAction == null || resourceAction.equals(ResourceAction.READ) || resourceAction.equals(ResourceAction.NONE))) {
                NetworkFunctions.disableCaching(retVal);
            }

            return retVal.build();
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

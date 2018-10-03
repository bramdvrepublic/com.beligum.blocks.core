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

import com.beligum.base.server.R;
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
    @GET
    @Path("/{path:.*}")
    @RequiresPermissions(PAGE_READ_ALL_HTML_PERM)
    //note: don't delete the method argument; it enables us to construct hooks into this method via the reverse router
    public Response getPage(@PathParam("path") String rawPath)
    {
        Response.ResponseBuilder retVal = null;

        PageRouter requestRouter = new PageRouter();

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
            return retVal.build();
        }
    }
    @GET
    @Path("/{path:.*}")
    //Sync this with com.beligum.blocks.rdf.ifaces.Format
    @Produces({ "application/rdf+xml",
                "application/n-triples",
                "application/ld+json",
                "text/turtle",
                "text/n3",
              }
    )
    @RequiresPermissions(PAGE_READ_ALL_RDF_PERM)
    public Response getPageRdf(@PathParam("path") String rawPath, @HeaderParam(HttpHeaders.ACCEPT) String acceptHeader)
    {
        PageRouter requestRouter = new PageRouter();

        //if we found an external redirect URL, redirect there, otherwise continue
        Response.ResponseBuilder retVal = null;

        if (requestRouter.getTargetUri() != null && requestRouter.needsRedirection()) {
            retVal = Response.seeOther(requestRouter.getTargetUri());
            //see getPage() for why this is used
            R.cacheManager().getFlashCache().resurrect();
        }
        else {
            if (requestRouter.getRequestedPage() != null) {

                Format rdfFormat = Format.fromMimeType(acceptHeader);

                //this is the old version, before the header was added to the method definition
                //                //having a page is not enough, the requested RDF format (mime type) needs to be
                //                //supported as well, so we'll iterate all acceptable media types and try to convert them
                //                //to supported RDF formats, stopping once we found one that matches
                //                for (MediaType m : R.requestContext().getJaxRsRequest().getAcceptableMediaTypes()) {
                //                    //note that we strip the possible parameters and charset from the media type
                //                    //and reconstruct the mimetype from its parts so the match is as broad as possible
                //                    rdfFormat = Format.fromMimeType(m.getType() + "/" + m.getSubtype());
                //                    if (rdfFormat != null) {
                //                        break;
                //                    }
                //                }

                if (rdfFormat != null) {
                    retVal = Response.ok(new PageRdfResource(requestRouter.getRequestedPage(), rdfFormat));
                }
            }
        }

        if (retVal == null) {
            throw new NotFoundException("Can't find RDF page; " + requestRouter.getRequestedUri());
        }
        else {
            return retVal.build();
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

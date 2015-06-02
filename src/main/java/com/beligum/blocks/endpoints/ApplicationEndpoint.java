package com.beligum.blocks.endpoints;

import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.routing.HtmlRouter;
import com.beligum.blocks.routing.ifaces.Route;
import com.beligum.blocks.routing.ifaces.Router;
import com.beligum.blocks.routing.nodes.ORouteNodeFactory;
import com.beligum.blocks.routing.RouteImpl;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/")
public class ApplicationEndpoint
{

    /*
    * Every resource on this domain has a url as id in for http://xxx.org/v1/resources/...
    *
    * These resources are mapped to clean urls in the routing table in the db.
    * Currently there ar 2 types of routes:
    * - OKURL: shows a resource (normally a view with optionally an other resource as argument, based on the current path
    * - MovedPermanentlyURL: redirects to an other url
    *
    * Language is not a part of the url-path in the database.
    *
    * */

    @Path(ParserConstants.RESOURCE_ENDPOINT + "{block_id:.*}")
    @GET
    public Response getPageWithId(@PathParam("block_id") String blockId, @QueryParam("resource") String resource_block_id, @QueryParam("language") String lang)
    {


        return Response.ok().build();
    }


    /*
    * using regular expression to let all requests to undefined paths end up here
    * We try to find these urls in our routing table and redirect them to the correct url
    * */

    @Path("/{randomPage:.*}")
    @GET
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath, @QueryParam("version") Long version, @QueryParam("deleted") @DefaultValue("false") boolean fetchDeleted)
                    throws Exception
    {

        URI currentURI = RequestContext.getJaxRsRequest().getUriInfo().getRequestUri();
        Route route = new RouteImpl(currentURI, ORouteNodeFactory.instance());

        Router router = new HtmlRouter(route);

        return router.response();
    }

    /**
     * Injects all parameters needed to create a new page into a template.
     *
     * @param newPageTemplate
     */
    private Response injectParameters(Template newPageTemplate)
    {
        //        List<Blueprint> pageBlocks = Blocks.templateCache().getPageBlocks();
        newPageTemplate.set(ParserConstants.ENTITY_URL, RequestContext.getJaxRsRequest().getUriInfo().getRequestUri().toString());
        //        newPageTemplate.set(ParserConstants.BLUEPRINTS, pageBlocks);
        return Response.ok(newPageTemplate).build();
    }

}
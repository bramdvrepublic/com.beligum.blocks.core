package com.beligum.blocks.endpoints;

import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.database.OBlocksDatabase;
import com.beligum.blocks.routing.HtmlRouter;
import com.beligum.blocks.routing.ifaces.Router;
import com.beligum.blocks.routing.Route;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Locale;

@Path("/")
public class ApplicationEndpoint
{

    @Path("/favicon.ico")
    @GET
    public Response favicon() {
        throw new NotFoundException();
    }

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
        Response retVal;
        URI currentURI = RequestContext.getJaxRsRequest().getUriInfo().getRequestUri();

        Route route = new Route(currentURI, OBlocksDatabase.instance());
        if (!route.getLocale().equals(Locale.ROOT)) {
            Router router = new HtmlRouter(route);
            retVal = router.response();
            // Todo Remove when this sits in db
//            OBlocksDatabase.instance().getGraph().commit();
        } else {
            URI url = UriBuilder.fromUri(BlocksConfig.instance().getSiteDomain()).path(BlocksConfig.instance().getDefaultLanguage().getLanguage()).path(route.getLanguagedPath().toString()).build();
            retVal = Response.seeOther(url).build();
        }
        return retVal;
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
package com.beligum.blocks.endpoints;

import com.beligum.base.server.RequestContext;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.database.DummyBlocksController;
import com.beligum.blocks.routing.HtmlRouter;
import com.beligum.blocks.routing.ifaces.Router;
import com.beligum.blocks.routing.Route;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

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
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath)
                    throws Exception
    {
        Response retVal;
        URI currentURI = RequestContext.getJaxRsRequest().getUriInfo().getRequestUri();

        Route route = new Route(currentURI, DummyBlocksController.instance());
        if (!route.getLocale().equals(Locale.ROOT)) {
            Router router = new HtmlRouter(route);
            retVal = router.response();
        } else {
            URI url = UriBuilder.fromUri(BlocksConfig.instance().getSiteDomain()).path(BlocksConfig.instance().getDefaultLanguage().getLanguage()).path(route.getLanguagedPath().toString()).build();
            retVal = Response.seeOther(url).build();
        }
        return retVal;
    }

    //    @GET
    //    @Path("{getLanguage:.*}/resource/{name:.*}")
    //    public Response showResource(@PathParam("getLanguage") String getLanguage, @PathParam("name") String name, @QueryParam("q") String query, @DefaultValue("1") @QueryParam("page") long page) throws Exception
    //    {
    //        int RESOURCES_ON_PAGE = 25;
    //        Locale locale = BlocksConfig.instance().getLocaleForLanguage(getLanguage);
    //        java.nio.file.Path path = Paths.get(name);
    //        if (path.getNameCount() > 1 || !locale.getLanguage().equals(getLanguage)) {
    //            return getPageWithId(RequestContext.getJaxRsRequest().getUriInfo().getRequestUri().getPath().toString());
    //        }
    //
    //
    //        String resourceName = path.getName(0).toString();
    //        String templateName = null;
    //        for (HtmlTemplate template: HtmlParser.getCachedTemplates().values()) {
    //            if (template.getTemplateName().equals("mot-"+resourceName+"-basic")) {
    //                templateName = template.getTemplateName();
    //                break;
    //            }
    //        }
    //        if (templateName != null) {
    //            // do search
    //            QueryBuilder termQuery;
    //            QueryBuilder typeQuery = QueryBuilders.matchQuery(ParserConstants.JSONLD_TYPE, RdfTools.createLocalType(resourceName).toString());
    //            if (query != null) {
    //                termQuery = QueryBuilders.termQuery("_all", query);
    //            } else {
    //                termQuery = QueryBuilders.matchAllQuery();
    //            }
    //
    //            QueryBuilder dbQuery = QueryBuilders.boolQuery().must(termQuery).must(typeQuery);
    //
    //            CountRequestBuilder count = ElasticSearchClient.instance().getClient().prepareCount(ElasticSearchServer.instance().getResourceIndexName(locale)).setQuery(dbQuery);
    //            CountResponse response = count.execute().actionGet();
    //
    //
    //
    //
    //
    //
    ////            SearchRequestBuilder searchRequest = ElasticSearchClient.instance().getClient().prepareSearch(ElasticSearchServer.instance().getResourceIndexName(locale)).setQuery(dbQuery).addField(
    ////                            "@id").setFetchSource(true).setFrom((int) ((page - 1) * RESOURCES_ON_PAGE)).setSize(RESOURCES_ON_PAGE);
    ////
    ////            if (resourceName.toLowerCase().equals("waterwell") || resourceName.toLowerCase().equals("bakehouse")) {
    ////                searchRequest = searchRequest.addSort("www_mot_be_ontology_address.www_mot_be_ontology_country", SortOrder.ASC);
    ////            }
    ////            SearchResponse searchResponse = searchRequest.execute().actionGet();
    //
    //
    ////            Map<String, Object> source = searchResponse.getHits().getAt(i).getSource();
    ////            Resource resource = new DummyResource(source, new HashMap<String, Object>(), locale);
    //
    //
    //
    //            return Response.ok(retVal.toString()).build();
    //
    //        } else {
    //            throw new NotFoundException();
    //        }
    //
    //    }



}
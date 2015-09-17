package com.beligum.blocks.endpoints;

import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.controllers.PersistenceControllerImpl;
import com.beligum.blocks.routing.HtmlRouter;
import com.beligum.blocks.routing.Route;
import com.beligum.blocks.routing.ifaces.Router;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@Path("/")
public class ApplicationEndpoint
{
    //TODO implement this?
    @Path("/favicon.ico")
    @GET
    public Response favicon()
    {
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

    //TODO this is the endpoint url of a resource, we don't know what to reply here yet
    //    @Path(ParserConstants.RESOURCE_ENDPOINT + "{block_id:.*}")
    //    @GET
    //    public Response getPageWithId(@PathParam("block_id") String blockId, @QueryParam("resource") String resource_block_id, @QueryParam("language") String lang)
    //    {
    //        return Response.ok().build();
    //    }


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

        Route route = new Route(currentURI, PersistenceControllerImpl.instance());
        if (!route.getLocale().equals(Locale.ROOT)) {
            Router router = new HtmlRouter(route);
            retVal = router.response();
        }
        else {
            Locale locale = BlocksConfig.instance().getDefaultLanguage();

            // We have to redirect the user to a url containing a language
            // We use the language of the referrer if there is one in that url
            // otherwise redirect to default language
            String referrer = RequestContext.getJaxRsRequest().getHeaderString("Referer");
            if (referrer != null) {
                try {
                    URI ref = new URI(referrer);
                    java.nio.file.Path path = Paths.get(ref.getPath());
                    if (path.getNameCount() > 0) {
                        String lang = path.getName(0).toString();
                        // This will return the default language if no language was found
                        locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                    }

                } catch (Exception e) {
                    // We do nothing with the exception. User will be redirected to the default language
                    Logger.error("Referrer in header is not a valid URI");
                }
            }

            URI url = UriBuilder.fromUri(BlocksConfig.instance().getSiteDomain()).path(locale.getLanguage()).path(route.getLanguagedPath().toString()).build();
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
    //        for (HtmlTemplate template: HtmlParser.getTemplateCache().values()) {
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
    //            CountRequestBuilder count = ElasticSearchClient.instance().getClient().prepareCount(ElasticSearch.instance().getResourceIndexName(locale)).setQuery(dbQuery);
    //            CountResponse response = count.execute().actionGet();
    //
    //
    //
    //
    //
    //
    ////            SearchRequestBuilder searchRequest = ElasticSearchClient.instance().getClient().prepareSearch(ElasticSearch.instance().getResourceIndexName(locale)).setQuery(dbQuery).addField(
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
package com.beligum.blocks.endpoints;

import com.beligum.base.resources.ResourceRequestImpl;
import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.HdfsPathInfo;
import com.beligum.blocks.fs.HdfsResource;
import com.beligum.blocks.fs.ifaces.PathInfo;
import com.beligum.blocks.fs.pages.DefaultPageImpl;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import org.apache.hadoop.fs.FileContext;
import org.apache.shiro.SecurityUtils;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/old")
public class ApplicationEndpointOld
{
    @Path("/favicon.ico")
    @GET
    public Response favicon()
    {
        throw new NotFoundException();
    }

    /*
    * Every resource on this domain has a url as id in for http://xxx.org/v1/resources/...
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

//    @Path("/{randomPage:.*}")
//    @GET
//    public Response getPageWithId(@PathParam("randomPage") String randomURLPath) throws Exception
//    {
//        Response retVal;
//        URI currentURI = R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri();
//
//        Route route = new Route(currentURI, PersistenceControllerImpl.instance());
//        if (!route.getLocale().equals(Locale.ROOT)) {
//            Router router = new HtmlRouter(route);
//            retVal = router.response();
//        }
//        else {
//            Locale locale = Settings.instance().getDefaultLanguage();
//
//            // We have to redirect the user to a url containing a language
//            // We use the language of the referrer if there is one in that url
//            // otherwise redirect to default language
//            String referrer = R.requestContext().getJaxRsRequest().getHeaderString(HttpHeaders.REFERER);
//            if (referrer != null) {
//                try {
//                    URI ref = new URI(referrer);
//                    java.nio.file.Path path = Paths.get(ref.getPath());
//                    if (path.getNameCount() > 0) {
//                        String lang = path.getName(0).toString();
//                        // This will return the default language if no language was found
//                        locale = Settings.instance().getLocaleForLanguage(lang);
//                    }
//
//                }
//                catch (Exception e) {
//                    // We do nothing with the exception. User will be redirected to the default language
//                    Logger.error("Referrer in header is not a valid URI");
//                }
//            }
//
//            URI url = UriBuilder.fromUri(Settings.instance().getSiteDomain()).path(locale.getLanguage()).path(route.getLanguagedPath().toString()).replaceQuery(currentURI.getQuery()).build();
//            retVal = Response.seeOther(url).build();
//        }
//
//        return retVal;
//    }
    @Path("/{randomPage:.*}")
    @GET
    public Response getPageNew(@PathParam("randomPage") String randomURLPath) throws Exception
    {
        URI requestedURI = R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri();
        URI validUri = DefaultPageImpl.create(requestedURI, Settings.instance().getPagesViewPath());

        FileContext fs = Settings.instance().getPageViewFileSystem();
        PathInfo pathInfo = new HdfsPathInfo(fs, validUri);

        final Page page = new DefaultPageImpl(pathInfo);

        Template template = R.templateEngine().getNewTemplate(R.resourceFactory().lookup(new HdfsResource(new ResourceRequestImpl(validUri), fs, page.getNormalizedPageProxyPath())));

        //this will allow the blocks javascript/css to be included if we're logged in and have permission
        if (SecurityUtils.getSubject().isPermitted(Permissions.Action.PAGE_MODIFY.getPermission())) {
            this.setBlocksMode(HtmlTemplate.ResourceScopeMode.edit, template.getContext());
        }

        return Response.ok(template).build();
    }

    //    @GET
    //    @Path("{getLanguage:.*}/resource/{name:.*}")
    //    public Response showResource(@PathParam("getLanguage") String getLanguage, @PathParam("name") String name, @QueryParam("q") String query, @DefaultValue("1") @QueryParam("page") long page) throws Exception
    //    {
    //        int RESOURCES_ON_PAGE = 25;
    //        Locale locale = Settings.instance().getLocaleForLanguage(getLanguage);
    //        java.nio.file.Path path = Paths.get(name);
    //        if (path.getNameCount() > 1 || !locale.getLanguage().equals(getLanguage)) {
    //            return getPageWithId(R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri().getPath().toString());
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

    //-----PRIVATE METHODS-----
    private void setBlocksMode(HtmlTemplate.ResourceScopeMode mode, TemplateContext context)
    {
        //this one is used by HtmlParser to test if we need to include certain tags
        R.cacheManager().getRequestCache().put(CacheKeys.BLOCKS_MODE, mode);

        //for velocity templates
        context.set(CacheKeys.BLOCKS_MODE.name(), mode.name());
    }

}
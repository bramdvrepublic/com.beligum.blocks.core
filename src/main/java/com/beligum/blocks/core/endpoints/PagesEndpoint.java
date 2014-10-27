package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.PageClassCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.PageClassCacheException;
import com.beligum.blocks.core.exceptions.PageParserException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Page;
import com.beligum.blocks.core.parsing.PageParser;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

/**
 * Created by bas on 07.10.14.
 */
@Path("/pages")
public class PagesEndpoint
{
    @GET
    @Path("/new")
    public Response newPage(){
        Template template = R.templateEngine().getEmptyTemplate("/views/new.html");
        return Response.ok(template.render()).build();
    }

    @POST
    /**
     * Create a new page-instance of the page-class specified as a parameter
     */
    public Response createPage(@FormParam("page-class-name") /*TODO: bean validation not showing thrown errors? @NotBlank(message = "No pageclass specified.")*/ String pageClassName)
                    throws PageClassCacheException, RedisException, URISyntaxException

    {
        /*
         * Get the page-class (containing the default blocks and rows) from the cache and use it to construct a new page
         */
        Map<String, PageClass> cache = PageClassCache.getInstance().getPageClassCache();
        PageClass pageClass = cache.get(pageClassName);

        //TODO: this try-with-resource block should be set somewhere at the very beginning of the application, so the Redis-instance is closed (and it's connection-pool destroyed) at the end of the application
//        try(Redis redis = Redis.getInstance()) {
        Redis redis = Redis.getInstance();
            Page newPage = redis.getNewPage(pageClass);
            redis.save(newPage);
            /*
             * Redirect the client to the newly created page
             */
            return Response.seeOther(newPage.getUrl().toURI()).build();
//        }
    }

    @PUT
    @Path("/{pageId:.*}")
    /*
     * update a page-instance with id 'pageId' to be the html specified
     */
    public Response updatePage(@PathParam("pageId") String pageId, String html) throws MalformedURLException, PageParserException, RedisException
    {
        URL pageUrl = new URL(BlocksConfig.getSiteDomain() + "/" + pageId);

        PageParser parser = new PageParser();
        Page page = parser.parsePage(html, pageUrl);
        Redis redis = Redis.getInstance();
        redis.save(page);
        return Response.ok().build();
    }

}

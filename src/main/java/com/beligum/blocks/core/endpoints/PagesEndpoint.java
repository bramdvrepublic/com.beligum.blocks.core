package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.PageCache;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.storables.Page;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
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
    public Response createPage(@FormParam("page-class-name") /*TODO: bean validation not showing thrown errors? @NotBlank(message = "No pageclass specified.")*/ String pageClassName) throws URISyntaxException
    {
        /*
         * Get the page-class (containing the default blocks and rows) from the cache and use it to construct a new page
         */
        Map<String, PageClass> cache = PageCache.getInstance().getPageCache();
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
    public Response updatePage(@PathParam("pageId") String pageId, String html)
    {

        return Response.ok().build();
    }

}

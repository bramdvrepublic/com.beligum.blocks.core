package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.PageTemplate;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;
import com.beligum.core.framework.templating.ifaces.TemplateEngine;
import com.beligum.core.framework.templating.velocity.VelocityTemplateEngine;
import org.apache.commons.collections.CollectionUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.RenderTool;
import org.jcp.xml.dsig.internal.dom.Utils;
import org.jsoup.nodes.Element;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Path("/")
public class ApplicationEndpoint
{
    @Path("/")
    @GET
    public Response index()
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/index.html");
//        TypeCacher.instance().reset();
        return Response.ok(indexTemplate).build();
    }

//    @Path("/show")
//    @GET
//    public Response show()
//    {
//        TypeCacher.instance().reset();
//
//        com.beligum.blocks.html.Template template = TypeCacher.instance().getTemplate("default");
//        Element element = TypeCacher.instance().getContent("free");
//
//        return Response.ok(template.renderContent(element)).build();
//    }

    @Path("/reset")
    @GET
    public Response reset()
    {
//        TypeCacher.instance().reset();
        // TODO enable reset of EntityClassCache
        return Response.ok("OK: all templates loaded").build();
    }

    //using regular expression to let all requests to undefined paths end up here
    @Path("/{randomPage:.+}")
    @GET
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath)
    {
        try{
            Redis redis = Redis.getInstance();
            URL url = new URL(RequestContext.getRequest().getRequestURL().toString());
            RedisID lastVersionId = new RedisID(url, redis.getLastVersion(url));
            Entity entity = redis.fetchEntity(lastVersionId, true);
            //TODO: the pagetemplate should be fetched from cache or db
            PageTemplate pageTemplate = PageTemplateCache.getInstance().get("default");
            String page = pageTemplate.renderContent(entity);
            return Response.ok(page).build();
        }
        catch(Exception e){
            throw new NotFoundException("The page '" + randomURLPath + "' could not be found.", e);
        }
    }

}
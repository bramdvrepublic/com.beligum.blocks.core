package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URL;

@Path("/")
public class ApplicationEndpoint
{
    @Path("/index")
    @GET
    public Response index()
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/index.html");
//        TypeCacher.instance().reset();
        return Response.ok(indexTemplate).build();
    }

    @Path("/finder")
    @GET
    public Response finder()
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/finder.html");
        //        TypeCacher.instance().reset();
        return Response.ok(indexTemplate).build();
    }

    @Path("/mot/{name}")
    @GET
    public Response mot(@PathParam("name") String name)
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/templates/mot/"+name+".html");
        //        TypeCacher.instance().reset();
        return Response.ok(indexTemplate).build();
    }

    @Path("/")
    @GET
    public Response overzicht()
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/overzicht.html");
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
        // TODO BAS: enable reset of EntityClassCache
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
            RedisID lastVersionId = new RedisID(url, RedisID.LAST_VERSION);
            EntityTemplate entityTemplate = redis.fetchEntityTemplate(lastVersionId);
            if(entityTemplate == null){
                throw new NullPointerException("Could not find page " + randomURLPath + " in db, received null.");
            }
            String page = entityTemplate.renderEntityInPageTemplate();
            return Response.ok(page).build();
        }
        catch(Exception e){
            throw new NotFoundException("The page '" + randomURLPath + "' could not be found.", e);
        }
    }

}
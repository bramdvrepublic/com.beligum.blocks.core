package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

@Path("/")
public class ApplicationEndpoint
{
//    @Path("/ind")
//    @GET
//    public Response index()
//    {
//        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/index.html");
////        TypeCacher.instance().reset();
//        return Response.ok(indexTemplate).build();
//    }

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
    public Response overzicht() throws URISyntaxException
    {
//        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/overzicht.html");
        //        TypeCacher.instance().reset();
        return Response.seeOther(new URI("/index")).build();
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



    //using regular expression to let all requests to undefined paths end up here
    @Path("/{randomPage:.+}")
    @GET
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath)
    {
        try{
            Redis redis = Redis.getInstance();
            URL url = new URL(RequestContext.getRequest().getRequestURL().toString());
            //if no language info is specified in the url, or if the specified language doesn't exist, the default language will still be shown
            RedisID lastVersionId = new RedisID(url, RedisID.LAST_VERSION, true);
            EntityTemplate entityTemplate = redis.fetchEntityTemplate(lastVersionId);
            if(entityTemplate == null){
                Template template = R.templateEngine().getEmptyTemplate("/views/new-page.html");
                List<EntityTemplateClass> entityTemplateClasses = EntityTemplateClassCache.getInstance().values();
                //TODO BAS: find general way to split entity-classes to be shown when creating a new page and when creating a new block in frontend
                List<EntityTemplateClass> pageClasses = new ArrayList<>();
                for(EntityTemplateClass entityTemplateClass : entityTemplateClasses){
                    if(entityTemplateClass.getName().contains("-page")){
                        pageClasses.add(entityTemplateClass);
                    }
                }
                template.set(ParserConstants.ENTITY_URL, RequestContext.getRequest().getRequestURL().toString());
                template.set(ParserConstants.ENTITY_CLASSES, pageClasses);
                return Response.ok(template).build();
            }
            String page = entityTemplate.renderEntityInPageTemplate();
            return Response.ok(page).build();
        }
        catch(Exception e){
            throw new NotFoundException("The page '" + randomURLPath + "' could not be found.", e);
        }
    }

}
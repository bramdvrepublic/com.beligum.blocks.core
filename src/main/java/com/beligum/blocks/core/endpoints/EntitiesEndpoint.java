package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;
import com.beligum.core.framework.utils.Logger;
import org.hibernate.validator.constraints.NotBlank;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by bas on 07.10.14.
 */
@Path("/entities")
public class EntitiesEndpoint
{

    @GET
    @Path("/reset")
    public Response resetCache() throws CacheException
    {
        EntityTemplateClassCache.getInstance().reset();
        return Response.ok("Cache reset").build();
    }

    @GET
    @Path("/new")
    public Response newPage() throws CacheException
    {
        Template template = R.templateEngine().getEmptyTemplate("/views/new-page.html");
        Collection<EntityTemplateClass> entityTemplateClasses = EntityTemplateClassCache.getInstance().values();
        template.set(ParserConstants.ENTITY_URL, RequestContext.getRequest().getRequestURI());
        template.set(ParserConstants.ENTITY_CLASSES, entityTemplateClasses);
        return Response.ok(template).build();
    }

    @POST
    /**
     * Create a new page-instance of the page-class specified as a parameter
     */
    public Response createEntity(
                    @FormParam("page-url")
                    @NotBlank(message = "No url specified.")
                    String url,
                    @FormParam("page-class-name")
                    @NotBlank(message = "No entity-class specified.")
                    String entityClassName)
                    throws CacheException, RedisException, IDException, URISyntaxException, ParseException, MalformedURLException

    {
        EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(entityClassName);
        URL pageUrl = new URL(url);
        URL entityUrl = TemplateParser.saveNewEntityTemplateToDb(pageUrl, entityTemplateClass);

        /*
         * Redirect the client to the newly created entity's page
         */
        return Response.seeOther(entityUrl.toURI()).build();
    }

    @GET
    @Path("/class/{entityTemplateClassName}")
    public Response getClassTemplate(@PathParam("entityTemplateClassName") String entityTemplateClasName) throws CacheException, ParseException
    {
        String classHtml = TemplateParser.renderEntityClass(EntityTemplateClassCache.getInstance().get(entityTemplateClasName));
        return Response.ok(classHtml).build();
    }


    @PUT
    @Path("/{entityId:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    /*
     * update a page-instance with id 'entityId' to be the html specified
     */
    public Response updateEntity(
                    @PathParam("entityId")
                    String entityId,
                    @NotBlank(message = "No page found to update to.")
                    String html) throws MalformedURLException, ParseException, URISyntaxException, IDException, RedisException
    {
        URL entityUrl = TemplateParser.updateEntity(html);
        //        EntityTemplate storedTemplate = Redis.getInstance().fetchEntityTemplate(new RedisID(entityUrl, RedisID.LAST_VERSION));
        //        if(storedTemplate == null){
        //            RedisID newInstanceID = new RedisID(entityUrl);
        //            EntityTemplate newInstance = new EntityTemplate()
        //        }
        return Response.ok(entityUrl.toURI()).build();
    }


    @GET
    @Path("/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
        /*
         * Return a list of strings of all available entities
         */
    public Response listEntities() throws CacheException
    {
        List<String> entityNames = new ArrayList<String>();
        for (EntityTemplateClass e : EntityTemplateClassCache.getInstance().values()) {
            entityNames.add(e.getName());
        }
        return Response.ok(entityNames).build();
    }

    
}

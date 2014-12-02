package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;
import org.hibernate.validator.constraints.NotBlank;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

/**
 * Created by bas on 07.10.14.
 */
@Path("/entities")
public class EntitiesEndpoint
{
    @GET
    @Path("/new")
    public Response newPage() throws CacheException
    {
        Template template = R.templateEngine().getEmptyTemplate("/views/new-page.html");
        Collection<EntityTemplateClass> entityTemplateClasses = EntityTemplateClassCache.getInstance().values();
        template.set(ParserConstants.ENTITY_CLASSES, entityTemplateClasses);
        return Response.ok(template).build();
    }

    @POST
    /**
     * Create a new page-instance of the page-class specified as a parameter
     */
    public Response createEntity(
                    @FormParam("page-class-name")
                    @NotBlank(message = "No entity-class specified.")
                    String entityClassName)
                    throws CacheException, RedisException, URISyntaxException

    {
        /*
         * Get the page-class (containing the default blocks and rows) from the cache and use it to construct a new page
         */
        EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(entityClassName);

        //this constructor will create a fresh entity, uniquely IDed by Redis
        EntityTemplate newEntityTemplate = new EntityTemplate(entityTemplateClass);
        Redis.getInstance().save(newEntityTemplate);
            /*
             * Redirect the client to the newly created page
             */
        return Response.seeOther(newEntityTemplate.getUrl().toURI()).build();
    }

//    @PUT
//    @Path("/{entityId:.*}")
//    @Consumes(MediaType.APPLICATION_JSON)
//    /*
//     * update a page-instance with id 'entityId' to be the html specified
//     */
//    public Response updateEntity(
//                    @PathParam("entityId")
//                    String entityId,
//                    @NotBlank(message = "No page found to update to.")
//                    String html) throws MalformedURLException, ParseException, RedisException, URISyntaxException
//    {
//        URL entityUrl = new URL(BlocksConfig.getSiteDomain() + "/" + entityId);
//
//        AbstractTemplate entityTemplate = new TemplateParser().parseEntityTemplate(entityUrl, html);
//        Redis redis = Redis.getInstance();
//        redis.save(entityTemplate);
//        return Response.seeOther(entityTemplate.getUrl().toURI()).build();
//    }
}

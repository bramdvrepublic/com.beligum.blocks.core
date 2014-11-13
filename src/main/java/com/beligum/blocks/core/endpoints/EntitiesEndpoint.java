package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.EntityClassCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.models.classes.EntityClass;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.blocks.core.parsing.EntityParser;
import com.beligum.blocks.core.validation.ValidationEntity;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.ws.rs.*;
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
        Collection<EntityClass> pageClasses = EntityClassCache.getInstance().getCache().values();
        template.set("pageClasses", pageClasses);
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
        Map<String, EntityClass> cache = EntityClassCache.getInstance().getCache();
        EntityClass pageClass = cache.get(entityClassName);


        Redis redis = Redis.getInstance();
        Entity newPage = redis.getNewPage(pageClass);
        redis.save(newPage);
            /*
             * Redirect the client to the newly created page
             */
        return Response.seeOther(newPage.getUrl().toURI()).build();
    }

    @PUT
    @Path("/{entityId:.*}")
    /*
     * update a page-instance with id 'pageId' to be the html specified
     */
    public Response updateEntity(
                    @PathParam("entityId")
                    String pageId,
                    @Valid
                    ValidationEntity validationEntity) throws MalformedURLException, ParserException, RedisException, URISyntaxException
    {
        String html = validationEntity.getHtml();
        String entityClassName = validationEntity.getEntityClassName();
        URL pageUrl = new URL(BlocksConfig.getSiteDomain() + "/" + pageId);

        EntityParser parser = new EntityParser(entityClassName);
        Entity page = parser.parsePage(html, pageUrl);
        Redis redis = Redis.getInstance();
        redis.save(page);
        return Response.seeOther(page.getUrl().toURI()).build();
    }
}

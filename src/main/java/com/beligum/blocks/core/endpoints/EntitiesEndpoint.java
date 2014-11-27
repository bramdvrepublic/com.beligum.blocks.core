package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.EntityClassCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.models.classes.EntityClass;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.blocks.core.parsers.AbstractParser;
import com.beligum.blocks.core.validation.ValidationEntity;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
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
        Collection<EntityClass> pageClasses = EntityClassCache.getInstance().getCache().values();
        template.set(ParserConstants.ENTITY_CLASSES, pageClasses);
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
        //TODO BAS SH: je bent net begonnen met doorlopen van het save-algoritme voor een entity (dus startend met het maken van een nieuwe lege entity)
        Map<String, EntityClass> cache = EntityClassCache.getInstance().getCache();
        EntityClass entityClass = cache.get(entityClassName);

        Redis redis = Redis.getInstance();
        //this constructor will create a fresh entity, uniquely IDed by Redis
        Entity newEntity = new Entity(entityClass);
        redis.save(newEntity);
            /*
             * Redirect the client to the newly created page
             */
        return Response.seeOther(newEntity.getUrl().toURI()).build();
    }

    @PUT
    @Path("/{entityId:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    /*
     * update a page-instance with id 'entityId' to be the html specified
     */
    public Response updateEntity(
                    @PathParam("entityId")
                    String entityId,
                    @Valid
                    ValidationEntity validationEntity) throws MalformedURLException, ParserException, RedisException, URISyntaxException
    {
        String html = validationEntity.getHtml();
        String entityClassName = validationEntity.getEntityClassName();
        URL entityUrl = new URL(BlocksConfig.getSiteDomain() + "/" + entityId);

        Entity entity = AbstractParser.parseEntity(entityUrl, html);
        Redis redis = Redis.getInstance();
        redis.save(entity);
        return Response.seeOther(entity.getUrl().toURI()).build();
    }
}

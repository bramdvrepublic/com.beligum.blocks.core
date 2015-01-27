package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by bas on 27.01.15.
 */
@Path("/debug")
public class DebugEndpoint
{
    @GET
    @Path("/flush")
    public Response flushEntities() throws CacheException
    {
        this.resetCache();
        Redis.getInstance().flushDB();
        return Response.ok("<ul><li>Cache reset</li><li>Database emptied</li></ul>").build();
    }

    @GET
    @Path("/reset")
    public Response resetCache() throws CacheException
    {
        EntityTemplateClassCache.getInstance().reset();
        PageTemplateCache.getInstance().reset();
        return Response.ok("Cache reset").build();
    }

    @GET
    @Path("/entityclasses")
    @Produces("text/plain")
    public Response getEntityTemplateClassCache() throws CacheException
    {
        List<String> entityTemplateClassKeys = EntityTemplateClassCache.getInstance().keys();
        List<EntityTemplateClass> entityTemplateClass = EntityTemplateClassCache.getInstance().values();
        String cache = "";
        for(int i = 0; i<entityTemplateClass.size(); i++){
            cache += "----------------------------------" + entityTemplateClassKeys.get(i) + "---------------------------------- \n\n" + entityTemplateClass.get(i).toString() + "\n\n\n\n\n\n";
        }
        return Response.ok(cache).build();
    }

    @GET
    @Path("/pagetemplates")
    @Produces("text/plain")
    public Response getPageTemplateCache() throws CacheException
    {
        List<String> pageTemplateKeys = PageTemplateCache.getInstance().keys();
        List<PageTemplate> pageTemplates = PageTemplateCache.getInstance().values();
        String cache = "";
        for(int i = 0; i<pageTemplates.size(); i++){
            cache += "----------------------------------" + pageTemplateKeys.get(i) + "---------------------------------- \n\n" + pageTemplates.get(i).toString() + "\n\n\n\n\n\n";
        }
        return Response.ok(cache).build();
    }

    @GET
    @Path("/src/{resourcePath:.+}")
    @Produces("text/plain")
    public Response fetchEntityTemplateSrc(
                    @PathParam("resourcePath")
                    @DefaultValue("")
                    String resourcePath,
                    @QueryParam("fragment")
                    @DefaultValue("")
                    String fragment)
                    throws MalformedURLException, IDException, RedisException, CacheException, ParseException
    {
        if(!StringUtils.isEmpty(fragment)){
            resourcePath += "#" + fragment;
        }
        String url = BlocksConfig.getSiteDomain() + "/" + resourcePath;
        EntityTemplate template = (EntityTemplate) Redis.getInstance().fetchLastVersion(new RedisID(new URL(url), RedisID.LAST_VERSION, false), EntityTemplate.class);
        return Response.ok(template.toString()).build();
    }

    @GET
    @Path("/show/{resourcePath:.+}")
    @Produces("text/html")
    public Response showEntityTemplate(
                    @PathParam("resourcePath")
                    @DefaultValue("")
                    String resourcePath,
                    @QueryParam("fragment")
                    @DefaultValue("")
                    String fragment)
                    throws MalformedURLException, IDException, RedisException, CacheException, ParseException
    {
        if(!StringUtils.isEmpty(fragment)){
            resourcePath += "#" + fragment;
        }
        String url = BlocksConfig.getSiteDomain() + "/" + resourcePath;
        EntityTemplate template = (EntityTemplate) Redis.getInstance().fetchLastVersion(new RedisID(new URL(url), RedisID.LAST_VERSION, false), EntityTemplate.class);
        return Response.ok(template.renderEntityInPageTemplate(template.getLanguage())).build();
    }
}

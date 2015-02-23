package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.URLMapping.URLMapper;
import com.beligum.blocks.core.URLMapping.XMLMapper;
import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.*;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.blocks.core.usermanagement.Permissions;
import com.beligum.core.framework.utils.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.joda.time.LocalDateTime;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by bas on 27.01.15.
 */
@Path("/debug")
@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
public class DebugEndpoint
{
    public static final String ENTTIY_INSTANCE_TYPE = "instance";
    public static final String ENTITY_CLASS_TYPE = "class";
    public static final String PAGE_TEMPLATE_TYPE = "template";

    @GET
    @Path("/flush")
    public Response flushEntities() throws CacheException
    {
        this.resetCache();
        Redis.getInstance().flushDB();
        Logger.warn("Database has been flushed by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");
        return Response.ok("<ul><li>Cache reset</li><li>Database emptied</li></ul>").build();
    }

    @GET
    @Path("/reset")
    public Response resetCache() throws CacheException
    {
        EntityTemplateClassCache.getInstance().reset();
        PageTemplateCache.getInstance().reset();
        EntityTemplateClassCache.getInstance();
        PageTemplateCache.getInstance();
        Logger.warn("Cache has been reset by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");
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
                    String fragment,
                    @QueryParam("type")
                    String typeName)
                    throws MalformedURLException, IDException, DatabaseException, CacheException, ParseException
    {
        URL url = renderUrl(resourcePath,fragment);
        Class<? extends AbstractTemplate> type = determineType(typeName);
        AbstractTemplate template = (AbstractTemplate) Redis.getInstance().fetchLastVersion(new BlocksID(url, BlocksID.LAST_VERSION, false), type);
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
                    String fragment,
                    @QueryParam("type")
                    String typeName)
                    throws MalformedURLException, IDException, DatabaseException, CacheException, ParseException
    {
        URL url = renderUrl(resourcePath, fragment);
        Class<? extends AbstractTemplate> type = determineType(typeName);
        AbstractTemplate template = (AbstractTemplate) Redis.getInstance().fetchLastVersion(new BlocksID(url, BlocksID.LAST_VERSION, false), type);
        if(template instanceof EntityTemplate) {
            return Response.ok(((EntityTemplate) template).renderEntityInPageTemplate(template.getLanguage())).build();
        }
        else{
            return Response.ok(TemplateParser.renderTemplate(template)).build();
        }
    }

    @GET
    @Path("/hash/{resourcePath:.+}")
    @Produces("text/html")
    public Response getTemplateHash(
                    @PathParam("resourcePath")
                    @DefaultValue("")
                    String resourcePath,
                    @QueryParam("fragment")
                    @DefaultValue("")
                    String fragment,
                    @QueryParam("type")
                    String typeName)
                    throws MalformedURLException, IDException, DatabaseException, CacheException, ParseException, SerializationException
    {
        URL url = renderUrl(resourcePath, fragment);
        Class<? extends AbstractTemplate> type = this.determineType(typeName);
        AbstractTemplate template = (AbstractTemplate) Redis.getInstance().fetchLastVersion(new BlocksID(url, BlocksID.LAST_VERSION, false), type);
        String retVal = "";
        Map<String, String> hash = template.toHash();
        List<String> keys = new ArrayList<>(hash.keySet());
        Collections.sort(keys);
        for(String key : keys){
            String fieldContent = hash.get(key);
            fieldContent = fieldContent.replace("<", "&lt;");
            fieldContent = fieldContent.replace(">", "&gt;");
            retVal += key + "  ---->  " + fieldContent + "<br/><br/>";
        }
        return Response.ok(retVal).build();
    }

    @GET
    @Path("/hash/allversions/{resourcePath:.+}")
    @Produces("text/html")
    public Response getTemplateHashForAllVersions(@PathParam("resourcePath")
                                                  @DefaultValue("")
                                                  String resourcePath,
                                                  @QueryParam("fragment")
                                                  @DefaultValue("")
                                                  String fragment,
                                                  @QueryParam("type")
                                                  String typeName) throws MalformedURLException, IDException, DatabaseException, SerializationException
    {
        Class<? extends AbstractTemplate> type = determineType(typeName);
        URL url = renderUrl(resourcePath, fragment);
        List<AbstractTemplate> versions = Redis.getInstance().fetchVersionList(new BlocksID(url, BlocksID.LAST_VERSION, false), type);
        String retVal = "";
        for(AbstractTemplate template : versions) {
            if(template != null) {
                retVal += "----------------------------------" + template.getId() + "---------------------------------- <br/><br/>";
                Map<String, String> hash = template.toHash();
                List<String> keys = new ArrayList<>(hash.keySet());
                Collections.sort(keys);
                for (String key : keys) {
                    String fieldContent = hash.get(key);
                    fieldContent = fieldContent.replace("<", "&lt;");
                    fieldContent = fieldContent.replace(">", "&gt;");
                    retVal += key + "  ---->  " + fieldContent + "<br/><br/>";
                }
                retVal += "<br/><br/><br/>";
            }
            else{
                retVal += "----------------------------------FOUND NULL TEMPLATE----------------------------------";
                retVal += "<br/><br/><br/><br/><br/>";
            }
        }
        return Response.ok(retVal).build();
    }

    private URL renderUrl(String resourcePath, String fragment) throws MalformedURLException
    {
        if(!StringUtils.isEmpty(fragment)){
            resourcePath += "#" + fragment;
        }
        return new URL(BlocksConfig.getSiteDomain() + "/" + resourcePath);
    }

    private Class<? extends AbstractTemplate> determineType(String typeName){
        Class<? extends AbstractTemplate> type;
        if(!StringUtils.isEmpty(typeName)) {
            switch (typeName) {
                case ENTTIY_INSTANCE_TYPE:
                    type = EntityTemplate.class;
                    break;
                case ENTITY_CLASS_TYPE:
                    type = EntityTemplateClass.class;
                    break;
                case PAGE_TEMPLATE_TYPE:
                    type = PageTemplate.class;
                    break;
                default:
                    type = EntityTemplate.class;
                    break;
            }
        }
        else{
            type = EntityTemplate.class;
        }
        return type;
    }

    @Path("start")
    @GET
    public Response debugMain() throws ParserConfigurationException, SAXException, IOException
    {
        return Response.ok().build();
    }

}

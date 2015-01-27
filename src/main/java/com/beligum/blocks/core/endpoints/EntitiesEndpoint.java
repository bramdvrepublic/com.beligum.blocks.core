package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import org.hibernate.validator.constraints.NotBlank;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

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
        PageTemplateCache.getInstance().reset();
        return Response.ok("Cache reset").build();
    }

    @GET
    @Path("/flush")
    //TODO BAS: this method should not be accessible in production-fase!
    public Response flushEntities() throws CacheException
    {
        this.resetCache();
        Redis.getInstance().flushDB();
        return Response.ok("<ul><li>Cache reset</li><li>Database emptied</li></ul>").build();
    }

    @POST
    /**
     * Create a new page-instance of the page-class specified as a parameter
     */
    public Response createEntity(
                    @FormParam("page-url")
                    @NotBlank(message = "No url specified.")
                    String pageUrl,
                    @FormParam("page-class-name")
                    @NotBlank(message = "No entity-class specified.")
                    String entityClassName)
                    throws Exception

    {
        EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(entityClassName);
        URL entityUrl = new URL(pageUrl);
        RedisID id = new RedisID(entityUrl, RedisID.LAST_VERSION, false);
        EntityTemplate lastVersion = Redis.getInstance().fetchEntityTemplate(id);
        URL newEntityUrl = null;
        /*
         * if no version was already present in db or if the url did not hold language-information,
         * render the language-information using the site's default values
         */
        if(lastVersion == null || !id.hasLanguage()) {
            id = new RedisID(entityUrl, RedisID.LAST_VERSION, true);
        }
        else if(lastVersion.getLanguages().contains(id.getLanguage())){
            throw new Exception("Cannot create an entity-language which already exists! This should not happen.");
        }
        else{
            //do nothing, since this just means we're adding a new language to an entity
        }
        newEntityUrl = TemplateParser.saveNewEntityTemplateToDb(entityUrl, id.getLanguage(), entityTemplateClass);

        /*
         * Redirect the client to the newly created entity's page
         */
        return Response.seeOther(newEntityUrl.toURI()).build();
    }

    @GET
    @Path("/class/{entityTemplateClassName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClassTemplate(@PathParam("entityTemplateClassName") String entityTemplateClasName) throws CacheException, ParseException
    {
        String classHtml = TemplateParser.renderTemplate(EntityTemplateClassCache.getInstance().get(entityTemplateClasName));
        HashMap<String, String> json = new HashMap<String, String>();
        json.put("template", classHtml);
        return Response.ok(json).build();
    }


    @POST
    @Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    /*
     * update a page-instance with id 'entityId' to be the html specified
     */
    public Response updateEntity(Map<String, String> data) throws MalformedURLException, ParseException, URISyntaxException, IDException, RedisException
    {
        String url = data.get("url");
        if(url.endsWith("#")){
            url = url.substring(0, url.length()-1);
        }
        URL entityUrl = new URL(url);
        TemplateParser.updateEntity(entityUrl, data.get("page"));
        //shouldn't do a redirect here, since the read of the entity could be done from a redis-slave, which would give the impression nothing was saved yet
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
        //TODO BAS: this should not be hard-coded
        Set<String> addableBlocks = new HashSet<>();
        addableBlocks.add("bordered-link");
        addableBlocks.add("building");
        addableBlocks.add("button");
        addableBlocks.add("exhibition");
        addableBlocks.add("experience");
        addableBlocks.add("image");
        addableBlocks.add("link-container");
        addableBlocks.add("pagetitle");
        addableBlocks.add("sectiontitle");
        addableBlocks.add("text-block");
        addableBlocks.add("text-with-border");
        addableBlocks.add("unkown");
        addableBlocks.add("whitespace");
        List<String> entityNames = new ArrayList<String>();
        for (EntityTemplateClass e : EntityTemplateClassCache.getInstance().values()) {
            if(!e.getName().equals(ParserConstants.DEFAULT_ENTITY_TEMPLATE_CLASS) && addableBlocks.contains(e.getName())){
                entityNames.add(e.getName());
            }
        }
        return Response.ok(entityNames).build();
    }

    @GET
    @Path("/template")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
        /*
         * Return a list of strings of all available page-templates
         */
    public Response listTemplates() throws CacheException
    {
        List<String> templateNames = new ArrayList<String>();
        for (PageTemplate e : PageTemplateCache.getInstance().values()) {
            if(!e.getName().equals(ParserConstants.DEFAULT_PAGE_TEMPLATE)){
                templateNames.add(e.getName());
            }
        }
        return Response.ok(templateNames).build();
    }

    @PUT
    @Path("/template")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeTemplate(@FormParam("template") String templateName, @FormParam("id") String id) throws CacheException, MalformedURLException, IDException, RedisException, ParseException {
        Redis redis = Redis.getInstance();
        URL url = new URL(id);
        RedisID lastVersionId = new RedisID(url, RedisID.LAST_VERSION, false);
        EntityTemplate entityTemplate = redis.fetchEntityTemplate(lastVersionId);
        //TODO BAS: must make BeanValidation checking that PageTemplateCache.getInstance().contains(templateName)
        entityTemplate.setPageTemplateName(templateName);
        String entity = entityTemplate.renderEntityInPageTemplate(entityTemplate.getLanguage());
        return Response.ok(entity).build();
    }
}

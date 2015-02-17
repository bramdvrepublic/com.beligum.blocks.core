package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Database;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.core.framework.i18n.I18n;
import com.beligum.core.framework.utils.Logger;
import gen.com.beligum.blocks.core.endpoints.ApplicationEndpointRoutes;
import org.hibernate.validator.constraints.NotBlank;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Created by bas on 07.10.14.
 */
@Path("/entities")
public class EntitiesEndpoint
{

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
        BlocksID id = new BlocksID(entityUrl, BlocksID.LAST_VERSION, false);
        EntityTemplate lastVersion = (EntityTemplate) Redis.getInstance().fetch(id, EntityTemplate.class);
        URL newEntityUrl = null;
        /*
         * if no version was already present in db or if the url did not hold language-information,
         * render the language-information using the site's default values
         */
        if(lastVersion == null || !id.hasLanguage()) {
            id = new BlocksID(entityUrl, BlocksID.LAST_VERSION, true);
        }
        else if(lastVersion.getLanguages().contains(id.getLanguage()) && lastVersion.getDeleted() == false){
            throw new Exception("Cannot create an entity-language which already exists! This should not happen.");
        }
        else{
            //do nothing, since this just means we're adding a new language to an entity or we are reviving an old version
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
    /*
     * update a page-instance with id 'entityId' to be the html specified
     */
    public Response updateEntity(Map<String, String> data)
    {
        try{
            String url = data.get("url");
            if(url.endsWith("#")){
                url = url.substring(0, url.length()-1);
            }
            URL entityUrl = new URL(url);
            //ignore the query-part of the url to fetch an entity from db, use only the path of the url
            entityUrl = new URL(entityUrl, entityUrl.getPath());
            EntityTemplate entity = (EntityTemplate) Redis.getInstance().fetchLastVersion(new BlocksID(entityUrl, BlocksID.LAST_VERSION, true), EntityTemplate.class);
            TemplateParser.updateEntity(entityUrl, data.get("page"));
            return Response.ok(entityUrl.getPath()).build();
        }catch (Exception e){
            return Response.status(Response.Status.BAD_REQUEST).entity(I18n.instance().getMessage("entitySaveFailed")).build();
        }
    }

    @POST
    @Path("/delete")
    public Response deleteEntity(String url)
    {
        try {
            Redis.getInstance().trash(new BlocksID(new URL(url), BlocksID.NO_VERSION, false));
            URL entityUrl = new URL(url);
            entityUrl = new URL(entityUrl, entityUrl.getPath());
            return Response.ok(entityUrl.toString()).build();
        }
        catch(Exception e){
            return Response.status(Response.Status.BAD_REQUEST).entity(I18n.instance().getMessage("entityDeleteFailed")).build();
        }
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
    public Response changeTemplate(@FormParam("template") String templateName, @FormParam("id") String id) throws CacheException, MalformedURLException, IDException, DatabaseException, ParseException {
        Database<AbstractTemplate> redis = Redis.getInstance();
        URL url = new URL(id);
        BlocksID lastVersionId = new BlocksID(url, BlocksID.LAST_VERSION, false);
        EntityTemplate entityTemplate = (EntityTemplate) redis.fetch(lastVersionId, EntityTemplate.class);
        //TODO BAS: must make BeanValidation checking that PageTemplateCache.getInstance().contains(templateName)
        entityTemplate.setPageTemplateName(templateName);
        String entity = entityTemplate.renderEntityInPageTemplate(entityTemplate.getLanguage());
        return Response.ok(entity).build();
    }

    @POST
    @Path("/deletedversion")
    public Response showDeletedVersion(@FormParam("page-url") String pageUrl) throws MalformedURLException, CacheException, ParseException, IDException, DatabaseException
    {
        List<AbstractTemplate> versionList = Redis.getInstance().fetchVersionList(new BlocksID(new URL(pageUrl), BlocksID.LAST_VERSION, true), EntityTemplate.class);
        EntityTemplate lastAccessibleVersion = null;
        Iterator<AbstractTemplate> it = versionList.iterator();
        while(lastAccessibleVersion == null && it.hasNext()){
            EntityTemplate version = (EntityTemplate) it.next();
            if(version != null && !version.getDeleted()){
                lastAccessibleVersion = version;
            }
        }
        if(lastAccessibleVersion != null) {
            String pageUrlPath = new URL(pageUrl).getPath().substring(1);
            return Response.seeOther(URI.create(ApplicationEndpointRoutes.getPageWithId(pageUrlPath, new Long(lastAccessibleVersion.getVersion())).getAbsoluteUrl())).build();
        }
        else{
            Logger.error("Bad request: cannot revive '" + pageUrl + "' to the state before it was deleted, since no version is present in db which is not deleted.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}

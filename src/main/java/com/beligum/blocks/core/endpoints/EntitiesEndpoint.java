package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.URLMapping.XMLUrlIdMapper;
import com.beligum.blocks.core.caching.BleuprintsCache;
import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Database;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.exceptions.*;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.Blueprint;
import com.beligum.blocks.core.models.redis.templates.PageTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.blocks.core.usermanagement.Permissions;
import com.beligum.core.framework.i18n.I18n;
import com.beligum.core.framework.utils.Logger;
import gen.com.beligum.blocks.core.endpoints.ApplicationEndpointRoutes;
import org.apache.shiro.authz.annotation.RequiresRoles;
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
@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
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
        Blueprint blueprint = BleuprintsCache.getInstance().get(entityClassName);
        URL pageURL = new URL(pageUrl);
        BlocksID existingId = XMLUrlIdMapper.getInstance().getId(pageURL);
        EntityTemplate lastVersion = (EntityTemplate) RedisDatabase.getInstance().fetchLastVersion(existingId, EntityTemplate.class);

        BlocksID newId = null;
        //if a not-deleted version exists in db, throw error
        if(lastVersion != null && !lastVersion.getDeleted()){
            throw new Exception("Cannot create already existing entity '" + lastVersion.getId().getUrl() + "' with url '" + pageURL + "'.");
//            //if the url to the template did not hold language-information,
//            if (!id.hasLanguage()) {
//                id = new BlocksID(entityUrl, BlocksID.LAST_VERSION, true);
//            }
//            else if (lastVersion.getLanguages().contains(id.getLanguage()) && lastVersion.getDeleted() == false) {
//                throw new Exception("Cannot create an entity-language which already exists! This should not happen.");
//            }
//            else {
//
//            }
        }
        //if the url isn't taken yet, render a new id
        else if(lastVersion == null){
            newId = BlocksID.renderNewEntityTemplateID(blueprint, existingId.getLanguage());
        }
        //if a deleted version is being revived, readd it to the mapping
        else{
            newId = existingId;
        }
        TemplateParser.saveNewEntityTemplateToDb(newId, blueprint);
        String unlanguagedPageUrl = Languages.translateUrl(pageURL.toString(), Languages.NO_LANGUAGE)[0];
        String unlanguagedIdUrl = Languages.translateUrl(newId.getUrl().toString(), Languages.NO_LANGUAGE)[0];
        if(!unlanguagedPageUrl.equals(unlanguagedIdUrl)){
            XMLUrlIdMapper.getInstance().put(newId, pageURL);
        }
        /*
         * Redirect the client to the newly created entity's page
         */
        return Response.seeOther(pageURL.toURI()).build();
    }

    @GET
    @Path("/class/{blueprintType}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClassTemplate(@PathParam("blueprintType") String entityTemplateClasName) throws CacheException, ParseException
    {
        String classHtml = TemplateParser.renderTemplate(BleuprintsCache.getInstance().get(entityTemplateClasName));
        HashMap<String, String> json = new HashMap<String, String>();
        json.put("template", classHtml);
        return Response.ok(json).build();
    }


    @PUT
    @Path("/{entityUrlPath:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    /*
     * update a page-instance with id 'entityId' to be the html specified
     */
    public Response updateEntity(@PathParam("entityUrlPath") String pageUrlPath, @QueryParam("deleted") @DefaultValue("false") boolean fetchDeleted, String pageHtml)
    {
        try{
            if(pageUrlPath.endsWith("#")){
                pageUrlPath = pageUrlPath.substring(0, pageUrlPath.length()-1);
            }
            URL pageUrl = new URL(BlocksConfig.getSiteDomainUrl(), pageUrlPath);
            //ignore the query-part of the url to fetch an entity from db, use only the path of the url
            pageUrl = new URL(pageUrl, pageUrl.getPath());
            BlocksID entityId = null;
            if(!fetchDeleted){
                entityId = XMLUrlIdMapper.getInstance().getId(pageUrl);
            }
            else{
                entityId = XMLUrlIdMapper.getInstance().getLastId(pageUrl);
            }
            EntityTemplate entity = (EntityTemplate) RedisDatabase.getInstance().fetchLastVersion(entityId, EntityTemplate.class);
            if(entity == null){
                throw new Exception("Cannot update entity which doesn't exist: '" + pageUrl + ".");
            }
            TemplateParser.updateEntity(entityId, pageHtml);
            if(fetchDeleted){
                XMLUrlIdMapper.getInstance().put(entityId, pageUrl);
            }
            return Response.ok(pageUrl.getPath()).build();
        }catch (Exception e){
            return Response.status(Response.Status.BAD_REQUEST).entity(I18n.instance().getMessage("entitySaveFailed")).build();
        }
    }

    @POST
    @Path("/delete")
    public Response deleteEntity(String url)
    {
        try {
            URL entityUrl = new URL(url);
            entityUrl = new URL(entityUrl, entityUrl.getPath());
            BlocksID id = XMLUrlIdMapper.getInstance().getId(entityUrl);
            AbstractTemplate lastVersion = (AbstractTemplate) RedisDatabase.getInstance().trash(id);
            Set<BlocksID> languagedIds = lastVersion.getTemplates().keySet();
            for(BlocksID languagedId : languagedIds) {
                XMLUrlIdMapper.getInstance().remove(languagedId);
            }
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
        List<String> entityNames = new ArrayList<String>();
        List<Blueprint> addableClasses = BleuprintsCache.getInstance().getAddableClasses();
        for (Blueprint e : addableClasses) {
            if(!e.getName().equals(ParserConstants.DEFAULT_BLUEPRINT)){
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
    public Response changeTemplate(@FormParam("template") String templateName, @FormParam("id") String id)
                    throws CacheException, MalformedURLException, IDException, DatabaseException, ParseException, UrlIdMappingException
    {
        Database<AbstractTemplate> redis = RedisDatabase.getInstance();
        URL url = new URL(id);
        BlocksID blocksId = XMLUrlIdMapper.getInstance().getId(url);
        EntityTemplate entityTemplate = (EntityTemplate) redis.fetchLastVersion(blocksId, EntityTemplate.class);
        //TODO BAS: must make BeanValidation checking that PageTemplateCache.getInstance().contains(templateName)
        entityTemplate.setPageTemplateName(templateName);
        String entity = entityTemplate.renderEntityInPageTemplate(entityTemplate.getLanguage());
        return Response.ok(entity).build();
    }

    @POST
    @Path("/deletedversion")
    public Response showDeletedVersion(@FormParam("page-url") String pageUrl) throws MalformedURLException, CacheException, ParseException, IDException, DatabaseException, UrlIdMappingException
    {
        BlocksID id = XMLUrlIdMapper.getInstance().getLastId(new URL(pageUrl));
        List<AbstractTemplate> versionList = RedisDatabase.getInstance().fetchVersionList(id, EntityTemplate.class);
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
            return Response.seeOther(URI.create(ApplicationEndpointRoutes.getPageWithId(pageUrlPath, new Long(lastAccessibleVersion.getVersion()), true).getAbsoluteUrl())).build();
        }
        else{
            Logger.error("Bad request: cannot revive '" + pageUrl + "' to the state before it was deleted, since no version is present in db which is not deleted.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}

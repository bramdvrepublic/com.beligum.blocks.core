package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplateClass;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.blocks.core.usermanagement.Permissions;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;
import gen.com.beligum.blocks.core.endpoints.ApplicationEndpointRoutes;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

@Path("/")
public class ApplicationEndpoint
{
    @GET
    public Response index() throws URISyntaxException
    {
        return Response.seeOther(new URI("/index")).build();
    }


    @Path("/mot/{name}")
    @GET
    public Response mot(@PathParam("name") String name)
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/templates/mot/"+name+".html");
        return Response.ok(indexTemplate).build();
    }


    //using regular expression to let all requests to undefined paths end up here
    @Path("/{randomPage:.+}")
    @GET
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath, @QueryParam("version") Long version)
    {
        //TODO BAS!3: make site-map with language-tree
        try{
            if(randomURLPath != null && (randomURLPath.equals("") || randomURLPath.equals("/"))){
                return Response.seeOther(URI.create(ApplicationEndpointRoutes.index().getPath())).build();
            }
            URL url = new URL(RequestContext.getRequest().getRequestURL().toString());
            if(version == null){
                version = BlocksID.LAST_VERSION;
            }
            else{
                if(!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)){
                    throw new UnauthorizedException("User is not allowed to see versioned entity: url = " + randomURLPath + ", version=" +version);
                }
            }
            //if no language info is specified in the url, or if the specified language doesn't exist, the default language will still be shown
            BlocksID id = new BlocksID(url, version, false);
            //if no such page is present in db, ask if user wants to create a new page
            if(id.getVersion() == BlocksID.NO_VERSION) {
                if(!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)){
                    throw new NotFoundException("Page does not exist: " + url);
                }
                Template template = R.templateEngine().getEmptyTemplate("/views/new-page.vm");
                return injectParameters(template);
            }
            //if a version is present in db, try to fetch the page from db
            else if(!id.hasLanguage()) {
                BlocksID primaryLanguageId = new BlocksID(id, BlocksID.PRIMARY_LANGUAGE);
                //if no primary language can be found in db, it means the page is not present in db
                if (!primaryLanguageId.hasLanguage()) {
                    throw new NotFoundException("Couldn't find " + primaryLanguageId.getUrl());
                }
                return Response.seeOther(primaryLanguageId.getLanguagedUrl().toURI()).build();

            }
            //if the url contains both version and language-information, try to render the entity
            else {
                version = id.getVersion();
                EntityTemplate entityTemplate = (EntityTemplate) Redis.getInstance().fetch(id, EntityTemplate.class);
                //if no entity-template is returned from db, the specified language doesn't exist
                if(entityTemplate == null){
                    //since a last version was found, it must be present in db
                    EntityTemplate storedInstance = (EntityTemplate) Redis.getInstance().fetchLastVersion(id, EntityTemplate.class);
                    if(storedInstance == null){
                        throw new Exception("Received null from db, after asking for last version of '" + id +"'. This should not happen!");
                    }
                    //if the page is deleted, it should not be shown (method throws NotFoundException)
                    boolean needsToBehandled = this.handleTrashedEntity(storedInstance);
                    //if the current user may modify entities, a choice is given to create a new page, or revive the deleted page
                    if(needsToBehandled && version.equals(storedInstance.getVersion())) {
                        Template template = R.templateEngine().getEmptyTemplate("/views/deleted-page.vm");
                        return injectParameters(template);
                    }
                    //if the page is reachable, render it
                    else {
                        //if the requested language already exists in db, render and show it
                        if (storedInstance.getLanguages().contains(id.getLanguage())) {
                            String page = storedInstance.renderEntityInPageTemplate(id.getLanguage());
                            return Response.ok(page).build();
                        }
                        //show the default language, but act as if it is the requested language
                        else {
                            String lastVersionHtml = TemplateParser.renderEntityInsidePageTemplate(storedInstance.getPageTemplate(), storedInstance, id.getLanguage());
                            return Response.ok(lastVersionHtml).build();
                        }
                    }
                }
                else {
                    //if the page is deleted, it should not be shown (method throws NotFoundException)
                    boolean needsToBehandled = this.handleTrashedEntity(entityTemplate);
                    if(needsToBehandled) {
                        Template template = R.templateEngine().getEmptyTemplate("/views/deleted-page.vm");
                        return injectParameters(template);
                    }
                    //if the page is reachable, render it
                    else {
                        String page = entityTemplate.renderEntityInPageTemplate(entityTemplate.getLanguage());
                        return Response.ok(page).build();
                    }
                }
            }
        }
        catch(AuthorizationException e){
            throw e;
        }
        catch(Exception e){
            throw new NotFoundException("The page '" + randomURLPath + "' could not be found.", e);
        }
    }

    /**
     * Checks if the specified entity is deleted and if so, it returns true if the current user is allowed to create new entities or throws a NotFoundException if the current user is not allowed.
     * @param entityTemplate
     * @return false if the entity did not need to be handled, since it was not a deleted entity, or true if it was and the current user is allowed to revive it
     * @throws javax.ws.rs.NotFoundException if the entity is deleted and the current user cannot revive it
     */
    private boolean handleTrashedEntity(EntityTemplate entityTemplate){
        if(entityTemplate.getDeleted() == true){
            if(!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
                throw new NotFoundException("A deleted entity cannot be accessed: '" + entityTemplate.getUrl() + "'.");
            }
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * Injects all parameters needed to create a new page into a template.
     * @param newPageTemplate
     * @throws InterruptedException
     * @throws CacheException
     */
    private Response injectParameters(Template newPageTemplate) throws InterruptedException, CacheException
    {
        //the first time the server is started, we need to wait for the cache to be proparly filled, so all classes will be shown the very first time a new page is made.
        EntityTemplateClassCache entityTemplateClassCache = EntityTemplateClassCache.getInstance();
        List<EntityTemplateClass> entityTemplateClasses = entityTemplateClassCache.values();
        List<EntityTemplateClass> pageClasses = new ArrayList<>();
        for (EntityTemplateClass entityTemplateClass : entityTemplateClasses) {
            if (entityTemplateClass.isPageBlock()) {
                pageClasses.add(entityTemplateClass);
            }
        }
        newPageTemplate.set(ParserConstants.ENTITY_URL, RequestContext.getRequest().getRequestURL().toString());
        newPageTemplate.set(ParserConstants.ENTITY_CLASSES, pageClasses);
        return Response.ok(newPageTemplate).build();
    }

}
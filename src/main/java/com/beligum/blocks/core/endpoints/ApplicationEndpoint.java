package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.URLMapping.XMLUrlIdMapper;
import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.RedisDatabase;
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

@Path("/")
public class ApplicationEndpoint
{
    @GET
    public Response index() throws URISyntaxException
    {
        return Response.seeOther(new URI("/index")).build();
    }

    @Path("/finder")
    @GET
    public Response finder()
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/finder.html");
        return Response.ok(indexTemplate).build();
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
        try{
            if(randomURLPath != null && (randomURLPath.equals("") || randomURLPath.equals("/"))){
                return Response.seeOther(URI.create(ApplicationEndpointRoutes.index().getPath())).build();
            }
            URL url = new URL(RequestContext.getRequest().getRequestURL().toString());
            if(version == null){
                version = BlocksID.NO_VERSION;
            }
            else{
                if(!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)){
                    throw new UnauthorizedException("User is not allowed to see versioned entity: url = " + randomURLPath + ", version=" +version);
                }
            }
            //if no language info is specified in the url, or if the specified language doesn't exist, the default language will still be shown
            BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
            EntityTemplate lastStoredVersion = (EntityTemplate) RedisDatabase.getInstance().fetchLastVersion(id, EntityTemplate.class);
            //TODO BAS SH: Er moet nog altijd een id teruggegeven worden voor getrashte urls. Nu worden ze verwijderd uit de url-id mapping en wordt dan bij het bezoeken van die verwijderde pagina niet langer de keuze gegeven om van een verwijderde versie te vertrekken. Vragen aan Wouter wat de beste oplossing is: de verwijderde url bijhouden in apparte XMLTemplate of in de huidige url-id-mapping, of niet langer de mogelijkheid geven om een verwijderde versie opnieuw op te halen
            //TODO BAS SH 2: alle TODO BAS! opmerkingen moeten nog gedaan worden voor dinsdag
            //if no such page is present in db, ask if user wants to create a new page
            if(lastStoredVersion == null) {
                if(!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)){
                    throw new NotFoundException("Page does not exist: " + url);
                }
                Template template = R.templateEngine().getEmptyTemplate("/views/new-page.vm");
                return injectParameters(template);
            }
            //render the entity
            else {
                id.setVersion(version);
                EntityTemplate entityTemplate = (EntityTemplate) RedisDatabase.getInstance().fetch(id, EntityTemplate.class);
                //if no entity-template is returned from db, the specified language or version don't exist, so we use the last stored version to render the page
                if(entityTemplate == null){
                    entityTemplate = lastStoredVersion;
                }
                //if the page is deleted, it should not be shown (method throws NotFoundException)
                boolean needsToBehandled = this.handleTrashedEntity(entityTemplate);
                //if the current user may modify entities, a choice is given to create a new page, or revive the deleted page
                if(needsToBehandled) {
                    Template template = R.templateEngine().getEmptyTemplate("/views/deleted-page.vm");
                    return injectParameters(template);
                }
                //if the page is reachable, render it
                else {
                    //if the requested language already exists in db, render and show it
                    if (entityTemplate.getLanguages().contains(id.getLanguage())) {
                        String page = entityTemplate.renderEntityInPageTemplate(id.getLanguage());
                        return Response.ok(page).build();
                    }
                    //show the default language, but act as if it is the requested language
                    else {
                        String lastVersionHtml = TemplateParser.renderEntityInsidePageTemplate(entityTemplate.getPageTemplate(), entityTemplate, id.getLanguage());
                        return Response.ok(lastVersionHtml).build();
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
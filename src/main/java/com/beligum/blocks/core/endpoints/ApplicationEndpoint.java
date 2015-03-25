package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.LanguageException;
import com.beligum.blocks.core.identifiers.MongoID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.nosql.Entity;
import com.beligum.blocks.core.models.nosql.StoredTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import com.beligum.blocks.core.usermanagement.Permissions;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;
import com.beligum.core.framework.utils.Logger;
import gen.com.beligum.blocks.core.endpoints.UsersEndpointRoutes;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Path("/")
public class ApplicationEndpoint
{

    @Path("/mot/{name}")
    @GET
    public Response mot(@PathParam("name") String name)
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/templates/mot/"+name+".html");
        return Response.ok(indexTemplate).build();
    }


    //using regular expression to let all requests to undefined paths end up here
    @Path("/{randomPage:.*}")
    @GET
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath, @QueryParam("version") Long version, @QueryParam("deleted") @DefaultValue("false") boolean fetchDeleted)
    {

        try {

            //            if(fetchDeleted && !SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)){
            //                Logger.debug("Unauthorized user tried to view deleted version of page '" + randomURLPath + "'.");
            //                fetchDeleted = false;
            //            }
            URL url = new URL(RequestContext.getRequest().getRequestUri().toString());

            // set language
            String language = BlocksConfig.getInstance().getUrlDispatcher().getLanguageOrNull(url);
            if (language == null) {
                List<Locale> languages = RequestContext.getRequest().getAcceptableLanguages();
                while (language == null && languages.iterator().hasNext()) {
                    Locale loc = languages.iterator().next();
                    if (BlocksConfig.getLanguages().contains(loc.getLanguage())) {
                        language = loc.getLanguage();
                    }
                }
            }
            //
            if (version != null && !SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
                //                    throw new UnauthorizedException("User is not allowed to see versioned entity: url = " + randomURLPath + ", version=" +version);
                version = null;
                Logger.debug("Unauthorized user tried to view older version of page '" + randomURLPath + "'.");
            }

            //            //if no language info is specified in the url, or if the specified language doesn't exist, the default language will still be shown
            String id = null;
            StoredTemplate storedTemplate = null;

            if (!fetchDeleted) {
                id = BlocksConfig.getInstance().getUrlDispatcher().findId(url);
                storedTemplate = BlocksConfig.getInstance().getDatabase().fetchTemplate(BlocksConfig.getInstance().getDatabase().getIdForString(id), language);
            }
            else {
                id = BlocksConfig.getInstance().getUrlDispatcher().findPreviousId(url);
            }


            if (storedTemplate == null) {
                if (!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
                    throw new NotFoundException("Page does not exist: " + url);
                }
//                //check if this url has ever before had an id mapped to it
//                if (!fetchDeleted) {
//                    id = BlocksConfig.getInstance().getUrlDispatcher().findPreviousId(url);
//                }
//                storedTemplate = BlocksConfig.getInstance().getDatabase().fetchTemplate(BlocksConfig.getInstance().getDatabase().getIdForString(id), language);

//                if (id == null) {
                    Template template = R.templateEngine().getEmptyTemplate("/views/new-page.vm");
                    return injectParameters(template);
                    //                    return Response.ok(TemplateCache.getInstance().getPagetemplate("menu-footer").getTemplateAsString(true)).build();
//                }
//                else {
//                    Template template = R.templateEngine().getEmptyTemplate("/views/deleted-page.vm");
//                    return injectParameters(template);
//                }

            } else {

                    if (storedTemplate.getEntity() != null) {
                        Entity entity = BlocksConfig.getInstance().getDatabase().fetchEntity(storedTemplate.getEntity().getId(), language);
                        storedTemplate.fillTemplateValuesWithEntityValues(entity, new HashSet<String>());
                    }
                return Response.ok(BlocksConfig.getInstance().getTemplateCache().getPagetemplate("menu-footer", language).getRenderedTemplate(false, storedTemplate)).build();
                }
                //



        }
        //if the index was not found, redirect to user login, else throw exception
        catch(NotFoundException e){
            String url = RequestContext.getRequest().getRequestUri().toString();
            try {
                if(url != null && (url.toString().equals(new URL(BlocksConfig.getSiteDomain() + "/" + BlocksConfig.getDefaultLanguage()).toString()) || url.toString().equals(new URL(BlocksConfig.getSiteDomain() + "/" + BlocksConfig.getDefaultLanguage() + "/").toString()))){
                    return Response.seeOther(URI.create(UsersEndpointRoutes.getLogin().getPath())).build();
                }
                else{
                    throw e;
                }
            }
            catch (MalformedURLException e1) {
                throw e;
            }
        }
        catch(AuthorizationException e){
            throw e;
        }
        catch(Exception e){
            Logger.error(e);
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
    private Response injectParameters(Template newPageTemplate) throws Exception
    {
        List<com.beligum.blocks.core.models.nosql.Blueprint> pageBlocks = BlocksConfig.getInstance().getTemplateCache().getPageBlocks();
        newPageTemplate.set(ParserConstants.ENTITY_URL, RequestContext.getRequest().getRequestUri().toString());
        newPageTemplate.set(ParserConstants.BLUEPRINTS, pageBlocks);
        return Response.ok(newPageTemplate).build();
    }

    private boolean hasLanguage(URL url) throws LanguageException
    {
        String[] urlAndLanguage = Languages.translateUrl(url.toString(), Languages.NO_LANGUAGE);
        return !"".equals(urlAndLanguage[1]);
    }

}
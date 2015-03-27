package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.LanguageException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.Blueprint;
import com.beligum.blocks.core.models.Entity;
import com.beligum.blocks.core.models.PageTemplate;
import com.beligum.blocks.core.models.StoredTemplate;
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
            String language = Blocks.urlDispatcher().getLanguageOrNull(url);
            if (language == null) {
                List<Locale> languages = RequestContext.getRequest().getAcceptableLanguages();
                while (language == null && languages.iterator().hasNext()) {
                    Locale loc = languages.iterator().next();
                    if (Blocks.config().getLanguages().contains(loc.getLanguage())) {
                        language = loc.getLanguage();
                    }
                }
            }
            //
            if (!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
                //                    throw new UnauthorizedException("User is not allowed to see versioned entity: url = " + randomURLPath + ", version=" +version);
                version = null;
                fetchDeleted = false;
                Logger.debug("Unauthorized user tried to view older version of page '" + randomURLPath + "'.");
            }

            //            //if no language info is specified in the url, or if the specified language doesn't exist, the default language will still be shown
            BlockId id = Blocks.urlDispatcher().findId(url);
            StoredTemplate storedTemplate = null;

            if (id != null) {
                storedTemplate = Blocks.database().fetchTemplate(id, language);

            } else if (fetchDeleted) {
                id = Blocks.urlDispatcher().findPreviousId(url);
                if (id != null) {
                    storedTemplate = Blocks.database().fetchPrevious(id, language, Blocks.factory().getStoredTemplateClass());
                }
            }


            if (storedTemplate == null) {
                if (fetchDeleted) {
                    // Todo add flash message
                }
                if (SecurityUtils.getSubject().isAuthenticated()) {
                    Template template = R.templateEngine().getEmptyTemplate("/views/new-page.vm");
                    return injectParameters(template);
                } else {
                    throw new NotFoundException();
                }

            } else {

                if (storedTemplate.getEntity() != null) {
                    Entity entity = Blocks.database().fetchEntity(storedTemplate.getEntity().getId(), language);
                    storedTemplate.fillTemplateValuesWithEntityValues(entity, new HashSet<String>());
                }
                PageTemplate pageTemplate = Blocks.templateCache().getPagetemplate(storedTemplate.getPageTemplateName(), storedTemplate.getLanguage());
                return Response.ok(pageTemplate.getRenderedTemplate(false, storedTemplate)).build();
            }
            //



        }
        //if the index was not found, redirect to user login, else throw exception
        catch(NotFoundException e){
                return Response.seeOther(URI.create(UsersEndpointRoutes.getLogin().getPath())).build();
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
     * Injects all parameters needed to create a new page into a template.
     * @param newPageTemplate
     * @throws InterruptedException
     * @throws CacheException
     */
    private Response injectParameters(Template newPageTemplate) throws Exception
    {
        List<Blueprint> pageBlocks = Blocks.templateCache().getPageBlocks();
        newPageTemplate.set(ParserConstants.ENTITY_URL, RequestContext.getRequest().getRequestUri().toString());
        newPageTemplate.set(ParserConstants.BLUEPRINTS, pageBlocks);
        return Response.ok(newPageTemplate).build();
    }


}
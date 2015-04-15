package com.beligum.blocks.endpoints;

import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.CacheException;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.Blueprint;
import com.beligum.blocks.models.Entity;
import com.beligum.blocks.models.PageTemplate;
import com.beligum.blocks.models.StoredTemplate;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import com.beligum.blocks.usermanagement.Permissions;
import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import gen.com.beligum.blocks.endpoints.UsersEndpointRoutes;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Locale;

@Path("/")
public class ApplicationEndpoint
{




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
            URL url = new URL(RequestContext.getJaxRsRequest().getUriInfo().getRequestUri().toString());

            // set language
            String language = Blocks.urlDispatcher().getLanguageOrNull(url);
            if (language == null) {
                List<Locale> languages = RequestContext.getJaxRsRequest().getAcceptableLanguages();
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

            //if no language info is specified in the url, or if the specified language doesn't exist, the default language will still be shown
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
                Entity entity = null;
                if (storedTemplate.getEntity() != null) {
                    entity = Blocks.database().fetchEntity(storedTemplate.getEntity().getId(), language);
                }
                PageTemplate pageTemplate = Blocks.templateCache().getPagetemplate(storedTemplate.getPageTemplateName());
                BlocksTemplateRenderer renderer = Blocks.factory().createTemplateRenderer();
                String page = renderer.render(pageTemplate, storedTemplate, entity, storedTemplate.getLanguage());
                return Response.ok(page).build();
            }


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
        newPageTemplate.set(ParserConstants.ENTITY_URL, RequestContext.getJaxRsRequest().getUriInfo().getRequestUri().toString());
        newPageTemplate.set(ParserConstants.BLUEPRINTS, pageBlocks);
        return Response.ok(newPageTemplate).build();
    }


}
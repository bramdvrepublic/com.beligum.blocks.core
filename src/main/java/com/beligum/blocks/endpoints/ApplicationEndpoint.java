package com.beligum.blocks.endpoints;

import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.CacheException;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.*;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import gen.com.beligum.blocks.core.fs.html.views.new_page;
import org.apache.shiro.SecurityUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Path("/")
public class ApplicationEndpoint
{
    //using regular expression to let all requests to undefined paths end up here
    @Path("/")
    @GET
    public Response getIndex() throws Exception
    {
        return this.getPageWithId(null, null, false);
    }
    //using regular expression to let all requests to undefined paths end up here
    @Path("/{randomPage:.*}")
    @GET
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath, @QueryParam("version") Long version, @QueryParam("deleted") @DefaultValue("false") boolean fetchDeleted)
                    throws Exception
    {
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
        if (!SecurityUtils.getSubject().isPermitted(com.beligum.blocks.security.Permissions.ENTITY_MODIFY)) {
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
        }
        else if (fetchDeleted) {
            id = Blocks.urlDispatcher().findPreviousId(url);
            if (id != null) {
                storedTemplate = Blocks.database().fetchPrevious(id, language, Blocks.factory().getStoredTemplateClass());
            }
        }

        if (storedTemplate == null) {
            if (fetchDeleted) {
                // Todo add flash message
            }
            //if we're logged in, render out the page where we can create a new page
            if (SecurityUtils.getSubject().isAuthenticated() || SecurityUtils.getSubject().isRemembered()) {
                return injectParameters(new_page.get().getNewTemplate());
            }
            else {
                throw new NotFoundException();
            }
        }
        else {
            Resource resource = null;
            //                if (storedTemplate.getEntity() != null) {
            ArrayList<JsonLDWrapper> model = Blocks.database().fetchEntities("{ '@graph.@id': 'mot:/" + storedTemplate.getId().toString() + "'}");

            if (model.iterator().hasNext())
                resource = model.iterator().next().getMainResource(storedTemplate.getLanguage());
            //                }

            PageTemplate pageTemplate = Blocks.templateCache().getPageTemplate(storedTemplate.getPageTemplateName());
            if (pageTemplate==null) {
                throw new Exception("Couldn't find the page template with name '"+storedTemplate.getPageTemplateName()+"'");
            }
            BlocksTemplateRenderer renderer = Blocks.factory().createTemplateRenderer();

            // Todo render entity
            return Response.ok(renderer.render(pageTemplate, storedTemplate, resource, storedTemplate.getLanguage())).build();
        }
    }

    /**
     * Injects all parameters needed to create a new page into a template.
     *
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
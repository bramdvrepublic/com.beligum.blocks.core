package com.beligum.blocks.endpoints;

import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.CacheException;
import com.beligum.blocks.models.*;
import com.beligum.blocks.models.jsonld.ResourceNode;
import com.beligum.blocks.models.jsonld.ResourceNodeInf;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import com.beligum.blocks.usermanagement.Permissions;
import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import gen.com.beligum.blocks.core.fs.html.views.new_page;
import org.apache.shiro.SecurityUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

@Path("/")
public class ApplicationEndpoint
{


    @Path(ParserConstants.RESOURCE_ENDPOINT + "{block_id:.*}")
    @GET
    public Response getPageWithId(@PathParam("block_id") String blockId, @QueryParam("view") String view_block_id, @QueryParam("language") String language)
                    throws MalformedURLException
    {
        String url = RequestContext.getJaxRsRequest().getUriInfo().getRequestUri().toString();

        if (language == null) language = Blocks.config().getRequestDefaultLanguage();
        ResourceNode view = Blocks.database().fetchResource(view_block_id, language);


        return Response.ok().build();
    }


    //using regular expression to let all requests to undefined paths end up here
    @Path("/{randomPage:.*}")
    @GET
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath, @QueryParam("version") Long version, @QueryParam("deleted") @DefaultValue("false") boolean fetchDeleted)
                    throws Exception
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
                language = Blocks.config().getRequestDefaultLanguage();
            }
            //
            if (!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
                //                    throw new UnauthorizedException("User is not allowed to see versioned entity: url = " + randomURLPath + ", version=" +version);
                version = null;
                fetchDeleted = false;
                Logger.debug("Unauthorized user tried to view older version of page '" + randomURLPath + "'.");
            }

            //if no language info is specified in the url, or if the specified language doesn't exist, the default language will still be shown
            SiteUrl siteUrl = Blocks.urlDispatcher().findId(url);
            StoredTemplate storedTemplate = null;
            ResourceNode resource = null;
            if (siteUrl != null) {
                ResourceNode view = Blocks.database().fetchResource(siteUrl.getViewUrl(), language);
                storedTemplate = new StoredTemplate();
                storedTemplate.wrap(view.unwrap());


                if (siteUrl.getResourceUrl() != null) {
                    ResourceNode resourceContext = Blocks.database().fetchResource(siteUrl.getResourceUrl(), language);
                    resource = resourceContext;
                }

            } else if (fetchDeleted) {
                //                id = Blocks.urlDispatcher().findPreviousId(url);
                if (siteUrl != null) {
                    //                    storedTemplate = Blocks.database().fetchPrevious(id, language, Blocks.factory().getStoredTemplateClass());
                }
            }


            if (storedTemplate == null) {
                if (fetchDeleted) {
                    // Todo add flash message
                }
                if (SecurityUtils.getSubject().isAuthenticated()) {
                    return injectParameters(new_page.instance.getNewTemplate());
                } else {
                    throw new NotFoundException();
                }

            } else {
                //                if (storedTemplate.getEntity() != null) {
                //                    ArrayList<JsonLDWrapper> model = Blocks.database().fetchEntities("{ '@graph.@id': 'mot:/" + storedTemplate.getId().toString() + "'}");

                //                if (model.iterator().hasNext()) resource = model.iterator().next().getMainResource(storedTemplate.getLanguage());
                //                }

                PageTemplate pageTemplate = Blocks.templateCache().getPagetemplate(storedTemplate.getPageTemplateName());
                BlocksTemplateRenderer renderer = Blocks.factory().createTemplateRenderer();

                // Todo render entity
                String page = renderer.render(pageTemplate, storedTemplate, resource, storedTemplate.getLanguage());
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
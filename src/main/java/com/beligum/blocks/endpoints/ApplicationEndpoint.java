package com.beligum.blocks.endpoints;

import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.exceptions.CacheException;
import com.beligum.blocks.models.*;
import com.beligum.blocks.models.jsonld.Resource;
import com.beligum.blocks.models.jsonld.ResourceImpl;
import com.beligum.blocks.models.url.BlocksURL;
import com.beligum.blocks.models.url.OkURL;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import com.beligum.blocks.repositories.ResourceRepository;
import com.beligum.blocks.repositories.UrlRepository;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.utils.UrlTools;
import gen.com.beligum.blocks.core.fs.html.views.new_page;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

@Path("/")
public class ApplicationEndpoint
{


    /*
    * Every resource on this domain has a url as id in for http://xxx.org/v1/resources/...
    *
    * These resources are mapped to clean urls in the routing table in the db.
    * Currently there ar 2 types of routes:
    * - OKURL: shows a resource (normally a view with optionally an other resource as argument, based on the current path
    * - MovedPermanentlyURL: redirects to an other url
    *
    * Language is not a part of the url-path in the database.
    *
    * */

    @Path(ParserConstants.RESOURCE_ENDPOINT + "{block_id:.*}")
    @GET
    public Response getPageWithId(@PathParam("block_id") String blockId, @QueryParam("resource") String resource_block_id, @QueryParam("language") String language)
    {
        URI pageUrl = RequestContext.getJaxRsRequest().getUriInfo().getRequestUri();
        language = language == null ? Blocks.config().getRequestDefaultLanguage() : language;
        URI resourceURI = null;
        if (resource_block_id != null) {
            resourceURI = UriBuilder.fromUri(resource_block_id).build();
        }

        OkURL okUrl = new OkURL(pageUrl, pageUrl, resourceURI, language);

        return okUrl.response(language);
    }


    /*
    * using regular expression to let all requests to undefined paths end up here
    * We try to find these urls in our routing table and redirect them to the correct url
    * */

    @Path("/{randomPage:.*}")
    @GET
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath, @QueryParam("version") Long version, @QueryParam("deleted") @DefaultValue("false") boolean fetchDeleted)
                    throws Exception
    {
        try {
            Response retVal = null;
            URI currentURI = RequestContext.getJaxRsRequest().getUriInfo().getRequestUri();

            /*
            * Check if a language is available in this url
            * If not, add it as first part of the url and redirect to that url
            * */
            String language = UrlTools.getLanguage(currentURI);
            if (language == null) {
                language = Blocks.config().getRequestDefaultLanguage();
                UriBuilder uriBuilder = UriBuilder.fromUri(currentURI);
                uriBuilder.replacePath("/" + language).path(currentURI.getPath());
                retVal = Response.temporaryRedirect(uriBuilder.build()).build();
            } else  {
                /*
                * Prevent everyone except admins from seeing previous versions of pages or viewing deleted pages
                * */
                if (!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
                    version = null;
                    fetchDeleted = false;
                    Logger.debug("Unauthorized user tried to view older version of page '" + randomURLPath + "'.");
                }



                /*
                * Select all urls with the same path as this url.
                * This code could be different per site so we have to find a way to inject it via the config file?
                * */

                java.nio.file.Path currentPath = Paths.get(currentURI.getPath());
                currentPath = UrlTools.getPathWithoutLanguage(currentPath);
                BlocksURL url = UrlRepository.instance().getUrlForURI(currentURI.getAuthority(), currentPath.toString(), language);
                if (url != null) {
                    // Routing found in DB so call this
                    retVal = url.response(language);
                } else {
                    // No routing found. If User with correct rights is logged in -> show create new page
                    if (SecurityUtils.getSubject().isAuthenticated() && SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
                        retVal = injectParameters(new_page.get().getNewTemplate());
                    } else if (R.configuration().getProduction()) {
                        // In production show page not found
                        throw new NotFoundException();
                    } else {
                        // In debug mode give option to log in.
//                        retVal = Response.seeOther(URI.create(UsersEndpointRoutes.getLogin().getPath())).build();
                        throw new NotFoundException();
                    }
                }

            }

            return retVal;
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
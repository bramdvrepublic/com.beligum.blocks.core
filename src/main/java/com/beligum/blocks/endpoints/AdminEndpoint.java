package com.beligum.blocks.endpoints;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.Parser;
import com.beligum.base.security.PermissionsConfigurator;
import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;
import com.beligum.base.validation.messages.DefaultFeedbackMessage;
import com.beligum.base.validation.messages.FeedbackMessage;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.controllers.PersistenceControllerImpl;
import com.beligum.blocks.models.sql.DBPath;
import com.beligum.blocks.routing.HtmlRouter;
import com.beligum.blocks.routing.Route;
import com.beligum.blocks.routing.ifaces.Router;
import com.beligum.blocks.templating.blocks.HtmlParser;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

@Path("/admin")
@RequiresRoles(PermissionsConfigurator.ADMIN_ROLE_NAME)
public class AdminEndpoint
{
    private static final String RESET_TEMPLATES = "templates";
    private static final String EVOLUTION_PAGESTORE = "pagestore";

    @Path("/")
    @GET
    public Response get(@Context HttpServletRequest httpRequest) throws IOException
    {
        if (!httpRequest.getRequestURI().endsWith("/")) {
            //works because of the @Path("/") above
            return Response.seeOther(gen.com.beligum.blocks.endpoints.AdminEndpointRoutes.get().getUri()).build();
        }
        else {
            return Response.ok(gen.com.beligum.blocks.core.fs.html.views.admin.get()).build();
        }
    }

    @Path("/reset/{type:.*}")
    @GET
    public Response reset(@PathParam("type") String type) throws IOException
    {
        switch (type) {
            case RESET_TEMPLATES:
                Parser htmlParser = R.resourceFactory().getParserFor(Resource.MimeType.HTML);
                if (htmlParser instanceof HtmlParser) {
                    //((HtmlParser) htmlParser).resetTemplateCache();

                    //we might as well load it immediately; easier for debugging (note that it's a static function)
                    HtmlParser.getTemplateCache();
                    R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.SUCCESS, "Template cache reset successfully"));
                }
                else {
                    throw new IOException("Found a HTML parser that's not a HtmlParse; this shouldn't happen" + htmlParser);
                }

                break;

            default:
                throw new NotFoundException("Supplied action '" + type + "' not found");
        }

        return Response.seeOther(gen.com.beligum.blocks.endpoints.AdminEndpointRoutes.get().getUri()).build();
    }
    @Path("/evolution/{type:.*}")
    @GET
    public Response evolution(@PathParam("type") String type) throws Exception
    {
        switch (type) {
            case EVOLUTION_PAGESTORE:

                int PAGE_SIZE = 100;
                int pageIdx = 0;
                boolean endReached = false;
                while (!endReached) {
                    List<DBPath> allPages = R.requestContext().getEntityManager().createQuery("select p FROM DBPath p ORDER BY p.updatedAt DESC", DBPath.class)
                                                          .setFirstResult(pageIdx)
                                                          .setMaxResults(PAGE_SIZE)
                                                          .getResultList();

                    for (DBPath path : allPages) {

                        //despite the name, the language isn't added here because the specialized constructor isn't called
                        String url = path.getLocalizedUrl().toString();
                        if (url.startsWith("/")) {
                            url = url.substring(1);
                        }
                        String localUrl = Paths.get("/").resolve(Paths.get(path.getLanguage().getLanguage())).resolve(url).normalize().toString();

                        URI uri = Settings.instance().getSiteDomain().resolve(localUrl);
                        Route route = new Route(uri, PersistenceControllerImpl.instance());

                        if (route.getLocale().equals(Locale.ROOT)) {
                            uri = UriBuilder.fromUri(Settings.instance().getSiteDomain()).path(Settings.instance().getDefaultLanguage().getLanguage()).path(localUrl).build();
                            route = new Route(uri, PersistenceControllerImpl.instance());
                            //TODO what about the other languages??
                        }

                        if (!route.exists()) {
                            Logger.error("Route doesn't exist??; "+uri);
                        }
                        else {
                            Router router = new HtmlRouter(route);
                            Response response = router.response();
                            String parsedHtml = response.getEntity().toString();
                            Logger.info(parsedHtml);

                            new PageEndpoint().savePage(uri, parsedHtml);

                            R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.SUCCESS, "All done."));

                            //DEBUG ONLY
                            break;
                        }



//                        Route route = new Route(webPage., PersistenceControllerImpl.instance());
//                        String templateStr = webPage.getPageTemplate();
//                        if (StringUtils.isEmpty(templateStr)) {
//                            List<HtmlTemplate> allPageTemplates = HtmlParser.getTemplateCache().getPageTemplates();
//                            //TODO we'll get the first, this should probably be configured somewhere
//                            templateStr = allPageTemplates.isEmpty() ? null : allPageTemplates.get(0).getTemplateName();
//                        }
//
//                        if (StringUtils.isEmpty(templateStr)) {
//                            throw new IOException("Unable to fetch or find a default page template, can't continue");
//                        }
//
//                        //Note: fallback var seems to be unused...
//                        String compactHtml = webPage.getParsedHtml(true);
//                        Template template = R.templateEngine().getNewStringTemplate(new StringBuilder().append("<" + templateStr + ">").append(compactHtml).append("</" + templateStr + ">").toString());
//
//                        String result = template.render();
//                        Logger.info(result);
//                        break;
                    }

                    pageIdx += allPages.size();
                    endReached = allPages.isEmpty() || allPages.size()<PAGE_SIZE;
                    //DEBUG ONLY!
                    endReached = true;
                }

                break;

            default:
                throw new NotFoundException("Supplied action '" + type + "' not found");
        }

        return Response.seeOther(gen.com.beligum.blocks.endpoints.AdminEndpointRoutes.get().getUri()).build();
    }
}
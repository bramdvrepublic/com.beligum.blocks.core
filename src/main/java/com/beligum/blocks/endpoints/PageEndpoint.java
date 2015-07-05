package com.beligum.blocks.endpoints;

///**
// * Created by bas on 07.10.14.
// */

import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.database.DummyBlocksController;
import com.beligum.blocks.database.OBlocksDatabase;
import com.beligum.blocks.pages.WebPageParser;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.resources.interfaces.Resource;
import com.beligum.blocks.routing.Route;
import com.beligum.blocks.search.ElasticSearchServer;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.utils.RdfTools;
import gen.com.beligum.blocks.core.fs.html.views.modals.newblock;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.hibernate.validator.constraints.NotBlank;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

@Path("/blocks/admin/page")
@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
public class PageEndpoint
{
    public static final String PAGE_TEMPLATE_NAME = "pageTemplateName";

    /**
     * Redirect back to the url where the page has to be created
     * We put the name of the pagetemplate in the flashcache
     */
    @GET
    @Path("/template")
    public Response getPageTemplate(
                    @QueryParam("page_url")
                    @NotBlank(message = "No url specified.")
                    String pageUrl,
                    @QueryParam("page_class_name")
                    @NotBlank(message = "No entity-class specified.")
                    String pageTemplateName)
                    throws Exception

    {
        PageTemplate pageTemplate = (PageTemplate) HtmlParser.getCachedTemplates().get(pageTemplateName);
        R.cacheManager().getFlashCache().put(PAGE_TEMPLATE_NAME, pageTemplateName);
        return Response.seeOther(new URI(pageUrl)).build();
    }

    @POST
    @Path("/save/{url:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response savePage(@PathParam("url") String url, String content)
                    throws Exception

    {

        URI uri = new URI(url);
        // Analyze the url to find the correct Route
        Route route = new Route(uri, DummyBlocksController.instance());
        boolean doVersion = false;

        if (!route.exists()) {
            route.create();
        }

        URI masterWebpage = route.getWebPath().getMasterPage();

        if (route.getWebPath().isNotFound()) {
            route.getWebPath().setPageOk(masterWebpage);
            DummyBlocksController.instance().savePath(route.getWebPath());

        } else if (route.getWebPath().isRedirect()) {
            //TODO get path to redirect
        }

        // fetch page for locale
        WebPage localizedWebpage = DummyBlocksController.instance().getWebPage(masterWebpage, route.getLocale());
        // if this page does not yet exist -> create
        if (localizedWebpage == null) {
            localizedWebpage = DummyBlocksController.instance().createWebPage(masterWebpage, RdfTools.createLocalResourceId(OBlocksDatabase.WEB_PAGE_CLASS), route.getLocale());
        } else {
            doVersion = true;
        }

        //        Parse html:
        //        1. get text
        //        2. get filtered html
        //        3. get resources - update new resources with resource-tag
        //        4. get href and src attributes
        WebPageParser oldPageParser = new WebPageParser(uri, localizedWebpage.getLanguage(), localizedWebpage.getParsedHtml(), DummyBlocksController.instance());
        WebPageParser pageParser = new WebPageParser(uri, localizedWebpage.getLanguage(), content, DummyBlocksController.instance());

        if (!(pageParser.getParsedHtml().equals(localizedWebpage.getParsedHtml()))) {
            localizedWebpage.setPageTemplate(pageParser.getPageTemplate());
            localizedWebpage.setParsedHtml(pageParser.getParsedHtml());
            localizedWebpage.setText(pageParser.getText());
            localizedWebpage.setLinks(pageParser.getLinks());
            localizedWebpage.setResources(pageParser.getResources().keySet());
            localizedWebpage.setTemplates(pageParser.getTemplates());

            // Put all found property values inside the resources'
            // return the resources that were changed
            WebPageParser.fillResourceProperties(pageParser.getResources(), pageParser.getResourceProperties(), oldPageParser.getResourceProperties(), DummyBlocksController.instance(), localizedWebpage.getLanguage());

            // TODO set webpage properties
            for (Resource resource: pageParser.getResources().values()) {
                DummyBlocksController.instance().saveResource(resource);
            }

            for (URI field: pageParser.getPageResource().getFields()) {
                localizedWebpage.set(field, pageParser.getPageResource().get(field));
            }

            // TODO update other pages that contain changed resources

            DummyBlocksController.instance().saveWebPage(localizedWebpage, doVersion);
        }


        return Response.ok().build();
    }

    @GET
    @Path("blocks")
    public Response getBlocks() {
        TemplateCache cache = HtmlParser.getCachedTemplates();
        List<Map<String, String>> templates = new ArrayList<>();
        Locale lang = BlocksConfig.instance().getRequestDefaultLanguage();
        for (HtmlTemplate template : cache.values()) {
            if (!(template instanceof PageTemplate)) {
                HashMap<String, String> pageTemplate = new HashMap();
                String title = null;
                String description = null;

                // current getLanguage of the request
                if (template.getTitles().containsKey(lang)) {
                    title = template.getTitles().get(lang);
                    description = template.getDescriptions().get(lang);
                }
                // default getLanguage of the site
                else if (template.getTitles().containsKey(BlocksConfig.instance().getDefaultLanguage())) {
                    title = template.getTitles().get(BlocksConfig.instance().getDefaultLanguage());
                    description = template.getDescriptions().get(BlocksConfig.instance().getDefaultLanguage());
                }
                // No getLanguage if available
                else if (template.getTitles().containsKey(Locale.ROOT)) {
                    title = template.getTitles().get(Locale.ROOT);
                    description = template.getDescriptions().get(Locale.ROOT);
                }
                // Random title and description
                else if (template.getTitles().values().size() > 0) {
                    title = (String) template.getTitles().values().toArray()[0];
                    description = (String) template.getDescriptions().values().toArray()[0];
                }
                // No title available
                else {
                    // TODO make this a translation
                    title = "A template";
                }
                if (description == null) {
                    description = "No description available";
                }
                pageTemplate.put("name", template.getTemplateName());
                pageTemplate.put("title", title);
                pageTemplate.put("description", description);
                templates.add(pageTemplate);
            }
        }
        Template template = newblock.get().getNewTemplate();
        template.set("templates", templates);
        return Response.ok(template.render()).build();
    }

    @GET
    @Path("block/{name:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBlock(@PathParam("name") String name) {
        HashMap<String, String> retVal = new HashMap<>();
        Template block = R.templateEngine().getNewStringTemplate("<"+ name + "></"+name+">");
        retVal.put("html", block.render());
        return Response.ok(retVal).build();
    }



    @DELETE
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePage(String url)
                    throws Exception

    {
        URI uri = new URI(url);
        Route route = new Route(uri, DummyBlocksController.instance());
        URI masterPage = route.getWebPath().getMasterPage();
        DummyBlocksController.instance().deleteWebPage(masterPage);
        return Response.ok().build();
    }

}

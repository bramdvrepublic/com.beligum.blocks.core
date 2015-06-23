package com.beligum.blocks.routing;

import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.endpoints.PageEndpoint;
import com.beligum.blocks.pages.ifaces.MasterWebPage;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.routing.ifaces.WebNode;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import gen.com.beligum.blocks.core.fs.html.views.new_page;
import org.apache.shiro.SecurityUtils;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Created by wouter on 1/06/15.
 *
 * Returns a response based on a RouteObject
 *
 */
public class HtmlRouter extends AbstractRouter
{
    private static final String NAME = "name";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";

    public HtmlRouter(Route route) {
        super(route);
    }


    /*
    * Show all pagetemplates so the user can choose.
    *
    * We try to find the title and description for all pagetemplates in the best language possible
    * */
    public Response newPage() {
        Response retVal = null;

        String newTemplate = null;
        if (R.cacheManager().getFlashCache().getTransferredEntries() != null) {
            newTemplate = (String) R.cacheManager().getFlashCache().getTransferredEntries().get(PageEndpoint.PAGE_TEMPLATE_NAME);
        }
        // Check if we just selected a template for this new page
        if (newTemplate == null) {
            if (!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
                throw new NotFoundException();
            }
            else {
                List<Map<String, String>> pageTemplates = new ArrayList<>();
                Template newPageTemplate = new_page.get().getNewTemplate();
                TemplateCache cache = HtmlParser.getCachedTemplates();
                for (HtmlTemplate template : cache.values()) {
                    if (template instanceof PageTemplate) {
                        HashMap<String, String> pageTemplate = new HashMap();
                        String title = null;
                        String description = null;
                        // current language of the request
                        if (template.getTitles().containsKey(this.route.getLocale())) {
                            title = template.getTitles().get(this.route.getLocale());
                            description = template.getDescriptions().get(this.route.getLocale());
                        }
                        // default language of the site
                        else if (template.getTitles().containsKey(BlocksConfig.instance().getDefaultLanguage())) {
                            title = template.getTitles().get(BlocksConfig.instance().getDefaultLanguage());
                            description = template.getDescriptions().get(BlocksConfig.instance().getDefaultLanguage());
                        }
                        // No language if available
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
                        pageTemplate.put(NAME, template.getTemplateName());
                        pageTemplate.put(TITLE, title);
                        pageTemplate.put(DESCRIPTION, description);
                        pageTemplates.add(pageTemplate);
                    }
                }
                newPageTemplate.set("url", this.route.getURI().toString());
                newPageTemplate.set("templates", pageTemplates);
                retVal = Response.ok(newPageTemplate).build();
            }
        } else {
            retVal = showCreatedPage(newTemplate);
        }

        return retVal;
    }


    /*
    * Shows a new pagetemplate for a url
    * This is the template the user can build a page from. Only when the user presses save,
    * the template is also save to the DB
    * */
    public Response showCreatedPage(final String pageTemplateName) {

        Iterable<HtmlTemplate> allTemplates = HtmlParser.getCachedTemplates().values();
        HtmlTemplate pageTemplate = Iterables.find(allTemplates, new Predicate<HtmlTemplate>()
        {
            public boolean apply(HtmlTemplate arg)
            {
                return arg.getTemplateName() != null && arg.getTemplateName().equals(pageTemplateName);
            }
        });

        return Response.ok(R.templateEngine().getNewStringTemplate(pageTemplate.getHtml().toString())).build();
    }

    /*
    * Gets a page from the database and renders it.
    * */
    public Response showPage() {
        StringBuilder rb = new StringBuilder();
        WebNode node = this.route.getNode();
        MasterWebPage master = this.route.getBlocksDatabase().getMasterWebPage(node.getPageUrl());
        WebPage page = master.getPageForLocale(this.route.getLocale());
        if (page == null) {
            // this language does not exist so show default language for this master page
            Locale locale = master.getDefaultLanguage();
            page = master.getPageForLocale(locale);

        }
        rb.append("<main-content>").append(page.getParsedHtml()).append("</main-content>");
        return Response.ok(R.templateEngine().getNewStringTemplate(rb.toString())).build();
    }

    public Response redirect() {
        return null;
    }
}

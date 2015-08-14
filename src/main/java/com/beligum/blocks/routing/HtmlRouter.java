package com.beligum.blocks.routing;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.caching.PageCache;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.controllers.interfaces.PersistenceController;
import com.beligum.blocks.endpoints.PageEndpoint;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.models.interfaces.WebPath;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.utils.comparators.MapComparator;
import gen.com.beligum.blocks.core.fs.html.views.new_page;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 1/06/15.
 * <p/>
 * Returns a response based on a RouteObject
 */
public class HtmlRouter extends AbstractRouter
{
    private static final String NAME = "name";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";

    private static final String ALLOW_EDIT_VARIABLE = "ALLOW_EDIT";

    private PersistenceController database;

    public HtmlRouter(Route route)
    {
        super(route);
    }

    /*
    * Show all pagetemplates so the user can choose.
    *
    * We try to find the title and description for all pagetemplates in the best getLanguage possible
    * */
    public Response newPage()
    {
        Response retVal = null;

        String newTemplate = null;

        //this means we redirected from the new-template-selection page
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
                TemplateCache cache = HtmlParser.getTemplateCache();
                for (HtmlTemplate template : cache.values()) {
                    if (template instanceof PageTemplate) {
                        HashMap<String, String> pageTemplate = new HashMap();
                        String title = null;
                        String description = null;
                        // current getLanguage of the request
                        if (template.getTitles().containsKey(this.route.getLocale())) {
                            title = template.getTitles().get(this.route.getLocale());
                            description = template.getDescriptions().get(this.route.getLocale());
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
                            title = I18nFactory.get("blocks.core.emptyTemplateTitle");
                        }
                        if (description == null) {
                            description = I18nFactory.get("blocks.core.emptyTemplateDescription");
                        }
                        pageTemplate.put(NAME, template.getTemplateName());
                        pageTemplate.put(TITLE, title);
                        pageTemplate.put(DESCRIPTION, description);
                        pageTemplates.add(pageTemplate);
                    }
                }

                Collections.sort(pageTemplates, new MapComparator("title"));

                newPageTemplate.set("url", this.route.getURI().toString());
                newPageTemplate.set("templates", pageTemplates);
                retVal = Response.ok(newPageTemplate).build();
            }
        }
        else {
            retVal = showCreatedPage(newTemplate);
        }

        return retVal;
    }

    /*
    * Shows a new pagetemplate for a url
    * This is the template the user can build a page from. Only when the user presses save,
    * the template is also save to the DB
    * */
    public Response showCreatedPage(final String pageTemplateName)
    {
        //by returning an empty tag (eg. <main-page></main-page>) the template engine will render a default page
        Template template = this.buildTemplateInstance(pageTemplateName, "");

        //this will allow the blocks javascript/css to be included
        template.set(ALLOW_EDIT_VARIABLE, true);

        return Response.ok(template).build();
    }

    /*
    * Gets a page from the database and renders it.
    * */
    public Response showPage() throws IOException
    {
        WebPath path = this.route.getWebPath();

        Object entity;
        String url = this.route.getURI().toString();
        if (PageCache.isEnabled() && PageCache.instance().hasUrl(url)) {
            entity = PageCache.instance().get(url);
        }
        else {
            URI master = path.getMasterPage();
            WebPage page = null;
            page = route.getBlocksDatabase().getWebPage(master, route.getLocale());
            String templateStr = page.getPageTemplate();
            if (StringUtils.isEmpty(templateStr)) {
                List<HtmlTemplate> allPageTemplates = HtmlParser.getTemplateCache().getPageTemplates();
                //TODO we'll get the first, this should probably be configured somewhere
                templateStr = allPageTemplates.isEmpty() ? null : allPageTemplates.get(0).getTemplateName();
            }

            if (StringUtils.isEmpty(templateStr)) {
                throw new IOException("Unable to fetch or find a default page template, can't continue");
            }

            Template template = this.buildTemplateInstance(templateStr, page.getParsedHtml());

            //this will allow the blocks javascript/css to be included
            template.set(ALLOW_EDIT_VARIABLE, true);

            entity = template;
        }

        return Response.ok(entity).build();

    }

    public Response redirect()
    {
        return null;
    }

    /**
     * A default tag instance with specified html inside
     * NOTE: both cases where this method is used don't use TagTemplates, so it's safe to just return the empty instance
     * If you ever want to use this method for TagTempaltes, don't, cause it won't include the attributes.
     * for that, use HtmlTemplate.createNewHtmlInstance() instead
     */
    private Template buildTemplateInstance(String templateName, String propertiesHtml)
    {
        return R.templateEngine().getNewStringTemplate(new StringBuilder().append("<" + templateName + ">").append(propertiesHtml).append("</" + templateName + ">").toString());
    }
}

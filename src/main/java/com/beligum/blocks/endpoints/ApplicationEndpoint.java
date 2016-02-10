package com.beligum.blocks.endpoints;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.resources.ResourceRequest;
import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.Resource;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.HdfsPathInfo;
import com.beligum.blocks.fs.HdfsResource;
import com.beligum.blocks.fs.pages.DefaultPageImpl;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.utils.comparators.MapComparator;
import gen.com.beligum.blocks.core.constants.blocks.core;
import gen.com.beligum.blocks.core.fs.html.views.new_page;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.FileContext;
import org.apache.shiro.SecurityUtils;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

/**
 * Created by bram on 2/10/16.
 */
@Path("/")
public class ApplicationEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Path("/favicon.ico")
    @GET
    public Response favicon()
    {
        throw new NotFoundException();
    }
    @Path("/{randomPage:.*}")
    @GET
    public Response getPageNew(@PathParam("randomPage") String randomURLPath) throws Exception
    {
        URI validatedUri = DefaultPageImpl.create(RequestContext.getJaxRsRequest().getUriInfo().getRequestUri());
        FileContext fs = Settings.instance().getPageStoreFileSystem();
        Page page = new DefaultPageImpl(new HdfsPathInfo(fs, validatedUri));
        Resource resource = R.resourceFactory().wrap(new HdfsResource(new ResourceRequest(validatedUri), fs, page.getNormalizedPageProxyPath()));

        Response.ResponseBuilder retVal = null;
        if (resource.exists()) {
            Template template = R.templateEngine().getNewTemplate(resource);

            //this will allow the blocks javascript/css to be included if we're logged in and have permission
            if (SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
                this.setBlocksMode(HtmlTemplate.ResourceScopeMode.edit, template.getContext());
            }

            retVal = Response.ok(template);
        }
        else {
            //if we have permission to create a new page, do it, otherwise, the page doesn't exist
            if (!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
                throw new NotFoundException();
            }
            else {
                Template newPageTemplate = new_page.get().getNewTemplate();
                newPageTemplate.set(core.Entries.NEW_PAGE_TEMPLATE_URL.getValue(), validatedUri.toString());
                newPageTemplate.set(core.Entries.NEW_PAGE_TEMPLATE_TEMPLATES.getValue(), this.buildLocalizedPageTemplateMap());

                retVal = Response.ok(newPageTemplate);
            }
        }

        return retVal.build();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void setBlocksMode(HtmlTemplate.ResourceScopeMode mode, TemplateContext context)
    {
        //this one is used by HtmlParser to test if we need to include certain tags
        R.cacheManager().getRequestCache().put(CacheKeys.BLOCKS_MODE, mode);

        //for velocity templates
        context.set(CacheKeys.BLOCKS_MODE.name(), mode.name());
    }
    private List<Map<String, String>> buildLocalizedPageTemplateMap()
    {
        TemplateCache cache = HtmlParser.getTemplateCache();
        List<Map<String, String>> pageTemplates = new ArrayList<>();
        Locale requestLocale = I18nFactory.instance().getOptimalLocale();
        for (HtmlTemplate template : cache.values()) {
            if (template instanceof PageTemplate) {
                HashMap<String, String> pageTemplate = new HashMap();
                String title = null;
                String description = null;
                // current getLanguage of the request
                if (template.getTitles().containsKey(requestLocale)) {
                    title = template.getTitles().get(requestLocale);
                    description = template.getDescriptions().get(requestLocale);
                }
                // default getLanguage of the site
                else if (template.getTitles().containsKey(Settings.instance().getDefaultLanguage())) {
                    title = template.getTitles().get(Settings.instance().getDefaultLanguage());
                    description = template.getDescriptions().get(Settings.instance().getDefaultLanguage());
                }
                // No getLanguage if available
                else if (template.getTitles().containsKey(Locale.ROOT)) {
                    title = template.getTitles().get(Locale.ROOT);
                    description = template.getDescriptions().get(Locale.ROOT);
                }
                // Random title and description
                else if (template.getTitles().values().size() > 0) {
                    title = (String) template.getTitles().values().toArray()[0];
                    if (template.getDescriptions().values().size() > 0) {
                        description = (String) template.getDescriptions().values().toArray()[0];
                    }
                }

                // No title available
                if (StringUtils.isEmpty(title)) {
                    title = gen.com.beligum.blocks.core.messages.blocks.core.Entries.emptyTemplateTitle.getI18nValue();
                }
                if (StringUtils.isEmpty(description)) {
                    description = gen.com.beligum.blocks.core.messages.blocks.core.Entries.emptyTemplateDescription.getI18nValue();
                }

                pageTemplate.put(core.Entries.NEW_PAGE_TEMPLATE_NAME.getValue(), template.getTemplateName());
                pageTemplate.put(core.Entries.NEW_PAGE_TEMPLATE_TITLE.getValue(), title);
                pageTemplate.put(core.Entries.NEW_PAGE_TEMPLATE_DESCRIPTION.getValue(), description);

                pageTemplates.add(pageTemplate);
            }
        }

        Collections.sort(pageTemplates, new MapComparator(core.Entries.NEW_PAGE_TEMPLATE_TITLE.getValue()));

        return pageTemplates;
    }
}

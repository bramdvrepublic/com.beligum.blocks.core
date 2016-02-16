package com.beligum.blocks.endpoints;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.resources.ResourceRequestImpl;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
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

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
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
    public Response getPage(@PathParam("randomPage") String randomURLPath) throws Exception
    {
        //security; rebuild the url instead of blindly accepting what comes in
        URI requestedUri = Settings.instance().getSiteDomain().resolve("/"+randomURLPath).normalize();

        FileContext fs = Settings.instance().getPageViewFileSystem();
        URI fsPageUri = DefaultPageImpl.toResourceUri(requestedUri, Settings.instance().getPagesViewPath());
        Page page = new DefaultPageImpl(new HdfsPathInfo(fs, fsPageUri));
        // Since we allow the user to create pretty url's, it's mime type will not always be clear.
        // But not this endpoint only accepts HTML requests, so force the mime type
        Resource resource = R.resourceFactory().lookup(new HdfsResource(new ResourceRequestImpl(requestedUri, Resource.MimeType.HTML), fs, page.getNormalizedPageProxyPath()));

        Response.ResponseBuilder retVal = null;
        if (resource.exists()) {
            Template template = R.templateEngine().getNewTemplate(resource);

            //this will allow the blocks javascript/css to be included if we're logged in and have permission
            if (SecurityUtils.getSubject().isPermitted(Permissions.Action.PAGE_MODIFY.getPermission())) {
                this.setBlocksMode(HtmlTemplate.ResourceScopeMode.edit, template);
            }

            retVal = Response.ok(template);
        }

        // General remark: we force the page to be created to have a language using redirects. It's not strictly necessary,
        // but it helps us a lot while linking translations together during page persist; by supplying a part in the URL-path
        // that identifies the language of the page, it's far easier to auto-generate translation URLs, and as a plus,
        // it introduces some structure to the site.
        //
        // We have four, ordered possibilities:
        // - OPTION 1: the page doesn't exist & the user has no rights -> 404
        // - OPTION 2: the page doesn't exist & the user has create rights & no language present -> redirect to URL with language prefix
        // - OPTION 3: the page doesn't exist & the user has create rights & language is present & page template in flash cache -> render a page template instance (not yet persisted)
        // - OPTION 4: the page doesn't exist & the user has create rights & language is present & nothing in flash cache -> show new page selection list
        else {
            //OPTION 1: if we have permission to create a new page, do it, otherwise, the page doesn't exist
            if (!SecurityUtils.getSubject().isPermitted(Permissions.Action.PAGE_MODIFY.getPermission())) {
                throw new NotFoundException();
            }
            else {
                // Note that, as a general language-selection mechanism, we only support URI locales,
                // but when there's no such locale found, we try to redirect to the one requested by the browser
                // and if all fails, we redirect to the default, configured locale
                Locale requestedUriLocale = R.i18nFactory().getUrlLocale(requestedUri);

                //OPTION 2: no URL-language available: redirect to a good guess
                if (requestedUriLocale==null) {
                    Locale redirectLocale = R.i18nFactory().getBrowserLocale();
                    //if the requested locale is supported by the site, use it, otherwise use the default locale
                    //if the default is forced, use it no matter what
                    if (Settings.instance().getForceRedirectToDefaultLocale() || !Settings.instance().getLanguages().containsKey(redirectLocale.getLanguage())) {
                        redirectLocale = Settings.instance().getDefaultLanguage();
                    }

                    if (redirectLocale==null) {
                        throw new IOException("Encountered null-valued default language; this shouldn't happen; "+requestedUri);
                    }
                    else {
                        retVal = Response.seeOther(UriBuilder.fromUri(requestedUri).replacePath("/"+redirectLocale.getLanguage()+requestedUri.getPath()).build());
                    }
                }
                else {
                    //this means we redirected from the new-template-selection page
                    String newPageTemplateName = null;
                    if (R.cacheManager().getFlashCache().getTransferredEntries() != null) {
                        newPageTemplateName = (String) R.cacheManager().getFlashCache().getTransferredEntries().get(CacheKeys.NEW_PAGE_TEMPLATE_NAME.name());
                    }

                    //OPTION 3: there's a template-selection in the flash cache (we came from the page-selection page)
                    // Note that we use the flash cache as a template-selection mechanism to keep the final URL clean
                    if (!StringUtils.isEmpty(newPageTemplateName)) {
                        //check if the name exists and is all right
                        HtmlTemplate pageTemplate = HtmlParser.getTemplateCache().getByTagName(newPageTemplateName);
                        if (pageTemplate != null && pageTemplate instanceof PageTemplate) {
                            Template newPageInstance = R.templateEngine().getNewTemplate(new ResourceRequestImpl(requestedUri, Resource.MimeType.HTML), pageTemplate.createNewHtmlInstance());

                            //this will allow the blocks javascript/css to be included
                            this.setBlocksMode(HtmlTemplate.ResourceScopeMode.edit, newPageInstance);

                            retVal = Response.ok(newPageInstance);
                        }
                        else {
                            throw new InternalServerErrorException("Requested to create a new page with an invalid page template name; " + newPageTemplateName);
                        }
                    }
                    //OPTION 4: empty flash cache; show the page templates list
                    else {
                        Template newPageTemplateList = new_page.get().getNewTemplate();
                        newPageTemplateList.set(core.Entries.NEW_PAGE_TEMPLATE_URL.getValue(), requestedUri.toString());
                        newPageTemplateList.set(core.Entries.NEW_PAGE_TEMPLATE_TEMPLATES.getValue(), this.buildLocalizedPageTemplateMap());

                        retVal = Response.ok(newPageTemplateList);
                    }
                }
            }
        }

        return retVal.build();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void setBlocksMode(HtmlTemplate.ResourceScopeMode mode, Template template)
    {
        //this one is used by HtmlParser to test if we need to include certain tags
        R.cacheManager().getRequestCache().put(CacheKeys.BLOCKS_MODE, mode);

        //for velocity templates
        template.getContext().set(CacheKeys.BLOCKS_MODE.name(), mode.name());
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

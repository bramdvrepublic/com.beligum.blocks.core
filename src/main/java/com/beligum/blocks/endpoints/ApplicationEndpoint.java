package com.beligum.blocks.endpoints;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.resources.ResourceRequestImpl;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.fs.HdfsResource;
import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.fs.index.entries.pages.IndexSearchResult;
import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.fs.pages.ReadOnlyPage;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ontology.factories.Terms;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.utils.comparators.MapComparator;
import gen.com.beligum.blocks.core.constants.blocks.core;
import gen.com.beligum.blocks.core.fs.html.views.new_page;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.shiro.SecurityUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
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
    public Response getPage(@PathParam("randomPage") String randomPage, @HeaderParam("Referer") String referer) throws Exception
    {
        Response.ResponseBuilder retVal = null;

        //make sure the path always starts with a slash (eg. not the case when this endpoint matched the root ("") path)
        if (!randomPage.startsWith("/")) {
            randomPage = "/" + randomPage;
        }

        //security; rebuild the url instead of blindly accepting what comes in
        //note: the randomURL doesn't include the query params; get them from the requestContext
        URI requestedUri = UriBuilder.fromUri(R.configuration().getSiteDomain()).replacePath(randomPage.toString())
                                     .replaceQuery(R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri().getQuery()).build();

        //We always need a language, so make sure we have one.
        // Note that, as a general language-selection mechanism, we only support URI locales,
        // but when there's no such locale found, we try to redirect to the one requested by the browser
        // and if all fails, we redirect to the default, configured locale
        Locale requestedUriLocale = R.i18nFactory().getUrlLocale(requestedUri);
        if (requestedUriLocale == null) {

            //if a request comes in without URL language, we launch some heuristics to fill it in:
            // 1) check if the page we come from is one of ours and if it is, use the same language for continuity
            Locale redirectLocale = null;
            if (!StringUtils.isEmpty(referer)) {
                URI refererUri = URI.create(referer);
                if (referer.startsWith(R.configuration().getSiteDomain().toString())) {
                    redirectLocale = R.i18nFactory().getUrlLocale(refererUri);
                }
            }

            // 2) detect the language of the client's browser (keeping some settings into account)
            // 3) revert to default language if the requested browser's language is unsupported or if all else fails
            if (redirectLocale==null) {
                redirectLocale = R.i18nFactory().getBrowserLocale();
                //if the requested locale is supported by the site, use it, otherwise use the default locale
                //if the default is forced, use it no matter what
                if (redirectLocale == null || Settings.instance().getForceRedirectToDefaultLocale() || !R.configuration().getLanguages().containsKey(redirectLocale.getLanguage())) {
                    redirectLocale = R.configuration().getDefaultLanguage();
                }
            }

            if (redirectLocale == null) {
                throw new IOException("Encountered null-valued default language; this shouldn't happen; " + requestedUri);
            }
            else {
                //we redirect to the &lang=.. (instead of lang-prefixed arl) when we're dealing with a resource to have a cleaner URL
                //Note that it gets stored locally with a lang-prefixed path though; see DefaultPageImpl.toResourceUri for details
                if (requestedUri.getPath().startsWith(Settings.RESOURCE_ENDPOINT)) {
                    retVal = Response.seeOther(UriBuilder.fromUri(requestedUri).queryParam(I18nFactory.LANG_QUERY_PARAM, redirectLocale.getLanguage()).build());
                }
                else {
                    retVal = Response.seeOther(UriBuilder.fromUri(requestedUri).replacePath("/" + redirectLocale.getLanguage() + requestedUri.getPath()).build());
                }
            }
        }

        if (retVal==null) {
            Page page = new ReadOnlyPage(requestedUri);
            // Since we allow the user to create pretty url's, it's mime type will not always be clear.
            // But not this endpoint only accepts HTML requests, so force the mime type
            Resource
                            resource =
                            R.resourceFactory()
                             .lookup(new HdfsResource(new ResourceRequestImpl(requestedUri, Resource.MimeType.HTML), page.getResourcePath().getFileContext(), page.getNormalizedPageProxyPath()));

            Locale optimalLocale = R.i18nFactory().getOptimalLocale();
            URI externalRedirectUri = null;

            // First, check if we're dealing with a resource.
            // If it's associated endpoint wants to redirect to another URL (eg. when we use resources of an external ontology)
            // don't try to lookup the resource locally, but redirect there.
            if (requestedUri.getPath().startsWith(Settings.RESOURCE_ENDPOINT)) {
                //check if it's a full resource path
                java.nio.file.Path path = Paths.get(requestedUri.getPath());
                if (path.getNameCount() == 3) {
                    //index wise: since part 0 is "resource" (validated above), we validate index 1 to be the type (and 2 to be the ID)
                    URI resourceTypeCurie = URI.create(Settings.instance().getRdfOntologyPrefix() + ":" + path.getName(1).toString());
                    RdfClass rdfClass = RdfFactory.getClassForResourceType(resourceTypeCurie);
                    if (rdfClass != null) {
                        RdfQueryEndpoint endpoint = rdfClass.getEndpoint();
                        if (endpoint != null) {
                            externalRedirectUri = endpoint.getExternalResourceRedirect(requestedUri, optimalLocale);
                        }
                    }
                }
            }

            //if we found an external redirect URL, redirect there, otherwise continue
            if (externalRedirectUri != null) {
                retVal = Response.seeOther(externalRedirectUri);
            }
            else {
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
                // We have a fixed set of ordered possibilities:
                // - OPTION 1: the page doesn't exist, but we can find another page with the URL as alias -> redirect to the correct URL of that page
                // - OPTION 2: the page doesn't exist, but we can find another page with the URL as alias -> redirect to the correct URL of that page
                // - OPTION 3: the page doesn't exist & the user has no rights -> 404
                // - OPTION 4: the page doesn't exist & the user has create rights & no language present -> can't happen because the language check happens at the start of this method (so before the login-check)
                // - OPTION 5: the page doesn't exist & the user has create rights & language is present & page template in flash cache -> render a page template instance (not yet persisted)
                // - OPTION 6: the page doesn't exist & the user has create rights & language is present & nothing in flash cache -> show new page selection list
                else {

                    //OPTION 1: before we give up and say the page doesn't exist, search for it's aliases first
                    LuceneQueryConnection queryConnection = StorageFactory.getMainPageQueryConnection();
                    BooleanQuery pageQuery = new BooleanQuery();
                    //don't really know if we should search for the formal path or just use the randomPage (or use StringFunctions.getRightOfDomain()),
                    //but let's start with this (eg. no query params like languages included) and see where we end up...
                    String searchUri = requestedUri.getPath().toString();
                    //we'll search for pages that have an alias (possibly/probably non-existent)
                    pageQuery.add(new TermQuery(new Term(Terms.sameAs.getCurieName().toString(), searchUri)), BooleanClause.Occur.SHOULD);
                    //and also for 'raw' resource url (eg. the backoffice uri that's used to link all translations together)
                    pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.resource.name(), searchUri)), BooleanClause.Occur.SHOULD);
                    //makes sense to make room for as much language-triples as we have clauses
                    IndexSearchResult results = queryConnection.search(pageQuery, pageQuery.clauses().size() * R.configuration().getLanguages().size());
                    PageIndexEntry selectedEntry = PageIndexEntry.selectBestForLanguage(results, optimalLocale);
                    if (selectedEntry != null) {
                        //we'll redirect to the id (eg. the public URI of the page) of the found resource
                        retVal = Response.seeOther(URI.create(selectedEntry.getId()));
                    }
                    //here, we decided the page really doesn't exist and we'll try to do something about that
                    else {
                        //OPTION 2: it's possible the user clicked a 'translated link' (eg. just the lang param was changed),
                        // but the translation page doesn't have the same structure as the original language (or is in fact totally different).
                        // Since we link pages together by using a more permanent resource url, we'll translate the url back for every language and see if it exists.
                        // If it does, we'll extract the resource uri and go looking for a page pointing to that resource and having the right language
                        pageQuery = new BooleanQuery();
                        //part a: first, we go hunting for the uri that _does_ exist
                        Collection<Locale> allLanguages = R.configuration().getLanguages().values();
                        for (Locale locale : allLanguages) {
                            if (!locale.equals(optimalLocale)) {
                                //replace the language of the uri by the language of the loopo
                                UriBuilder uriBuilder = UriBuilder.fromUri(searchUri);
                                R.i18nFactory().getUrlLocale(requestedUri, uriBuilder, locale);
                                //we'll search for a page that has the translated request uri as it's address
                                pageQuery.add(new TermQuery(new Term(IndexEntry.Field.id.name(), StringFunctions.getRightOfDomain(uriBuilder.build()).toString())), BooleanClause.Occur.SHOULD);
                            }
                        }
                        results = queryConnection.search(pageQuery, allLanguages.size());
                        //part b: if it exist, extract it's resource uri and search for a page pointing to it using the right language
                        if (!results.getResults().isEmpty()) {
                            //since all resources should be the same, we take the first match
                            String resourceUri = ((PageIndexEntry) results.getResults().iterator().next()).getResource();
                            pageQuery = new BooleanQuery();
                            pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.resource.name(), resourceUri)), BooleanClause.Occur.FILTER);
                            pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.language.name(), optimalLocale.getLanguage())), BooleanClause.Occur.FILTER);
                            results = queryConnection.search(pageQuery, -1);

                            selectedEntry = PageIndexEntry.selectBestForLanguage(results, optimalLocale);
                            if (selectedEntry != null) {
                                //we'll redirect to the id (eg. the public URI of the page) of the found resource
                                retVal = Response.seeOther(URI.create(selectedEntry.getId()));
                            }
                        }

                        //OPTION 3: if we have permission to create a new page, do it, otherwise, the page doesn't exist
                        if (retVal == null) {
                            if (!SecurityUtils.getSubject().isPermitted(Permissions.Action.PAGE_MODIFY.getPermission())) {
                                throw new NotFoundException("Can't find this page and you have no rights to create it; " + randomPage);
                            }
                            else {
                                //when we're dealing with a resource, we validate the resource type (the path part coming after the "/resource" path)
                                // to make sure it can be mapped in our ontology (during page creation)
                                if (requestedUri.getPath().startsWith(Settings.RESOURCE_ENDPOINT)) {
                                    java.nio.file.Path path = Paths.get(requestedUri.getPath());
                                    if (path.getNameCount() != 3) {
                                        throw new IOException("Encountered an (unexisting) resource URL with the wrong format. Need at least 3 segments: /resource/<type>/<id>;" + requestedUri);
                                    }
                                    else {
                                        //index wise: since part 0 is "resource" (validated above), we validate index 1 to be the type
                                        URI resourceTypeCurie = URI.create(Settings.instance().getRdfOntologyPrefix() + ":" + path.getName(1).toString());
                                        RdfClass resourcedType = RdfFactory.getClassForResourceType(resourceTypeCurie);
                                        if (resourcedType == null) {
                                            throw new IOException("Encountered an (unexisting) resource URL with an unknown RDF ontology type (" + resourceTypeCurie + ");" + requestedUri);
                                        }
                                    }
                                }

                                //this means we redirected from the new-template-selection page
                                String newPageTemplateName = null;
                                if (R.cacheManager().getFlashCache().getTransferredEntries() != null) {
                                    newPageTemplateName = (String) R.cacheManager().getFlashCache().getTransferredEntries().get(CacheKeys.NEW_PAGE_TEMPLATE_NAME.name());
                                }

                                //OPTION 5: there's a template-selection in the flash cache (we came from the page-selection page)
                                // Note that we use the flash cache as a template-selection mechanism to keep the final URL clean
                                if (!StringUtils.isEmpty(newPageTemplateName)) {
                                    //check if the name exists and is all right
                                    HtmlTemplate pageTemplate = HtmlParser.getTemplateCache().getByTagName(newPageTemplateName);
                                    if (pageTemplate != null && pageTemplate instanceof PageTemplate) {
                                        Template
                                                        newPageInstance =
                                                        R.templateEngine().getNewTemplate(new ResourceRequestImpl(requestedUri, Resource.MimeType.HTML), pageTemplate.createNewHtmlInstance());

                                        //this will allow the blocks javascript/css to be included
                                        this.setBlocksMode(HtmlTemplate.ResourceScopeMode.edit, newPageInstance);

                                        retVal = Response.ok(newPageInstance);
                                    }
                                    else {
                                        throw new InternalServerErrorException("Requested to create a new page with an invalid page template name; " + newPageTemplateName);
                                    }
                                }
                                //OPTION 6: empty flash cache; show the page templates list
                                else {
                                    Template newPageTemplateList = new_page.get().getNewTemplate();
                                    newPageTemplateList.set(core.Entries.NEW_PAGE_TEMPLATE_URL.getValue(), requestedUri.toString());
                                    newPageTemplateList.set(core.Entries.NEW_PAGE_TEMPLATE_TEMPLATES.getValue(), this.buildLocalizedPageTemplateMap());

                                    retVal = Response.ok(newPageTemplateList);
                                }
                            }
                        }
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
                else if (template.getTitles().containsKey(R.configuration().getDefaultLanguage())) {
                    title = template.getTitles().get(R.configuration().getDefaultLanguage());
                    description = template.getDescriptions().get(R.configuration().getDefaultLanguage());
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

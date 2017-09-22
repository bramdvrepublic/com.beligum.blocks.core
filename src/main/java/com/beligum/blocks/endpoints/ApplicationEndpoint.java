/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.endpoints;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.Source;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.filesystem.index.entries.IndexEntry;
import com.beligum.blocks.filesystem.index.entries.pages.IndexSearchResult;
import com.beligum.blocks.filesystem.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ontology.factories.Terms;
import com.beligum.blocks.rdf.sources.PageSource;
import com.beligum.blocks.rdf.sources.PageSourceCopy;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.utils.comparators.MapComparator;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import gen.com.beligum.blocks.core.constants.blocks.core;
import gen.com.beligum.blocks.core.fs.html.templates.blocks.core.new_page;
import org.apache.commons.io.IOUtils;
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
import java.io.InputStream;
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
    /**
     * Note: when the favicon is requested (a lot), we don't need to boot up the entire page-lookup below,
     * but can skip it (because it needs to be below the /assets path anyway)
     */
    @Path("/favicon.ico")
    @GET
    public Response getFavicon() throws Exception
    {
        throw new NotFoundException();
    }
    @Path("/{path:.*}")
    @GET
    public Response getPage(@PathParam("path") String path, @HeaderParam(HttpHeaders.REFERER) String referer) throws Exception
    {
        Response.ResponseBuilder retValBuilder = null;

        //make sure the path always starts with a slash (eg. not the case when this endpoint matched the root ("") path)
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        //security; rebuild the url instead of blindly accepting what comes in
        URI requestedUri = UriBuilder.fromUri(R.configuration().getSiteDomain())
                                     .replacePath(path)
                                     //note: the randomURL doesn't include the query params; get them from the requestContext
                                     .replaceQuery(R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri().getQuery())
                                     .build();

        //remove all unsupported query params from the page URI, so it doesn't matter if we
        // eg. save a page while on page 6 in a search result, or in the root; they should resolve to
        //     and save the same page, from use user-admin's point of view.
        //Note: the reason this works while creating a page (eg. the page_url and page_class_name query params),
        //      is because that callback is caught by the /admin/page/template endpoint and those parameters
        //      are in the flash cache once this request comes in.
        requestedUri = PageSource.cleanQueryParams(requestedUri);

        Locale optimalLocale = R.i18n().getOptimalLocale();

        // First, check if we're dealing with a resource.
        // If it's associated endpoint wants to redirect to another URL (eg. when we use resources of an external ontology)
        // don't try to lookup the resource locally, but redirect there.
        URI externalRedirectUri = null;
        if (requestedUri.getPath().startsWith(Settings.RESOURCE_ENDPOINT)) {
            //check if it's a full resource path
            java.nio.file.Path requestedPath = Paths.get(requestedUri.getPath());
            if (requestedPath.getNameCount() == 3) {
                //index wise: since part 0 is "resource" (validated above), we validate index 1 to be the type (and 2 to be the ID)
                URI resourceTypeCurie = URI.create(Settings.instance().getRdfOntologyPrefix() + ":" + requestedPath.getName(1).toString());
                RdfClass rdfClass = RdfFactory.getClassForResourceType(resourceTypeCurie);
                if (rdfClass != null) {
                    RdfQueryEndpoint endpoint = rdfClass.getEndpoint();
                    if (endpoint != null) {
                        externalRedirectUri = endpoint.getExternalResourceId(requestedUri, optimalLocale);
                    }
                }
            }
        }

        //if we found an external redirect URL, redirect there, otherwise continue
        if (externalRedirectUri != null) {
            retValBuilder = Response.seeOther(externalRedirectUri);
        }
        else {
            // Since we allow the user to instance pretty url's, it's mime type will not always be clear, so pass it on explicitly
            //Note: this will return null if the resource wasn't found
            Page page = R.resourceManager().get(requestedUri, Page.class);

            if (page != null) {

                Template template = R.resourceManager().newTemplate(page);

                //this will allow the blocks javascript/css to be included if we're logged in and have permission
                if (SecurityUtils.getSubject().isPermitted(Permissions.Action.PAGE_MODIFY.getPermission())) {
                    this.setBlocksMode(HtmlTemplate.ResourceScopeMode.edit, template);
                }

                retValBuilder = Response.ok(template);
            }

            // General remark: we force the page to be created to have a language using redirects. It's not strictly necessary,
            // but it helps us a lot while linking translations together during page persist; by supplying a part in the URL-path
            // that identifies the language of the page, it's far easier to auto-generate translation URLs, and as a plus,
            // it introduces some structure to the site.
            //
            // We have a fixed set of ordered possibilities:
            // - OPTION 1: the page doesn't exist, but we can find another page with the URL as alias -> redirect to the correct URL of that page
            // - OPTION 2: the page doesn't exist, but we can find a similar page in another language -> extract the resource-uri from that page and reverse-lookup the public URI in the requested language
            // - OPTION 3: the page doesn't exist & the user has no rights -> 404
            // - OPTION 4: the page doesn't exist & the user has instance rights & no language present -> try to detectAndReplace a decent language and redirect to a language-prefixed url (recursive call with roundtrip)
            // - OPTION 5: the page doesn't exist & the user has instance rights & language is present & page template in flash cache -> render a page template instance (not yet persisted)
            // - OPTION 6: the page doesn't exist & the user has instance rights & language is present & nothing in flash cache & page slug is unsafe -> redirect to safe variant
            // - OPTION 7: the page doesn't exist & the user has instance rights & language is present & nothing in flash cache & page slug is safe -> show new page selection list
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

                //this detects if the above search matched on the resource uri (and not the sameAs) when we're editing pages
                //it needs to jump out of the redirect in this case because when creating a new resource page in eg. english,
                //and we want to instance it's translation, it would redirect back to the english page instead of allowing us to
                //instance the translated page
                if (selectedEntry != null && SecurityUtils.getSubject().isPermitted(Permissions.Action.PAGE_MODIFY.getPermission())) {
                    if (selectedEntry.getResource().equals(searchUri)) {
                        selectedEntry = null;
                    }
                }

                if (selectedEntry != null) {
                    //we'll redirect to the id (eg. the public URI of the page) of the found resource
                    retValBuilder = Response.seeOther(URI.create(selectedEntry.getId()));
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
                            //replace the language of the uri by the language of the loop
                            UriBuilder uriBuilder = UriBuilder.fromUri(searchUri);
                            R.i18n().getUrlLocale(requestedUri, uriBuilder, locale);
                            //we'll search for a page that has the translated request uri as it's address
                            pageQuery.add(new TermQuery(new Term(IndexEntry.Field.id.name(), StringFunctions.getRightOfDomain(uriBuilder.build()).toString())), BooleanClause.Occur.SHOULD);
                        }
                    }
                    results = queryConnection.search(pageQuery, allLanguages.size());
                    //part b: if it exist, extract it's resource uri and search for a page pointing to it using the right language
                    if (!results.isEmpty()) {
                        //since all resources should be the same, we take the first match
                        String resourceUri = ((PageIndexEntry) results.iterator().next()).getResource();
                        pageQuery = new BooleanQuery();
                        pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.resource.name(), resourceUri)), BooleanClause.Occur.FILTER);
                        pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.language.name(), optimalLocale.getLanguage())), BooleanClause.Occur.FILTER);
                        results = queryConnection.search(pageQuery, -1);

                        selectedEntry = PageIndexEntry.selectBestForLanguage(results, optimalLocale);
                        if (selectedEntry != null) {
                            //we'll redirect to the id (eg. the public URI of the page) of the found resource
                            retValBuilder = Response.seeOther(URI.create(selectedEntry.getId()));
                        }
                    }

                    //OPTION 3: if we have permission to instance a new page, do it, otherwise, the page doesn't exist
                    if (retValBuilder == null) {

                        //if we reach this stage, we're 'deep enough' in the processing algo to assume there's a language present in the requested URI
                        // so it's a good time to demand for a language or redirect away otherwise (in which case this method will be called again)
                        //Note that we can't put this check a lot earlier because of eg. alias checking; eg. old URLs who don't have any formatting restraints
                        //We always need a language, so make sure we have one.
                        // Note that, as a general language-selection mechanism, we only support URI locales,
                        // but when there's no such locale found, we try to redirect to the one requested by the browser
                        // and if all fails, we redirect to the default, configured locale
                        Locale requestedUriLocale = R.i18n().getUrlLocale(requestedUri);

                        //OPTION 4: no language in the URL, redirect to a new address with a detected language
                        if (requestedUriLocale == null) {

                            //if a request comes in without URL language, we launch some heuristics to fill it in:
                            // 1) check if the page we come from is one of ours and if it is, use the same language for continuity
                            Locale redirectLocale = null;
                            if (!StringUtils.isEmpty(referer)) {
                                URI refererUri = URI.create(referer);
                                if (referer.startsWith(R.configuration().getSiteDomain().toString())) {
                                    redirectLocale = R.i18n().getUrlLocale(refererUri);
                                }
                            }

                            // 2) detectAndReplace the language of the client's browser (keeping some settings into account)
                            // 3) revert to default language if the requested browser's language is unsupported or if all else fails
                            if (redirectLocale == null) {
                                redirectLocale = R.i18n().getBrowserLocale();
                                //if the requested locale is supported by the site, use it, otherwise use the default locale
                                //if the default is forced, use it no matter what
                                if (redirectLocale == null || Settings.instance().getForceRedirectToDefaultLocale() ||
                                    !R.configuration().getLanguages().containsKey(redirectLocale.getLanguage())) {
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
                                    retValBuilder = Response.seeOther(UriBuilder.fromUri(requestedUri).queryParam(I18nFactory.LANG_QUERY_PARAM, redirectLocale.getLanguage()).build());
                                }
                                else {
                                    retValBuilder = Response.seeOther(UriBuilder.fromUri(requestedUri).replacePath("/" + redirectLocale.getLanguage() + requestedUri.getPath()).build());
                                }
                            }
                        }

                        //this means we haven't redirected away to an address with a language
                        if (retValBuilder == null) {
                            if (!SecurityUtils.getSubject().isPermitted(Permissions.Action.PAGE_MODIFY.getPermission())) {
                                throw new NotFoundException("Can't find this page and you have no rights to instance it; " + path);
                            }
                            else {
                                //when we're dealing with a resource, we validate the resource type (the path part coming after the "/resource" path)
                                // to make sure it can be mapped in our ontology (during page creation)
                                if (requestedUri.getPath().startsWith(Settings.RESOURCE_ENDPOINT)) {
                                    java.nio.file.Path requestedPath = Paths.get(requestedUri.getPath());
                                    if (requestedPath.getNameCount() != 3) {
                                        throw new IOException("Encountered an (unexisting) resource URL with the wrong format. Need at least 3 segments: /resource/<type>/<id>;" + requestedUri);
                                    }
                                    else {
                                        //index wise: since part 0 is "resource" (validated above), we validate index 1 to be the type
                                        URI resourceTypeCurie = URI.create(Settings.instance().getRdfOntologyPrefix() + ":" + requestedPath.getName(1).toString());
                                        RdfClass resourcedType = RdfFactory.getClassForResourceType(resourceTypeCurie);
                                        if (resourcedType == null) {
                                            throw new IOException("Encountered an (unexisting) resource URL with an unknown RDF ontology type (" + resourceTypeCurie + ");" + requestedUri);
                                        }
                                    }
                                }

                                //this means we redirected from the new-page-selection page
                                String newPageTemplateName = null;
                                String newPageCopyUrl = null;
                                Boolean newPageCopyLink = false;
                                if (R.cacheManager().getFlashCache().getTransferredEntries() != null) {
                                    newPageTemplateName = (String) R.cacheManager().getFlashCache().getTransferredEntries().get(CacheKeys.NEW_PAGE_TEMPLATE_NAME.name());
                                    newPageCopyUrl = (String) R.cacheManager().getFlashCache().getTransferredEntries().get(CacheKeys.NEW_PAGE_COPY_URL.name());
                                    newPageCopyLink = (Boolean) R.cacheManager().getFlashCache().getTransferredEntries().get(CacheKeys.NEW_PAGE_COPY_LINK.name());
                                }

                                //OPTION 5: there's a template-selection in the flash cache (we came from the page-selection page)
                                // Note that we use the flash cache as a template-selection mechanism to keep the final URL clean
                                if (!StringUtils.isEmpty(newPageTemplateName)) {
                                    //check if the name exists and is all right
                                    HtmlTemplate pageTemplate = TemplateCache.instance().getByTagName(newPageTemplateName);
                                    if (pageTemplate != null && pageTemplate instanceof PageTemplate) {
                                        Template
                                                        newPageInstance =
                                                        R.resourceManager().newTemplate(new StringSource(requestedUri, pageTemplate.createNewHtmlInstance(false), MimeTypes.HTML, optimalLocale));

                                        //this will allow the blocks javascript/css to be included
                                        this.setBlocksMode(HtmlTemplate.ResourceScopeMode.edit, newPageInstance);

                                        retValBuilder = Response.ok(newPageInstance);
                                    }
                                    else {
                                        throw new InternalServerErrorException("Requested to instance a new page (" + requestedUri + ") with an invalid page template name; " + newPageTemplateName);
                                    }
                                }
                                else if (!StringUtils.isEmpty(newPageCopyUrl)) {

                                    //read the page we'll copy from
                                    Page copyPage = R.resourceManager().get(URI.create(newPageCopyUrl), Page.class);

                                    //First, we'll read in the normalized code of the copy page (important: in edit mode because we need the edit imports).
                                    //Note that we need to read the normalized version because the templates might have changed in the mean time (between store and copy)
                                    //then, we'll adapt the html a little bit by calling prepareForCopying() on it
                                    //Finally, we'll render it out in a new template, again in edit mode.
                                    if (copyPage != null) {

                                        //we need to pull the normalized html through the template engine for this to work
                                        Template copyTemplate = R.resourceManager().newTemplate(copyPage);

                                        //activate blocks mode for the old template so we copy everything, not just the public html
                                        this.setBlocksMode(HtmlTemplate.ResourceScopeMode.edit, copyTemplate);

                                        Source source = new StringSource(copyPage.getPublicAbsoluteAddress(),
                                                                         copyTemplate.render(),
                                                                         copyPage.getMimeType(),
                                                                         copyPage.getLanguage());

                                        //pull the source through our template controllers for some last-minute adaptations to the html source
                                        source = HtmlTemplate.prepareForCopy(source, requestedUri, optimalLocale);

                                        //effectively make a copy
                                        PageSource html = new PageSourceCopy(source, newPageCopyLink);

                                        Template template;
                                        try (InputStream is = html.newInputStream()) {
                                            template = R.resourceManager().newTemplate(new StringSource(requestedUri, IOUtils.toString(is), MimeTypes.HTML, optimalLocale));
                                        }

                                        //this will allow the blocks javascript/css to be included
                                        this.setBlocksMode(HtmlTemplate.ResourceScopeMode.edit, template);

                                        retValBuilder = Response.ok(template);
                                    }
                                    else {
                                        throw new InternalServerErrorException("Requested to instance a new page (" + requestedUri + ") with an unknown page to copy from; " + newPageCopyUrl);
                                    }
                                }
                                //here, the page doesn't exist, but we can instance it
                                else {

                                    //We allow the user to instance any kind of URL since it can be typed in the browser
                                    //However, the address is somehow mapped to disk, so make sure it's valid or redirect to an auto-fixed
                                    //address when it's not valid. Because we'll parse the URL extensively, let's do it here, early on (see below)
                                    String safePage = this.safePagePath(path);

                                    //OPTION 6: URL is not safe: fix it and redirect
                                    if (!path.equals(safePage)) {
                                        retValBuilder = Response.seeOther(UriBuilder.fromUri(requestedUri).replacePath(safePage).build());
                                    }
                                    //OPTION 7: show the instance-new page list
                                    else {
                                        //we'll use the admin-interface language to render this page, not the language of the content
                                        R.i18n().setManualLocale(R.i18n().getBrowserLocale());

                                        Template newPageTemplateList = new_page.get().getNewTemplate();
                                        newPageTemplateList.set(core.Entries.NEW_PAGE_TEMPLATE_URL.getValue(), requestedUri.toString());
                                        newPageTemplateList.set(core.Entries.NEW_PAGE_TEMPLATE_TEMPLATES.getValue(), this.buildPageTemplateMap());
                                        newPageTemplateList.set(core.Entries.NEW_PAGE_TEMPLATE_TRANSLATIONS.getValue(), this.searchAllPageTranslations(requestedUri));

                                        //Note: we don't set the edit mode for safety: it makes sure the user has no means to save the in-between selection page
                                        this.setBlocksMode(HtmlTemplate.ResourceScopeMode.create, newPageTemplateList);

                                        retValBuilder = Response.ok(newPageTemplateList);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Response retVal = retValBuilder.build();

        //if we're redirecting straight away, but we have some entities in the flash cache, we'll propagate them again,
        //otherwise we would lose eg. feedback messages (eg. when a successful login redirects automatically from "/" to "/en/")
        if (retVal.getStatus() == Response.Status.SEE_OTHER.getStatusCode() && R.cacheManager().getFlashCache().getTransferredEntries() != null) {
            R.cacheManager().getFlashCache().putAll(R.cacheManager().getFlashCache().getTransferredEntries());
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void setBlocksMode(HtmlTemplate.ResourceScopeMode mode, Template template)
    {
        //this one is used by HtmlParser to doIsValid if we need to include certain tags
        R.cacheManager().getRequestCache().put(CacheKeys.BLOCKS_MODE, mode);

        //for velocity templates
        template.set(CacheKeys.BLOCKS_MODE.name(), mode.name());
    }
    private List<Map<String, String>> buildPageTemplateMap()
    {
        List<Map<String, String>> retVal = new ArrayList<>();

        for (HtmlTemplate template : TemplateCache.instance().values()) {
            if (template instanceof PageTemplate && template.getDisplayType() != HtmlTemplate.MetaDisplayType.HIDDEN) {

                ImmutableMap.Builder<String, String> map = ImmutableMap.<String, String>builder()
                                .put(core.Entries.NEW_PAGE_TEMPLATE_NAME.getValue(), template.getTemplateName())
                                .put(core.Entries.NEW_PAGE_TEMPLATE_TITLE.getValue(), template.getTitle());

                if (!StringUtils.isEmpty(template.getDescription())) {
                    map.put(core.Entries.NEW_BLOCK_DESCRIPTION.getValue(), template.getDescription());
                }

                retVal.add(map.build());
            }
        }

        Collections.sort(retVal, new MapComparator(core.Entries.NEW_PAGE_TEMPLATE_TITLE.getValue()));

        return retVal;
    }
    private Map<Locale, Page> searchAllPageTranslations(URI uri)
    {
        Map<Locale, Page> retVal = new LinkedHashMap<>();

        for (Map.Entry<String, Locale> l : R.configuration().getLanguages().entrySet()) {
            Locale lang = l.getValue();
            UriBuilder translatedUri = UriBuilder.fromUri(uri);
            if (R.i18n().getUrlLocale(uri, translatedUri, lang) != null) {
                Page transPage = R.resourceManager().get(translatedUri.build(), Page.class);
                if (transPage != null) {
                    retVal.put(lang, transPage);
                }
            }
        }

        return retVal;
    }
    /**
     * This is modified version of StringFunctions.prepareSeoValue()
     */
    private String safePagePath(String path)
    {
        String retVal = path.trim();

        if (!StringUtils.isEmpty(retVal)) {
            //convert all special chars to ASCII
            retVal = StringFunctions.webNormalizeString(retVal);
            //this might be extended later on (note that we need to allow slashes because the path might have multiple segments)
            retVal = retVal.replaceAll("[^a-zA-Z0-9_ \\-/.]", "");
            //replace whitespace with dashes
            retVal = retVal.replaceAll("\\s+", "-");
            //replace double dashes with single dashes
            retVal = retVal.replaceAll("-+", "-");
            //make sure the path doesn't begin or end with a dash
            retVal = StringUtils.strip(retVal, "-");

            //Note: don't do a toLowerCase, it messes up a lot of resource-addresses, eg. /resource/SmithMark/360

            //see for inspiration: http://stackoverflow.com/questions/417142/what-is-the-maximum-length-of-a-url-in-different-browsers
            final int MAX_LENGTH = 2000;
            if (retVal.length() > MAX_LENGTH) {
                retVal = retVal.substring(0, MAX_LENGTH);
                retVal = retVal.substring(0, retVal.lastIndexOf("-"));
            }
        }

        return retVal;
    }
}

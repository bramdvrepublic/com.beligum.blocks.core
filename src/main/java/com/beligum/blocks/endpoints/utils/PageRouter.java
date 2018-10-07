package com.beligum.blocks.endpoints.utils;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.ResourceAction;
import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.base.resources.ifaces.Source;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.endpoints.PageAdminEndpoint;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.filesystem.index.entries.IndexEntry;
import com.beligum.blocks.filesystem.index.entries.pages.IndexSearchResult;
import com.beligum.blocks.filesystem.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ontology.factories.Terms;
import com.beligum.blocks.rdf.sources.PageSource;
import com.beligum.blocks.rdf.sources.PageSourceCopy;
import com.beligum.blocks.config.Permissions;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.utils.RdfTools;
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

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

import static com.beligum.blocks.config.Permissions.PAGE_CREATE_COPY_ALL_PERM;
import static com.beligum.blocks.config.Permissions.PAGE_CREATE_TEMPLATE_ALL_PERM;

/**
 * This class holds the general algorithm we follow when parsing publicly incoming requests
 * for a page. It handles all the details of RDF resource URIs, translated URIs, aliasing, etc.
 * <p>
 * The algorithm in more detail:
 * <p>
 * First, a general remark: we force the page to be created to have a language using redirects. It's not strictly necessary,
 * but it helps us a lot while linking translations together during page persist; by supplying a part in the URL-path
 * that identifies the language of the page, it's far easier to auto-generate translation URLs, and as a plus,
 * it introduces some structure to the site.
 * <p>
 * We have a fixed set of ordered possibilities:
 * - the page exists, but it's an external resource (eg. a Wikidata page or a Geonames page) -> redirect to the URI of the external site
 * - the page exists and it's Page resource can be resolved -> serve that resource
 * - the page doesn't exist, but we can find another page with the URL as alias -> redirect to the correct URL of that page
 * - the page doesn't exist, but we can find a similar page in another language -> extract the resource-uri from that page and reverse-lookup the public URI in the requested language
 * - the page doesn't exist & the user has no rights -> 404
 * - the page doesn't exist & the user has instance rights & no language present -> try to detectAndReplace a decent language and redirect to a language-prefixed url (recursive call with roundtrip)
 * - the page doesn't exist & the user has instance rights & language is present & page template in flash cache -> render a page template instance (not yet persisted)
 * - the page doesn't exist & the user has instance rights & language is present & nothing in flash cache & page slug is unsafe -> redirect to safe variant
 * - the page doesn't exist & the user has instance rights & language is present & nothing in flash cache & page slug is safe -> show new page selection list
 */
public class PageRouter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    /**
     * The raw, unprocessed and unsafe requested URI
     */
    private final URI unsafeUri;

    /**
     * The best-guess locale, largely based on the requested URI
     */
    private final Locale locale;

    /**
     * The requested URI that has been cleaned in the light of safety measurements
     */
    private URI requestedUri;

    /**
     * The requested query parameters that are acceptable
     */
    private MultivaluedMap<String, String> queryParameters;

    /**
     * The target URI this request resolved to (might need a redirection)
     */
    private URI targetUri;

    /**
     * The translation of a possible type query parameter to a (supported) RDF format or null if not present/supported.
     */
    private Format rdfType;

    /**
     * The RDF resource information, extracted from the requested URI
     */
    private RdfTools.RdfResourceUri rdfResourceUriAnalyzer;

    /**
     * A flag indicating the target URI can't be found in this request and needs a redirect
     */
    private boolean needsRedirection;

    /**
     * The requested page object that is filled as soon as it's found
     */
    private Page requestedPage;

    /**
     * A flag indicating we have permission to create new pages
     */
    private boolean allowCreateNew;

    /**
     * A flag indicating we have permission to edit existing pages
     */
    private boolean allowEditExisting;

    /**
     * The following newPage... variables will hold the values that are passed from the "Create new page" via the flash cache,
     * note that it's important they are initialized conservatively
     */
    private String newPageTemplateName = null;
    private String newPageCopyUrl = null;
    private boolean newPageCopyLink = false;
    private boolean newPagePersistent = false;
    private MultivaluedMap<String, String> newPageExtraParams = new MultivaluedHashMap<>();

    /**
     * This will hold the template when we're creating, copying or selecting a new page
     */
    private Template adminTemplate;

    /**
     * Just a cached index connection
     */
    private LuceneQueryConnection cachedQueryConnection;

    //-----CONSTRUCTORS-----
    /**
     * Note: the main objective here is to set the targetUri variable.
     * As long as it's null, we haven't found our target resource yet and we keep looking.
     * This also allow us to skip later steps as soon as it was found
     *
     * @param anonymous
     */
    public PageRouter(boolean anonymous) throws InternalServerErrorException
    {
        this.unsafeUri = R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri();
        this.locale = R.i18n().getOptimalLocale(this.unsafeUri);
        this.needsRedirection = false;
        this.allowCreateNew = anonymous ? false : R.securityManager().isPermitted(Permissions.PAGE_CREATE_ALL_PERM);
        this.allowEditExisting = anonymous ? false : R.securityManager().isPermitted(Permissions.PAGE_UPDATE_ALL_PERM);

        // --- The basic steps
        this.doRequestUriCleaning();
        this.doRdfResourceDetection();
        this.doRdfTypeDetection();
        this.doResolvePage();

        // --- The additional edge cases
        if (this.assertUnfinished()) {
            this.doDetectAliases();
            this.doDetectTranslatedLink();
            this.doDetectMissingLanguage();
        }

        // --- The create new page cases
        if (this.assertUnfinished() && this.assertAllowCreateNew()) {
            this.doCheckValidResourceUrl();
            this.doExtractCreateVariables();
            this.doBuildNewTemplatePage();
            this.doBuildNewCopyPage();
            this.doPersistNewPage();
            this.doBuildNewPageSelection();
        }
    }

    //-----PUBLIC METHODS-----
    public URI getRequestedUri()
    {
        return this.requestedUri;
    }
    public boolean needsRedirection()
    {
        return this.needsRedirection;
    }
    public URI getTargetUri()
    {
        return this.targetUri;
    }
    public Format getTargetRdfType()
    {
        return this.rdfType;
    }
    public Page getRequestedPage()
    {
        return this.requestedPage;
    }
    public Template buildRequestedPageTemplate()
    {
        Template retVal = null;

        if (this.getRequestedPage() != null) {

            Page page = this.getRequestedPage();

            //this is the retVal for the public page
            retVal = R.resourceManager().newTemplate(page);

            //this will allow the extra edit javascript/css to be included
            if (page.isPermitted(ResourceAction.UPDATE)) {
                this.setResourceAction(ResourceAction.UPDATE);
            }
        }

        return retVal;
    }
    public Template getAdminTemplate()
    {
        return this.adminTemplate;
    }

    //-----PROTECTED METHODS-----

    //-----WORKER METHODS-----
    /**
     * This is a step we always need to do and provides some URI hacking security
     */
    private void doRequestUriCleaning()
    {
        if (this.assertUnfinished()) {

            // make sure the path always starts with a slash
            // (eg. this is not the case when this endpoint matched the root ("") path)
            String path = this.unsafeUri.getPath();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            //security; rebuild the url instead of blindly accepting what comes in
            UriBuilder uriBuilder = UriBuilder.fromUri(R.configuration().getSiteDomain()).replacePath(path);

            //remove all unsupported query params from the page URI, so it doesn't matter if we
            // eg. save a page while on page 6 in a search result, or in the root; they should resolve to
            //     and save the same page, from the user-admin's point of view.
            //Note: the reason this works while creating a page (eg. the page_url and page_class_name query params),
            //      is because that callback is caught by the /admin/page/template endpoint and those parameters
            //      are in the flash cache once this request comes in.
            this.queryParameters = PageSource.transferCleanedQueryParams(uriBuilder, R.requestContext().getJaxRsRequest().getUriInfo().getQueryParameters());

            this.requestedUri = uriBuilder.build();

            // We allow the user to instance any kind of URL since it can be typed in the browser
            // However, the address is somehow mapped to disk, so make sure it's valid or redirect to an auto-fixed
            // address when it's not valid. Because we'll parse the URL extensively, let's do it here, early on (see below)
            // If the URL is not safe, fix it and redirect
            String safePagePath = new PageUrlValidator().createSafePagePath(this.requestedUri);
            if (!this.requestedUri.getPath().equals(safePagePath)) {
                this.needsRedirection = true;
                this.targetUri = UriBuilder.fromUri(this.requestedUri).replacePath(safePagePath).build();
            }
        }
    }
    /**
     * Check if a type query parameter was passed, so we need to redirect away to a supporting
     * method.
     */
    private void doRdfTypeDetection()
    {
        if (this.assertUnfinished()) {

            //if we have an explicit type parameter, use that, otherwise, use the http headers
            if (this.queryParameters.containsKey(ResourceRequest.TYPE_QUERY_PARAM)) {
                //let's check if the passed type is a supported RDF type
                this.rdfType = Format.fromMimeType(this.queryParameters.getFirst(ResourceRequest.TYPE_QUERY_PARAM));

                //Important: don't do this because (in case of the type parameter) it would cache this rdf version as the regular page
                //this.requestedUri = UriBuilder.fromUri(this.requestedUri).replaceQueryParam(ResourceRequest.TYPE_QUERY_PARAM, null).build();
            }
            else {
                List<MediaType> acceptableMediaTypes = R.requestContext().getJaxRsRequest().getAcceptableMediaTypes();
                if (acceptableMediaTypes != null && !acceptableMediaTypes.isEmpty()) {
                    MediaType mainType = acceptableMediaTypes.iterator().next();
                    //note that we strip the possible parameters and charset from the media type
                    //and reconstruct the mimetype from its parts so the match is as broad as possible
                    this.rdfType = Format.fromMimeType(mainType.getType() + "/" + mainType.getSubtype());
                }
            }
        }
    }
    /**
     * Check and see if we can stop short because the requested URI is in the RDF resource
     * format and needs redirection anyway
     */
    private void doRdfResourceDetection()
    {
        if (this.assertUnfinished()) {

            //this will parse the URI structure in the light of possible detection of an RDF resource URI
            this.rdfResourceUriAnalyzer = new RdfTools.RdfResourceUri(this.requestedUri);

            // First, check if we're dealing with an external resource.
            // If it's associated endpoint wants to redirect to another URL (eg. when we use resources of an external ontology)
            // don't try to lookup the resource locally, but redirect there.
            if (this.rdfResourceUriAnalyzer.isValid()) {
                if (this.rdfResourceUriAnalyzer.isTyped()) {
                    RdfQueryEndpoint endpoint = this.rdfResourceUriAnalyzer.getResourceClass().getEndpoint();
                    if (endpoint != null) {
                        this.targetUri = endpoint.getExternalResourceId(this.requestedUri, this.locale);
                        this.needsRedirection = true;
                    }
                }
            }
        }
    }
    /**
     * This is the main function of resolving a URI to a page
     */
    private void doResolvePage()
    {
        if (this.assertUnfinished()) {
            // Since we allow the user to instance pretty url's, it's mime type will not always be clear, so pass it on explicitly
            //Note: this will return null if the resource wasn't found
            this.requestedPage = R.resourceManager().get(this.requestedUri, Page.class);

            //if the uri resolved to a page, it's the target uri of the resource we're looking for,
            //so set the target uri
            if (this.requestedPage != null) {
                this.targetUri = this.requestedUri;
            }
        }
    }
    /**
     * Search the index for sameAs aliases and check if the requested path is saved as an alias of another page
     */
    private void doDetectAliases() throws InternalServerErrorException
    {
        if (this.assertUnfinished()) {

            try {
                //don't really know if we should search for the formal path or just use the randomPage (or use StringFunctions.getRightOfDomain()),
                //but let's start with this (eg. no query params like languages included) and see where we end up...
                String searchUri = this.requestedUri.getPath();

                BooleanQuery pageQuery = new BooleanQuery();
                //we'll search for pages that have an alias (possibly/probably non-existent)
                pageQuery.add(new TermQuery(new Term(Terms.sameAs.getCurieName().toString(), searchUri)), BooleanClause.Occur.SHOULD);
                //and also for 'raw' resource url (eg. the backoffice uri that's used to link all translations together)
                pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.resource.name(), searchUri)), BooleanClause.Occur.SHOULD);
                //makes sense to make room for as much language-triples as we have clauses
                IndexSearchResult results = this.getMainPageQueryConnection().search(pageQuery, pageQuery.clauses().size() * R.configuration().getLanguages().size());
                PageIndexEntry selectedEntry = PageIndexEntry.selectBestForLanguage(results, this.locale);

                //by default, we'll redirect to the id of the found resource (eg. the public URI of the page)
                String selectedEntryAddress = selectedEntry == null ? null : selectedEntry.getId();

                //this detects if the above search matched on the resource uri (and not the sameAs)
                if (selectedEntry != null && selectedEntry.getResource().equals(searchUri)) {

                    //If we're dealing with a sub-resource, we need to change the address because:
                    // 1) it doesn't really exist
                    // 2) it would redirect forever because a sub-resource doesn't have a 'real' address
                    //So we redirect to the parent's id
                    //Also note that (unlike real resources, see check just below) we can't offer the user the choice to make this page,
                    //because sub-resources are not supposed to have real pages; they only exist in the context of a parent page.
                    if (selectedEntry.getParentId() != null) {
                        selectedEntryAddress = selectedEntry.getParentId();
                    }
                    //when we're editing pages it needs to jump out of the redirect in this case because when creating a new resource page in eg. english,
                    //and we want to instance it's translation, it would redirect back to the english page instead of allowing us to
                    //instance the translated page
                    else if (this.allowEditExisting) {
                        selectedEntryAddress = null;
                    }
                }

                //little protection against eternal redirects
                if (selectedEntryAddress != null && selectedEntryAddress.equals(searchUri)) {
                    selectedEntryAddress = null;
                }

                //this means we found something, so save the redirection url
                if (selectedEntryAddress != null) {
                    this.targetUri = URI.create(selectedEntryAddress);
                    this.needsRedirection = true;
                }
            }
            catch (IOException e) {
                throw new InternalServerErrorException("Error while detecting aliases for " + this.requestedUri, e);
            }
        }
    }
    /**
     * It's possible the user clicked a 'translated link' (eg. just the lang param was changed),
     * but the translation page doesn't have the same structure as the original language (or is in fact totally different).
     * Since we link pages together by using a more permanent resource url, we'll translate the url back for every language and see if it exists.
     * If it does, we'll extract the resource uri and go looking for a page pointing to that resource and having the right language
     */
    private void doDetectTranslatedLink() throws InternalServerErrorException
    {
        if (this.assertUnfinished()) {

            try {
                //more or less the same remark here as with doDetectAliases()
                String searchUri = this.requestedUri.getPath();

                BooleanQuery pageQuery = new BooleanQuery();
                //part a: first, we go hunting for the uri that _does_ exist
                Collection<Locale> allLanguages = R.configuration().getLanguages().values();
                for (Locale locale : allLanguages) {
                    if (!locale.equals(this.locale)) {
                        //replace the language of the uri by the language of the loop
                        UriBuilder uriBuilder = UriBuilder.fromUri(searchUri);
                        R.i18n().getUrlLocale(this.requestedUri, uriBuilder, locale);
                        //we'll search for a page that has the translated request uri as it's address
                        pageQuery.add(new TermQuery(new Term(IndexEntry.Field.id.name(), StringFunctions.getRightOfDomain(uriBuilder.build()).toString())), BooleanClause.Occur.SHOULD);
                    }
                }

                LuceneQueryConnection queryConnection = this.getMainPageQueryConnection();
                IndexSearchResult results = queryConnection.search(pageQuery, allLanguages.size());
                //part b: if it exist, extract it's resource uri and search for a page pointing to it using the right language
                if (!results.isEmpty()) {
                    //since all resources should be the same, we take the first match
                    String resourceUri = ((PageIndexEntry) results.iterator().next()).getResource();
                    pageQuery = new BooleanQuery();
                    pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.resource.name(), resourceUri)), BooleanClause.Occur.FILTER);
                    pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.language.name(), this.locale.getLanguage())), BooleanClause.Occur.FILTER);
                    results = queryConnection.search(pageQuery, -1);

                    PageIndexEntry selectedEntry2 = PageIndexEntry.selectBestForLanguage(results, this.locale);

                    //this means we found something, so save the redirection url
                    if (selectedEntry2 != null) {
                        //we'll redirect to the id (eg. the public URI of the page) of the found resource
                        this.targetUri = URI.create(selectedEntry2.getId());
                        this.needsRedirection = true;
                    }
                }
            }
            catch (IOException e) {
                throw new InternalServerErrorException("Error while detecting translated link for " + this.requestedUri, e);
            }
        }
    }
    /**
     * If we reach this stage, we're 'deep enough' in the processing algo to assume there's a language present in the requested URI
     * so it's a good time to demand for a language or redirect away otherwise (in which case this method will be called again)
     * Note that we can't put this check a lot earlier because of eg. alias checking; eg. old URLs who don't have any formatting restraints
     * We always need a language, so make sure we have one.
     * Note that, as a general language-selection mechanism, we only support URI locales,
     * but when there's no such locale found, we try to redirect to the one requested by the browser
     * and if all fails, we redirect to the default, configured locale
     */
    private void doDetectMissingLanguage() throws InternalServerErrorException
    {
        if (this.assertUnfinished()) {
            Locale requestedUriLocale = R.i18n().getUrlLocale(this.requestedUri);

            //no language in the URL means we will redirect to a new address with a detected language
            if (requestedUriLocale == null) {

                //if a request comes in without URL language, we launch some heuristics to fill it in:
                // 1) check if the page we come from is one of ours and if it is, use the same language for continuity
                Locale redirectLocale = null;
                String referer = R.requestContext().getJaxRsRequest().getHeaders().getFirst(HttpHeaders.REFERER);
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
                    throw new InternalServerErrorException("Encountered null-valued default language; this shouldn't happen; " + requestedUri);
                }
                else {
                    //we redirect to the &lang=.. (instead of lang-prefixed url) when we're dealing with a resource to have a cleaner URL
                    //Note that it gets stored locally with a lang-prefixed path though; see DefaultPageImpl.toResourceUri for details
                    //Note: don't use isValid() here because it will redirect /resource/... to eg. /en/resource/...
                    //      and it will fail the validity test down below. This is only a low-level lexicographical test, not a resource check yet
                    this.needsRedirection = true;
                    if (this.rdfResourceUriAnalyzer.isPrefixed()) {
                        this.targetUri = UriBuilder.fromUri(this.requestedUri).queryParam(I18nFactory.LANG_QUERY_PARAM, redirectLocale.getLanguage()).build();
                    }
                    else {
                        this.targetUri = UriBuilder.fromUri(this.requestedUri).replacePath("/" + redirectLocale.getLanguage() + this.requestedUri.getPath()).build();
                    }
                }
            }
        }
    }
    /**
     * If we're about to create a resource-prefixed URI, don't allow the creation of invalid resource URIs
     */
    private void doCheckValidResourceUrl() throws InternalServerErrorException
    {
        if (this.assertUnfinished() && this.assertAllowCreateNew()) {
            if (this.rdfResourceUriAnalyzer.isPrefixed() && !this.rdfResourceUriAnalyzer.isValid()) {
                throw new InternalServerErrorException("Encountered an invalid resource URI;" + this.requestedUri);
            }
        }
    }
    /**
     * Extract the variables from the flash cache if we just came from the create-new-page page.
     * Note that we can't pass them on via query parameters because we need to keep the final URL
     * of the page we're creating intact.
     */
    private void doExtractCreateVariables()
    {
        if (this.assertUnfinished() && this.assertAllowCreateNew()) {
            if (R.cacheManager().getFlashCache().getTransferredEntries() != null) {

                this.newPageTemplateName = R.cacheManager().getFlashCache().getTransferredEntries().get(CacheKeys.NEW_PAGE_TEMPLATE_NAME.name());

                this.newPageCopyUrl = R.cacheManager().getFlashCache().getTransferredEntries().get(CacheKeys.NEW_PAGE_COPY_URL.name());

                //If we make a copy from another page, we need to know if we need to treat it as a translation (re-using it's resource URI)
                //or treat it as a completely new resource. So if the user passed a copy url, it also needs to decide which mode to copy it in.
                Boolean linkToSource = R.cacheManager().getFlashCache().getTransferredEntries().get(CacheKeys.NEW_PAGE_COPY_LINK.name());
                if (!StringUtils.isEmpty(this.newPageCopyUrl) && linkToSource == null) {
                    throw new InternalServerErrorException("Asking to create a page by copying an existing page (" + this.newPageCopyUrl
                                                           + ") without supplying the boolean to link it or not. This is not allowed; " + this.getRequestedPage());
                }
                else if (linkToSource != null) {
                    this.newPageCopyLink = linkToSource;
                }

                //only modify the default if we have a value
                Boolean persistNewPage = R.cacheManager().getFlashCache().getTransferredEntries().get(CacheKeys.NEW_PAGE_PERSISTENT.name());
                if (persistNewPage != null) {
                    this.newPagePersistent = persistNewPage;
                }

                this.newPageExtraParams = R.cacheManager().getFlashCache().getTransferredEntries().get(CacheKeys.NEW_PAGE_EXTRA_PARAMS.name());
            }
        }
    }
    /**
     * There's a template-selection in the flash cache (we came from the page-selection page)
     * Note that we use the flash cache as a template-selection mechanism to keep the final URL clean
     */
    private void doBuildNewTemplatePage() throws InternalServerErrorException
    {
        if (this.assertUnfinished() && this.assertAllowCreateNew() && !this.assertCreatedNew()) {

            //make sure we have permission to create a page copy
            R.securityManager().checkPermission(PAGE_CREATE_TEMPLATE_ALL_PERM);

            if (!StringUtils.isEmpty(this.newPageTemplateName)) {

                //check if the name exists and is all right
                TemplateCache templateCache = TemplateCache.instance();
                HtmlTemplate pageTemplate = templateCache.getByTagName(this.newPageTemplateName);
                if (pageTemplate != null && pageTemplate instanceof PageTemplate) {

                    if (!templateCache.isDisabled(pageTemplate)) {

                        this.adminTemplate = R.resourceManager().newTemplate(new StringSource(this.requestedUri, pageTemplate.createNewHtmlInstance(false), MimeTypes.HTML, this.locale));

                        //transfer the passed-in extra params into the template context
                        this.transferExtraParams(this.adminTemplate.getContext(), this.newPageExtraParams);

                        //this will allow the extra edit javascript/css to be included
                        this.setResourceAction(ResourceAction.UPDATE);

                        //Note: we deliberately don't set the targetUri because we don't have a real target yet (only after the first save)
                        //(except when we'll persist the page in one go, see later)
                    }
                    else {
                        throw new InternalServerErrorException("Requested to instance a new page (" + requestedUri + ") with a disabled page template; " + newPageTemplateName);
                    }
                }
                else {
                    throw new InternalServerErrorException("Requested to instance a new page (" + requestedUri + ") with an invalid page template name; " + newPageTemplateName);
                }

            }
        }
    }
    /**
     * If there's a copy page URL in the flash cache, we're expected to create a copy of an existing page
     */
    private void doBuildNewCopyPage() throws InternalServerErrorException
    {
        if (this.assertUnfinished() && this.assertAllowCreateNew() && !this.assertCreatedNew()) {

            //make sure we have permission to create a page copy
            R.securityManager().checkPermission(PAGE_CREATE_COPY_ALL_PERM);

            if (!StringUtils.isEmpty(this.newPageCopyUrl)) {

                //read the page we'll copy from
                Page copyPage = R.resourceManager().get(URI.create(this.newPageCopyUrl), Page.class);

                //First, we'll read in the normalized code of the copy page (important: in edit mode because we need the edit imports).
                //Note that we need to read the normalized version because the templates might have changed in the mean time (between store and copy)
                //then, we'll adapt the html a little bit by calling prepareForCopying() on it
                //Finally, we'll render it out in a new template, again in edit mode.
                if (copyPage != null) {

                    try {
                        PageTemplate pageTemplate = copyPage.createAnalyzer().getTemplate();
                        if (pageTemplate == null || TemplateCache.instance().isDisabled(pageTemplate)) {
                            throw new IOException("Requested to copy a page (" + requestedUri + ") with an unknown or disabled page template; " + pageTemplate);
                        }

                        //we need to pull the normalized html through the template engine for this to work
                        Template copyTemplate = R.resourceManager().newTemplate(copyPage);

                        //note: extra params are inserted in the copy, not here

                        //activate blocks mode for the old template so we copy everything, not just the public html
                        this.setResourceAction(ResourceAction.UPDATE);

                        Source source = new StringSource(copyPage.getPublicAbsoluteAddress(),
                                                         copyTemplate.render(),
                                                         copyPage.getMimeType(),
                                                         copyPage.getLanguage());

                        //pull the source through our template controllers for some last-minute adaptations to the html source
                        source = HtmlTemplate.prepareForCopy(source, this.requestedUri, this.locale);

                        //effectively make a copy
                        PageSource html = new PageSourceCopy(source, this.newPageCopyLink);

                        try (InputStream is = html.newInputStream()) {
                            this.adminTemplate = R.resourceManager().newTemplate(new StringSource(this.requestedUri, IOUtils.toString(is), MimeTypes.HTML, this.locale));
                        }

                        //transfer the passed-in extra params into the template context
                        this.transferExtraParams(this.adminTemplate.getContext(), this.newPageExtraParams);

                        //this will allow the extra edit javascript/css to be included
                        this.setResourceAction(ResourceAction.UPDATE);

                        //note: we deliberately don't set the targetUri because we don't have a real target yet (only after the first save)
                        //(except when we'll persist the page in one go, see later)
                    }
                    catch (IOException e) {
                        throw new InternalServerErrorException("Error while building page copy of page " + this.newPageCopyUrl + " for " + this.requestedUri, e);
                    }
                }
                else {
                    throw new InternalServerErrorException("Requested to instance a new page (" + this.requestedUri + ") with an unknown page to copy from; " + this.newPageCopyUrl);
                }
            }
        }
    }
    private void doPersistNewPage() throws InternalServerErrorException
    {
        if (this.assertUnfinished() && this.assertAllowCreateNew() && this.assertCreatedNew()) {

            if (this.newPagePersistent) {
                try {
                    //sync these with the annotations on the PageAdminEndpoint.save() method
                    R.securityManager().checkPermissions(Permissions.PAGE_CREATE_ALL_PERM);

                    new PageAdminEndpoint().savePage(this.requestedUri, this.adminTemplate.render());

                    //we rendered and saved the page, so jump out of admin mode
                    this.adminTemplate = null;

                    //this will uniformly set the required variables for further handling
                    this.doResolvePage();
                }
                catch (IOException e) {
                    throw new InternalServerErrorException("Error while persisting a new page (" + requestedUri + ") of template; " + newPageTemplateName);
                }
            }
        }
    }
    /**
     * Show the instance-new page list to the end user
     */
    private void doBuildNewPageSelection()
    {
        if (this.assertUnfinished() && this.assertAllowCreateNew() && !this.assertCreatedNew()) {

            //we'll use the admin-interface language to render this page, not the language of the content
            R.i18n().setManualLocale(R.i18n().getBrowserLocale());

            this.adminTemplate = new_page.get().getNewTemplate();
            this.adminTemplate.set(core.Entries.NEW_PAGE_TEMPLATE_URL.getValue(), this.requestedUri.toString());
            this.adminTemplate.set(core.Entries.NEW_PAGE_TEMPLATE_TEMPLATES.getValue(), this.buildPageTemplateMap(this.adminTemplate));
            this.adminTemplate.set(core.Entries.NEW_PAGE_TEMPLATE_TRANSLATIONS.getValue(), this.searchAllPageTranslations(this.requestedUri));

            //Note: we don't set the edit mode for safety: it makes sure the user has no means to save the in-between selection page
            this.setResourceAction(ResourceAction.CREATE);
        }
    }

    //-----PRIVATE METHODS-----
    private boolean assertUnfinished()
    {
        //as long as the target uri is null, we keep resolving (so make sure you set it if complete)
        return this.targetUri == null;
    }
    private boolean assertAllowCreateNew()
    {
        return this.allowCreateNew;
    }
    private boolean assertCreatedNew()
    {
        return this.adminTemplate != null;
    }
    private LuceneQueryConnection getMainPageQueryConnection() throws IOException
    {
        //note: no synchronization needed, this is assumed to be all one thread
        if (this.cachedQueryConnection == null) {
            this.cachedQueryConnection = StorageFactory.getMainPageQueryConnection();
        }

        return this.cachedQueryConnection;
    }
    private void transferExtraParams(TemplateContext context, MultivaluedMap<String, String> newPageExtraParams)
    {
        if (newPageExtraParams != null) {
            for (Map.Entry<String, List<String>> param : newPageExtraParams.entrySet()) {
                if (param.getValue() != null && !param.getValue().isEmpty()) {
                    if (param.getValue().size() > 1) {
                        context.set(param.getKey(), param.getValue());
                    }
                    //let's convert single-valued lists to their object couterparts, it's more natural
                    else if (param.getValue().size() == 1) {
                        context.set(param.getKey(), param.getValue().get(0));
                    }
                }
                //there might be some interest in params being set, without it containing any value...
                //also, since params come in through the text of the URL, we opted to set empty ones as empty
                //strings instead of null
                else {
                    context.set(param.getKey(), "");
                }
            }
        }
    }
    private List<Map<String, String>> buildPageTemplateMap(Template template)
    {
        List<Map<String, String>> retVal = new ArrayList<>();

        TemplateCache templateCache = TemplateCache.instance();
        for (HtmlTemplate htmlTemplate : templateCache.getAllTemplates()) {
            if (htmlTemplate instanceof PageTemplate && htmlTemplate.getDisplayType() != HtmlTemplate.MetaDisplayType.HIDDEN && !templateCache.isDisabled(htmlTemplate)) {

                ImmutableMap.Builder<String, String> map = ImmutableMap.<String, String>builder()
                                .put(core.Entries.NEW_PAGE_TEMPLATE_NAME.getValue(), template.getContext().evaluate(htmlTemplate.getTemplateName()))
                                .put(core.Entries.NEW_PAGE_TEMPLATE_TITLE.getValue(), template.getContext().evaluate(htmlTemplate.getTitle()));

                if (!StringUtils.isEmpty(htmlTemplate.getDescription())) {
                    map.put(core.Entries.NEW_PAGE_TEMPLATE_DESCRIPTION.getValue(), template.getContext().evaluate(htmlTemplate.getDescription()));
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
    private void setResourceAction(ResourceAction action)
    {
        //this one is used by HtmlParser to doIsValid if we need to include certain tags
        R.cacheManager().getRequestCache().put(CacheKeys.RESOURCE_ACTION, action);
    }

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return "" + this.targetUri;
    }
}

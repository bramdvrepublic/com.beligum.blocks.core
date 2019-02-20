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

import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceAction;
import com.beligum.base.resources.ifaces.Source;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.endpoints.utils.PageUrlValidator;
import com.beligum.blocks.filesystem.index.reindex.*;
import com.beligum.blocks.filesystem.pages.PageFixTask;
import com.beligum.blocks.filesystem.pages.PageRepository;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontology.vocabularies.LocalVocabulary;
import com.beligum.blocks.rdf.ontology.vocabularies.local.factories.Classes;
import com.beligum.blocks.rdf.sources.NewPageSource;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TagTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.utils.comparators.MapComparator;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import gen.com.beligum.blocks.core.fs.html.templates.blocks.core.modals.new_block;
import gen.com.beligum.blocks.core.messages.blocks.core;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.beligum.blocks.config.StorageFactory.getMainPageIndexer;
import static com.beligum.blocks.config.StorageFactory.getTriplestoreIndexer;
import static gen.com.beligum.blocks.core.constants.blocks.core.*;

/**
 * Created by bram on 2/10/16.
 */
@Path("/blocks/admin/page")
public class PageAdminEndpoint
{
    //-----CONSTANTS-----
    private static Format ERROR_STAMP_FORMATTER = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

    //-----VARIABLES-----
    private static Object longRunningThreadLock = new Object();
    private static LongRunningThread longRunningThread = null;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static void endAllAsyncTasksNow()
    {
        synchronized (longRunningThreadLock) {
            if (longRunningThread != null) {
                longRunningThread.cancel();
            }
        }
    }
    /**
     * When a new page (a non-existing page) is requested, the (logged-in) user is presented with a list of page templates.
     * This endpoint is called when a page template is selected from that list.
     * Basically, we redirect back to the url where the page has to be created and put the name of the pagetemplate in the flashcache,
     * so the root endpoint (ApplicationEndpoint.getPageNew()) can detectAndReplace what to do.
     *
     * @param pageUrl          the url of the new page to be created
     * @param pageTemplateName the name of the page template to use for the new page
     * @param pageCopyUrl      the url of an existing page to make a copy from
     * @param pageCopyLink     true if the existing page needs to share the resource with the copy, false if a new resource URI will be generated
     * @param pagePersistent   true if the existing page needs to be persisted (saved) during creation
     */
    @GET
    @Path("/template")
    @RequiresPermissions(logical = Logical.OR,
                         value = { PAGE_CREATE_ALL_PERM,
                                   PAGE_CREATE_TEMPLATE_ALL_PERM,
                                   PAGE_CREATE_COPY_ALL_PERM })
    public Response getPageTemplate(@QueryParam(NEW_PAGE_URL_PARAM) URI pageUrl,
                                    @QueryParam(NEW_PAGE_TEMPLATE_PARAM) String pageTemplateName,
                                    @QueryParam(NEW_PAGE_COPY_URL_PARAM) String pageCopyUrl,
                                    @QueryParam(NEW_PAGE_COPY_LINK_PARAM) Boolean pageCopyLink,
                                    @QueryParam(NEW_PAGE_PERSISTENT_PARAM) Boolean pagePersistent) throws URISyntaxException
    {
        //if this endpoint is called directly, make sure we validate the url
        //or eg. the new URI() method below will fail
        String safePageUrl = new PageUrlValidator().createSafePagePath(pageUrl);

        if (StringUtils.isEmpty(safePageUrl)) {
            throw new InternalServerErrorException(core.Entries.newPageNoUrlError.toString());
        }
        else {

            if (!StringUtils.isEmpty(pageTemplateName)) {
                R.cacheManager().getFlashCache().put(CacheKeys.NEW_PAGE_TEMPLATE_NAME.name(), pageTemplateName);
                R.cacheManager().getFlashCache().put(CacheKeys.NEW_PAGE_PERSISTENT.name(), pagePersistent);
            }
            else if (!StringUtils.isEmpty(pageCopyUrl)) {
                R.cacheManager().getFlashCache().put(CacheKeys.NEW_PAGE_COPY_URL.name(), pageCopyUrl);
                R.cacheManager().getFlashCache().put(CacheKeys.NEW_PAGE_COPY_LINK.name(), pageCopyLink == null ? false : pageCopyLink);
                R.cacheManager().getFlashCache().put(CacheKeys.NEW_PAGE_PERSISTENT.name(), pagePersistent);
            }
            else {
                throw new InternalServerErrorException(core.Entries.newPageNoDataError.toString());
            }

            //makes the map non-immutable so we can remove the 'internal' params
            MultivaluedMap<String, String> extraParams = new MultivaluedHashMap<>();
            extraParams.putAll(R.requestContext().getJaxRsRequest().getUriInfo().getQueryParameters());
            extraParams.remove(NEW_PAGE_URL_PARAM);
            extraParams.remove(NEW_PAGE_TEMPLATE_PARAM);
            extraParams.remove(NEW_PAGE_COPY_URL_PARAM);
            extraParams.remove(NEW_PAGE_COPY_LINK_PARAM);
            extraParams.remove(NEW_PAGE_PERSISTENT_PARAM);
            R.cacheManager().getFlashCache().put(CacheKeys.NEW_PAGE_EXTRA_PARAMS.name(), extraParams);

            //redirect to the requested page with the flash cache filled in
            return Response.seeOther(new URI(safePageUrl)).build();
        }
    }

    /**
     * When a new block is created, this is the modal panel that is shown on drop:
     * a list of currently registered block-types in the system.
     */
    @GET
    @Path("/blocks")
    @RequiresPermissions(logical = Logical.OR,
                         value = { PAGE_UPDATE_ALL_PERM,
                                   PAGE_UPDATE_OWN_PERM })
    public Response getBlocks(@QueryParam(GET_BLOCKS_TYPEOF_PARAM) String typeOfStr, @QueryParam(GET_BLOCKS_TEMPLATE_PARAM) String pageTemplateName)
    {
        RdfClass typeOf = null;
        if (!StringUtils.isEmpty(typeOfStr)) {
            typeOf = RdfFactory.getClassForResourceType(typeOfStr);
        }
        //make sure we always have a type
        if (typeOf == null) {
            typeOf = Classes.DEFAULT_CLASS;
        }

        PageTemplate pageTemplate = null;
        if (!StringUtils.isEmpty(pageTemplateName)) {
            HtmlTemplate template = TemplateCache.instance().getByTagName(pageTemplateName);
            if (template != null) {
                if (template instanceof PageTemplate) {
                    pageTemplate = (PageTemplate) template;
                }
                else {
                    Logger.error("Encountered template name that was expected to be a page template, but it isn't; " + pageTemplateName);
                }
            }
            else {
                Logger.error("Encountered unknown template name; " + pageTemplateName);
            }
        }

        Template template = new_block.get().getNewTemplate();

        //build a list of all blocks that are accessible from this template and/or type
        TemplateCache cache = TemplateCache.instance();
        List<Map<String, String>> templates = new ArrayList<>();
        for (HtmlTemplate htmlTemplate : cache.getAllTemplates()) {

            //we're looking for visible tag templates (not pages)
            if (htmlTemplate instanceof TagTemplate && !htmlTemplate.getDisplayType().equals(HtmlTemplate.MetaDisplayType.HIDDEN)) {

                TagTemplate tagTemplate = (TagTemplate) htmlTemplate;

                //don't include the blocks that are disabled entirely or disabled only for this page template
                boolean enableTemplate = !cache.isDisabled(tagTemplate) && (pageTemplate == null || !cache.isDisabled(tagTemplate, pageTemplate));

                //note that the default class is an exception: we allow it to have all blocks
                //also note that we don't filter out templates that don't have any properties at all
                // (otherwise these would always be filtered out by the logic below)
                if (enableTemplate && !typeOf.equals(Classes.DEFAULT_CLASS) && tagTemplate.hasProperties()) {

                    //If we reach this point, we know the template-tag has some properties.
                    //These properties can be RDF properties ("property" attribute) or
                    //data hook properties ("data-property").
                    //The goal of this filter is to filter out all template-tags that can
                    //make the current page context invalid in the RDF sense. This means we
                    //are interested in RDF properties inside this template-tag that, once added
                    //to the current page (the whole reason this method is called), would be
                    //invalid because those properties are not part of the ontology of this page class.
                    //Note however that some of the template-tags have special dynamic property-lists
                    //that are generated on the fly, taking the current context (eg. the current local
                    //vocabulary and the page class context) into account.
                    //This also means that template-tags without rdf properties (only data properties)
                    //should always be allowed because they're transparent to the rdf parser.

                    //request the properties for the current context
                    Iterable<String> rdfProperties = htmlTemplate.getProperties(true, false, typeOf.getVocabulary(), typeOf);
                    Iterable<String> dataProperties = htmlTemplate.getProperties(false, true, typeOf.getVocabulary(), typeOf);

                    boolean hasRdfProperties = rdfProperties.iterator().hasNext();
                    boolean hasNonRdfProperties = dataProperties.iterator().hasNext();

                    //stop short if this template doesn't have any properties in the current context
                    enableTemplate = enableTemplate && (hasRdfProperties || hasNonRdfProperties);

                    //This is tricky: let's say a template-tag can have rdf properties (in general),
                    // but they are activated contextually (eg. blocks-fact-entry) and in the current context,
                    // there are none (eg. because the current page type doesn't have any properties in its ontology).
                    // Then this template-tag shouldn't be enabled because the user won't be able to create instances
                    // of this tempalte-tag (the client-side UI should prevent this)
                    enableTemplate = enableTemplate && !(tagTemplate.hasRdfProperties() && !hasRdfProperties);

                    if (enableTemplate) {
                        for (String p : rdfProperties) {

                            RdfProperty rdfProp = (RdfProperty) RdfFactory.getClassForResourceType(p);
                            if (rdfProp != null) {
                                enableTemplate = enableTemplate && typeOf.getProperties().contains(rdfProp);
                            }
                            else {
                                //let's do this so the developer is forced to fix it (the block won't show up, see log msg below)
                                enableTemplate = false;
                                Logger.warn("Encountered an RDF property ('" + p + "' in template '" + htmlTemplate.getTemplateName() +"')" +
                                            " that doesn't seem to resolve to a valid property in any known vocabulary." +
                                            " As a result, this template is disabled and won't show up in the list of blocks." +
                                            " Please fix this!");
                            }

                            if (!enableTemplate) {
                                break;
                            }
                        }
                    }
                }

                if (enableTemplate) {
                    ImmutableMap.Builder<String, String> map = ImmutableMap.<String, String>builder()
                                    .put(Entries.NEW_BLOCK_NAME.getValue(), template.getContext().evaluate(htmlTemplate.getTemplateName()))
                                    .put(Entries.NEW_BLOCK_TITLE.getValue(), template.getContext().evaluate(htmlTemplate.getTitle()));

                    if (!StringUtils.isEmpty(htmlTemplate.getDescription())) {
                        map.put(Entries.NEW_BLOCK_DESCRIPTION.getValue(), template.getContext().evaluate(htmlTemplate.getDescription()));
                    }

                    if (!StringUtils.isEmpty(htmlTemplate.getIcon())) {
                        map.put(Entries.NEW_BLOCK_ICON.getValue(), template.getContext().evaluate(htmlTemplate.getIcon()));
                    }

                    templates.add(map.build());
                }
            }
        }

        //sort the blocks by title
        Collections.sort(templates, new MapComparator(gen.com.beligum.blocks.core.constants.blocks.core.Entries.NEW_BLOCK_TITLE.getValue()));

        template.set(gen.com.beligum.blocks.core.constants.blocks.core.Entries.NEW_BLOCK_TEMPLATES.getValue(), templates);

        return Response.ok(template).build();
    }

    /**
     * When a user clicks on a block from the above list (getBlocks()), this endpoint is called.
     *
     * @param name the name of the block template to instantiate
     */
    @GET
    @Path("/block")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(logical = Logical.OR,
                         value = { PAGE_UPDATE_ALL_PERM,
                                   PAGE_UPDATE_OWN_PERM })
    public Response getBlock(@QueryParam(GET_BLOCK_NAME_PARAM) String name) throws IOException
    {
        HashMap<String, Object> retVal = new HashMap<>();

        HtmlTemplate htmlTemplate = TemplateCache.instance().getByTagName(name);

        //we drag-and-dropped a block in a certain page and this is the callback to get the html for that block,
        //so it makes sense to try to use the language of that page
        Locale lang = R.i18n().getOptimalRefererLocale();

        //note that we need to force this request to be that language, otherwise, a regular getOptimalLocale() will be used
        R.i18n().setManualLocale(lang);

        //signal the block-to-be-created we want all it's resources for update mode
        R.cacheManager().getRequestCache().put(CacheKeys.RESOURCE_ACTION, ResourceAction.UPDATE);

        // Warning: tag templates are stored/searched in the cache by their relative path (eg. see TemplateCache.putByRelativePath()),
        // so make sure you don't use that key to instance this resource or you'll re-instance the template, instead of an instance.
        // To avoid any clashes, we'll use the name of the instance as resource URI
        Template block = R.resourceManager().newTemplate(new StringSource(URI.create(htmlTemplate.getTemplateName()),
                                                                          htmlTemplate.createNewHtmlInstance(false),
                                                                          MimeTypes.HTML,
                                                                          //since this is the value of the template context lang,
                                                                          //it makes sense to instance the string in the same lang
                                                                          lang));

        retVal.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.BLOCK_DATA_PROPERTY_HTML.getValue(), block.render());

        //Note: the newArrayList is needed to make it work with json
        retVal.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.BLOCK_DATA_PROPERTY_INLINE_STYLES.getValue(), Lists.newArrayList(htmlTemplate.getInlineStyleElementsForCurrentScope()));
        retVal.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.BLOCK_DATA_PROPERTY_EXTERNAL_STYLES.getValue(),
                   Lists.newArrayList(htmlTemplate.getExternalStyleElementsForCurrentScope()));
        retVal.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.BLOCK_DATA_PROPERTY_INLINE_SCRIPTS.getValue(), Lists.newArrayList(htmlTemplate.getInlineScriptElementsForCurrentScope()));
        retVal.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.BLOCK_DATA_PROPERTY_EXTERNAL_SCRIPTS.getValue(),
                   Lists.newArrayList(htmlTemplate.getExternalScriptElementsForCurrentScope()));

        return Response.ok(retVal).build();
    }

    @POST
    @Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    //keep these in sync with the code in the PageRouter
    //Note: security is handled by R.resourceManager().save()
    public Response savePage(@QueryParam(PAGE_URL_PARAM) URI url, String content) throws IOException
    {
        //!!! WARNING: this method is also called directly from the PageRouter
        // if it needs to persist the created page, beware of changes !!!

        //this wraps and parses the raw data coming in
        Source source = new NewPageSource(url, content);

        //pull the source through our template controllers for some last-minute adaptations to the html source
        source = HtmlTemplate.prepareForSave(source);

        //save the file to disk and pull all the proxies etc
        Page savedPage = R.resourceManager().save(source, R.securityManager().getCurrentPerson(), Page.class);

        return Response.ok().build();
    }

    @DELETE
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    //Note that we can't make the uri an URI, because it's incompatible with the client side
    //Note: security is handled by R.resourceManager().delete()
    public Response deletePage(String uri) throws Exception
    {
        Resource page = R.resourceManager().get(URI.create(uri), MimeTypes.HTML);
        if (page == null) {
            throw new NotFoundException("Resource not found; " + uri);
        }

        R.resourceManager().delete(page, R.securityManager().getCurrentPerson(), Page.class);

        return Response.ok().build();
    }

    @DELETE
    @Path("/delete/all")
    @Consumes(MediaType.APPLICATION_JSON)
    //Note that we can't make the uri an URI, because it's incompatible with the client side
    //Note: security is handled by R.resourceManager().delete()
    public Response deletePageAndTranslations(String uri) throws Exception
    {
        Resource page = R.resourceManager().get(URI.create(uri));
        if (page == null) {
            throw new NotFoundException("Resource not found; " + uri);
        }

        R.resourceManager().delete(page, R.securityManager().getCurrentPerson(), Page.class, PageRepository.PageDeleteOption.DELETE_ALL_TRANSLATIONS);

        return Response.ok().build();
    }

    @POST
    @Path("/index")
    @RequiresPermissions(PAGE_REINDEX_ALL_PERM)
    public Response reindex(@QueryParam("url") URI uri) throws Exception
    {
        Response.ResponseBuilder retVal = null;

        synchronized (longRunningThreadLock) {
            if (longRunningThread == null) {
                Page page = R.resourceManager().get(uri, MimeTypes.HTML, Page.class);
                if (page == null) {
                    throw new NotFoundException("Page not found; " + uri);
                }

                //Note: transaction handling is done through the global XA transaction
                //Note: the page index must be indexed first, because it's used to search the translations during triplestore indexing!
                getMainPageIndexer().connect(StorageFactory.getCurrentScopeTx()).update(page);
                getTriplestoreIndexer().connect(StorageFactory.getCurrentScopeTx()).update(page);

                retVal = Response.ok("Index of item successfull; " + uri);
            }
            else {
                retVal =
                                Response.ok("Can't start a single index action because there's a reindexing process running that was launched on " +
                                            ERROR_STAMP_FORMATTER.format(longRunningThread.getStartStamp()));
            }
        }

        return retVal.build();
    }

    @GET
    @Path("/index/all")
    @RequiresPermissions(PAGE_REINDEX_ALL_PERM)
    public Response indexAll(@QueryParam("folder") List<String> folder,
                             @QueryParam("classCurie") List<String> classCurie,
                             @QueryParam("filter") String filter,
                             @QueryParam("depth") Integer depth,
                             @QueryParam("task") String task,
                             @QueryParam("param") List<String> param,
                             @QueryParam("threads") Integer threads)
                    throws Exception
    {
        Response.ResponseBuilder retVal = null;

        //validation
        if (depth == null || depth < 0) {
            depth = -1;
        }
        if (folder == null || folder.isEmpty()) {
            folder = Lists.newArrayList("/");
        }
        if (classCurie == null) {
            classCurie = Lists.newArrayList();
        }

        synchronized (longRunningThreadLock) {
            if (longRunningThread == null) {
                final Map<String, Class<? extends ReindexTask>> taskMappings = ImmutableMap.<String, Class<? extends ReindexTask>>builder()
                                .put("pageReindex", PageReindexTask.class)
                                .put("pageFix", PageFixTask.class)
                                .build();
                Class<? extends ReindexTask> taskClass = null;
                if (task != null) {
                    taskClass = taskMappings.get(task);
                }

                //if we didn't use a shortcut name, try to parse the supplied string to a class
                if (taskClass == null && task != null) {
                    try {
                        taskClass = (Class<? extends ReindexTask>) Class.forName(task);
                    }
                    catch (ClassNotFoundException e) {
                    }
                }

                if (taskClass != null) {

                    //now lookup the classes if we have them
                    boolean allClassesOk = true;
                    Set<RdfClass> rdfClasses = new HashSet<>();
                    for (String c : classCurie) {
                        RdfClass rdfClass = RdfFactory.getClassForResourceType(URI.create(c));
                        if (rdfClass == null) {
                            retVal =
                                            Response.ok("Can't start an index all action because you supplied an unknown 'class' parameter: '" + c +
                                                        "'; please make sure you pass correct RDF class curies for this param.");
                            allClassesOk = false;
                            break;
                        }
                        else {
                            rdfClasses.add(rdfClass);
                        }
                    }

                    if (allClassesOk) {
                        //will register itself in the static variable
                        //Note: the PageRepository is not very kosher, but it works
                        longRunningThread = new ReindexThread(folder, rdfClasses, filter, depth, new PageRepository(), taskClass, param, threads, new ReindexThread.Listener()
                        {
                            @Override
                            public void longRunningThreadStarted()
                            {
                            }
                            @Override
                            public void longRunningThreadEnded()
                            {
                                synchronized (longRunningThreadLock) {
                                    longRunningThread = null;
                                }
                            }
                        });

                        longRunningThread.start();

                        retVal = Response.ok("Launched new reindexation thread with" +
                                             " folder " + Arrays.toString(folder.toArray()) + "," +
                                             " classes " + Arrays.toString(classCurie.toArray()) + "," +
                                             " filter " + filter + "," +
                                             " depth " + depth
                        );
                    }
                }
                else {
                    retVal =
                                    Response.ok("Can't start an index all action because you didn't supply a (correct) 'task' parameter; either pass a full class name or a shortcut string; possible shortcut values are: " +
                                                Joiner.on(", ").join(taskMappings.keySet()) + ".");
                }
            }
            else {
                retVal = Response.ok("Can't start an index all action because there's a reindexing process running that was launched on " +
                                     ERROR_STAMP_FORMATTER.format(longRunningThread.getStartStamp()));
            }
        }

        return retVal.build();
    }

    //    @GET
    //    @Path("/index/clear")
    //    @RequiresPermissions(PAGE_REINDEX_PERMISSION)
    //    public synchronized Response indexClear() throws Exception
    //    {
    //        Response.ResponseBuilder retVal = null;
    //
    //        synchronized (currentIndexAllLock) {
    //        if (currentIndexAllThread == null) {
    //            try {
    //                StorageFactory.getMainPageIndexer().connect(StorageFactory.getCurrentScopeTx()).deleteAll();
    //                StorageFactory.getTriplestoreIndexer().connect(StorageFactory.getCurrentScopeTx()).deleteAll();
    //            }
    //            finally {
    //                //simulate a transaction commit for each action or we'll end up with errors.
    //                //Note: this means every single index call will be atomic, but the entire operation will not,
    //                // so on errors, you'll end up with half-indexed pages and probably errors
    //                //Also note that we need to re-connect for every action or the connection will be closed because of the cleanup
    //                StorageFactory.releaseCurrentRequestTx(false);
    //            }
    //
    //            Logger.info("Index cleared.");
    //
    //            retVal = Response.ok("Index cleared successfully.");
    //        }
    //        else {
    //            retVal = Response.ok("Can't start a clear index action because there's a reindexing process running that was launched on " + ERROR_STAMP_FORMATTER.format(currentIndexAllThread.getStartStamp()));
    //        }
    //        }
    //
    //        return retVal.build();
    //    }

    @GET
    @Path("/index/all/cancel")
    @RequiresPermissions(PAGE_REINDEX_ALL_PERM)
    public Response indexAllCancel() throws Exception
    {
        Response.ResponseBuilder retVal = null;

        synchronized (longRunningThreadLock) {
            if (longRunningThread != null) {
                longRunningThread.cancel();
                retVal = Response.ok("Reindex all process cancellation requested");
            }
            else {
                retVal = Response.ok("Can't cancel a reindex all process because nothing is running at the moment.");
            }
        }

        return retVal.build();
    }

    @GET
    @Path("/index/all/status")
    @RequiresPermissions(PAGE_REINDEX_ALL_PERM)
    public Response indexAllStatus() throws Exception
    {
        Response.ResponseBuilder retVal = null;

        synchronized (longRunningThreadLock) {
            if (longRunningThread != null) {
                retVal = Response.ok("Reindex all process currently running and started at " + ERROR_STAMP_FORMATTER.format(longRunningThread.getStartStamp()));
            }
            else {
                retVal = Response.ok("No reindex all process currently running.");
            }
        }

        return retVal.build();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

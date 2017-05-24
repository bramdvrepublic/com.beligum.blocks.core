package com.beligum.blocks.endpoints;

import com.beligum.base.auth.repositories.PersonRepository;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.Source;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.security.Authentication;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.index.reindex.PageReindexTask;
import com.beligum.blocks.filesystem.index.reindex.ReindexTask;
import com.beligum.blocks.filesystem.index.reindex.ReindexThread;
import com.beligum.blocks.filesystem.pages.PageFixTask;
import com.beligum.blocks.filesystem.pages.PageRepository;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.sources.NewPageSource;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.utils.comparators.MapComparator;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import gen.com.beligum.blocks.core.fs.html.views.modals.new_block;
import gen.com.beligum.blocks.core.messages.blocks.core;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.beligum.blocks.config.StorageFactory.getMainPageIndexer;
import static com.beligum.blocks.config.StorageFactory.getTriplestoreIndexer;
import static gen.com.beligum.base.core.constants.base.core.ADMIN_ROLE_NAME;
import static gen.com.beligum.blocks.core.constants.blocks.core.*;

/**
 * Created by bram on 2/10/16.
 */
@Path("/blocks/admin/page")
@RequiresRoles(ADMIN_ROLE_NAME)
public class PageAdminEndpoint
{
    //-----CONSTANTS-----
    private static Format ERROR_STAMP_FORMATTER = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

    //-----VARIABLES-----
    private static Object currentIndexAllLock = new Object();
    private static ReindexThread currentIndexAllThread = null;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static void endAllAsyncTasksNow()
    {
        synchronized (currentIndexAllLock) {
            if (currentIndexAllThread != null) {
                currentIndexAllThread.cancel();
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
     */
    @GET
    @Path("/template")
    @RequiresPermissions(value = { Permissions.PAGE_CREATE_PERMISSION_STRING })
    public Response getPageTemplate(@QueryParam(NEW_PAGE_URL_PARAM) String pageUrl,
                                    @QueryParam(NEW_PAGE_TEMPLATE_PARAM) String pageTemplateName,
                                    @QueryParam(NEW_PAGE_COPY_URL_PARAM) String pageCopyUrl) throws URISyntaxException
    {
        if (StringUtils.isEmpty(pageUrl)) {
            throw new InternalServerErrorException(core.Entries.newPageNoUrlError.toString());
        }
        else {
            boolean processed = false;

            if (!StringUtils.isEmpty(pageTemplateName)) {
                R.cacheManager().getFlashCache().put(CacheKeys.NEW_PAGE_TEMPLATE_NAME.name(), pageTemplateName);
                processed = true;
            }

            if (!StringUtils.isEmpty(pageCopyUrl)) {
                R.cacheManager().getFlashCache().put(CacheKeys.NEW_PAGE_COPY_URL.name(), pageCopyUrl);
                processed = true;
            }

            if (!processed) {
                throw new InternalServerErrorException(core.Entries.newPageNoDataError.toString());
            }

            //redirect to the requested page with the flash cache filled in
            return Response.seeOther(new URI(pageUrl)).build();
        }
    }

    /**
     * When a new block is created, this is the modal panel that is shown on drop:
     * a list of currently registered block-types in the system.
     */
    @GET
    @Path("/blocks")
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public Response getBlocks()
    {
        TemplateCache cache = TemplateCache.instance();
        List<Map<String, String>> templates = new ArrayList<>();
        for (HtmlTemplate template : cache.values()) {
            if (!(template instanceof PageTemplate) && template.getDisplayType() != HtmlTemplate.MetaDisplayType.HIDDEN) {

                ImmutableMap.Builder<String, String> map = ImmutableMap.<String, String>builder()
                                .put(Entries.NEW_BLOCK_NAME.getValue(), template.getTemplateName())
                                .put(Entries.NEW_BLOCK_TITLE.getValue(), template.getTitle());

                if (!StringUtils.isEmpty(template.getDescription())) {
                    map.put(Entries.NEW_BLOCK_DESCRIPTION.getValue(), template.getDescription());
                }

                if (!StringUtils.isEmpty(template.getIcon())) {
                    map.put(Entries.NEW_BLOCK_ICON.getValue(), template.getIcon());
                }

                templates.add(map.build());
            }
        }

        //sort the blocks by title
        Collections.sort(templates, new MapComparator(gen.com.beligum.blocks.core.constants.blocks.core.Entries.NEW_BLOCK_TITLE.getValue()));

        Template template = new_block.get().getNewTemplate();
        template.set(gen.com.beligum.blocks.core.constants.blocks.core.Entries.NEW_BLOCK_TEMPLATES.getValue(), templates);

        return Response.ok(template).build();
    }

    /**
     * When a user clicks on a block from the above list (getBlocks()), this endpoint is called.
     *
     * @param name the name of the block template to instantiate
     */
    @GET
    @Path("/block/{name:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public Response getBlock(@PathParam("name") String name) throws IOException
    {
        HashMap<String, Object> retVal = new HashMap<>();

        HtmlTemplate htmlTemplate = TemplateCache.instance().getByTagName(name);

        //we drag-and-dropped a block in a certain page and this is the callback to get the html for that block,
        //so it makes sense to try to use the language of that page
        Locale lang = R.i18n().getOptimalRefererLocale();

        //note that we need to force this request to be that language, otherwise, a regular getOptimalLocale() will be used
        R.i18n().setManualLocale(lang);

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

        retVal.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.BLOCK_DATA_PROPERTY_INLINE_STYLES.getValue(),
                   Lists.transform(Lists.newArrayList(htmlTemplate.getInlineStyleElementsForCurrentScope()), Functions.toStringFunction()));
        retVal.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.BLOCK_DATA_PROPERTY_EXTERNAL_STYLES.getValue(),
                   Lists.transform(Lists.newArrayList(htmlTemplate.getExternalStyleElementsForCurrentScope()), Functions.toStringFunction()));
        retVal.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.BLOCK_DATA_PROPERTY_INLINE_SCRIPTS.getValue(),
                   Lists.transform(Lists.newArrayList(htmlTemplate.getInlineScriptElementsForCurrentScope()), Functions.toStringFunction()));
        retVal.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.BLOCK_DATA_PROPERTY_EXTERNAL_SCRIPTS.getValue(),
                   Lists.transform(Lists.newArrayList(htmlTemplate.getExternalScriptElementsForCurrentScope()), Functions.toStringFunction()));

        return Response.ok(retVal).build();
    }

    @POST
    @javax.ws.rs.Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = { Permissions.PAGE_CREATE_PERMISSION_STRING,
                                   Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public Response savePage(@QueryParam("url") URI url, String content) throws Exception
    {
        //this wraps and parses the raw data coming in
        Source source = new NewPageSource(url, content);

        //pull the source through our template controllers for some last-minute adaptations to the html source
        source = HtmlTemplate.prepareForSave(source);

        //save the file to disk and pull all the proxies etc
        Page savedPage = R.resourceManager().save(source, new PersonRepository().get(Authentication.getCurrentPrincipal()), Page.class);

        return Response.ok().build();
    }

    @DELETE
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING,
                                   Permissions.PAGE_DELETE_PERMISSION_STRING })
    //Note that we can't make the uri an URI, because it's incompatible with the client side
    public Response deletePage(String uri) throws Exception
    {
        Resource page = R.resourceManager().get(URI.create(uri), MimeTypes.HTML);
        if (page == null) {
            throw new NotFoundException("Resource not found; " + uri);
        }

        R.resourceManager().delete(page, new PersonRepository().get(Authentication.getCurrentPrincipal()), Page.class);

        return Response.ok().build();
    }

    @DELETE
    @Path("/delete/all")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING,
                                   Permissions.PAGE_DELETE_PERMISSION_STRING })
    //Note that we can't make the uri an URI, because it's incompatible with the client side
    public Response deletePageAndTranslations(String uri) throws Exception
    {
        Resource page = R.resourceManager().get(URI.create(uri));
        if (page == null) {
            throw new NotFoundException("Resource not found; " + uri);
        }

        R.resourceManager().delete(page, new PersonRepository().get(Authentication.getCurrentPrincipal()), Page.class, PageRepository.PageDeleteOption.DELETE_ALL_TRANSLATIONS);

        return Response.ok().build();
    }

    @POST
    @javax.ws.rs.Path("/index")
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public Response reindex(@QueryParam("url") URI uri) throws Exception
    {
        Response.ResponseBuilder retVal = null;

        synchronized (currentIndexAllLock) {
            if (currentIndexAllThread == null) {
                Page page = R.resourceManager().get(uri, MimeTypes.HTML, Page.class);
                if (page == null) {
                    throw new NotFoundException("Page not found; " + uri);
                }

                //Note: transaction handling is done through the global XA transaction
                //Note: the page index must be indexed first, because it's used to search the translations during triplestore indexing!
                getMainPageIndexer().connect(StorageFactory.getCurrentRequestTx()).update(page);
                getTriplestoreIndexer().connect(StorageFactory.getCurrentRequestTx()).update(page);

                retVal = Response.ok("Index of item successfull; " + uri);
            }
            else {
                retVal =
                                Response.ok("Can't start a single index action because there's a reindexing process running that was launched on " +
                                            ERROR_STAMP_FORMATTER.format(currentIndexAllThread.getStartStamp()));
            }
        }

        return retVal.build();
    }

    @GET
    @javax.ws.rs.Path("/index/all")
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public Response indexAll(@QueryParam("folder") List<String> folder, @QueryParam("filter") String filter,
                             @QueryParam("depth") Integer depth, @QueryParam("task") String task)
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

        synchronized (currentIndexAllLock) {
            if (currentIndexAllThread == null) {
                final Map<String, Class<? extends ReindexTask>> taskMappings = ImmutableMap.<String, Class<? extends ReindexTask>>builder()
                                .put("pageReindex", PageReindexTask.class)
                                .put("pageFix", PageFixTask.class)
                                .build();
                Class<? extends ReindexTask> taskClass = null;
                if (task != null) {
                    taskClass = taskMappings.get(task);
                }

                //if we didn't use a shortcut name, try to parse the supplied string to a class
                if (taskClass == null) {
                    try {
                        taskClass = (Class<? extends ReindexTask>) Class.forName(task);
                    }
                    catch (ClassNotFoundException e) {
                    }
                }

                if (taskClass != null) {
                    //will register itself in the static variable
                    //Note: the PageRepository is not very kosher, but it works
                    currentIndexAllThread = new ReindexThread(folder, filter, depth, new PageRepository(), taskClass, new ReindexThread.Listener()
                    {
                        @Override
                        public void reindexingStarted()
                        {
                        }
                        @Override
                        public void reindexingEnded()
                        {
                            synchronized (currentIndexAllLock) {
                                currentIndexAllThread = null;
                            }
                        }
                    });

                    currentIndexAllThread.start();

                    retVal = Response.ok("Launched new reindexation thread with folder " + Arrays.toString(folder.toArray()) + ", filter " + filter +
                                         ", depth " + depth);
                }
                else {
                    retVal = Response.ok("Can't start an index all action because you didn't supply a (correct) 'task' parameter; either pass a full class name or a shortcut string; possible shortcut values are: " +
                                         Joiner.on(", ").join(taskMappings.keySet()) + ".");
                }
            }
            else {
                retVal = Response.ok("Can't start an index all action because there's a reindexing process running that was launched on " +
                                     ERROR_STAMP_FORMATTER.format(currentIndexAllThread.getStartStamp()));
            }
        }

        return retVal.build();
    }

    //    @GET
    //    @javax.ws.rs.Path("/index/clear")
    //    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    //    public synchronized Response indexClear() throws Exception
    //    {
    //        Response.ResponseBuilder retVal = null;
    //
    //        synchronized (currentIndexAllLock) {
    //        if (currentIndexAllThread == null) {
    //            try {
    //                StorageFactory.getMainPageIndexer().connect(StorageFactory.getCurrentRequestTx()).deleteAll();
    //                StorageFactory.getTriplestoreIndexer().connect(StorageFactory.getCurrentRequestTx()).deleteAll();
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
    @javax.ws.rs.Path("/index/all/cancel")
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public Response indexAllCancel() throws Exception
    {
        Response.ResponseBuilder retVal = null;

        synchronized (currentIndexAllLock) {
            if (currentIndexAllThread != null) {
                currentIndexAllThread.cancel();
                retVal = Response.ok("Reindex all process cancelled");
            }
            else {
                retVal = Response.ok("Can't cancel a reindex all process because nothing is running at the moment.");
            }
        }

        return retVal.build();
    }

    @GET
    @javax.ws.rs.Path("/index/all/status")
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public Response indexAllStatus() throws Exception
    {
        Response.ResponseBuilder retVal = null;

        synchronized (currentIndexAllLock) {
            if (currentIndexAllThread != null) {
                retVal = Response.ok("Reindex all process currently running and started at " + ERROR_STAMP_FORMATTER.format(currentIndexAllThread.getStartStamp()));
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

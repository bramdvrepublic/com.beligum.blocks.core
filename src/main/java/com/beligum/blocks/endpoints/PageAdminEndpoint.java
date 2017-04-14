package com.beligum.blocks.endpoints;

import com.beligum.base.auth.repositories.PersonRepository;
import com.beligum.base.resources.DefaultResourceFilter;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.*;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.security.Authentication;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.RdfClassNode;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexConnection;
import com.beligum.blocks.filesystem.pages.PageRepository;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.sources.NewPageSource;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.utils.comparators.MapComparator;
import com.github.dexecutor.core.DefaultDexecutor;
import com.github.dexecutor.core.DexecutorConfig;
import com.github.dexecutor.core.ExecutionConfig;
import com.github.dexecutor.core.support.ThreadPoolUtil;
import com.github.dexecutor.core.task.Task;
import com.github.dexecutor.core.task.TaskProvider;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import gen.com.beligum.blocks.core.fs.html.views.modals.new_block;
import gen.com.beligum.blocks.core.messages.blocks.core;
import net.sf.ehcache.concurrent.Sync;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private static ReindexThread currentIndexAllThread = null;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static void endAllAsyncTasksNow()
    {
        if (currentIndexAllThread != null) {
            currentIndexAllThread.cancel();
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
    public synchronized Response reindex(@QueryParam("url") URI uri) throws Exception
    {
        Response.ResponseBuilder retVal = null;

        if (currentIndexAllThread == null) {
            Page page = R.resourceManager().get(uri, MimeTypes.HTML, Page.class);
            if (page == null) {
                throw new NotFoundException("Page not found; " + uri);
            }

            //Note: transaction handling is done through the global XA transaction
            getMainPageIndexer().connect(StorageFactory.getCurrentRequestTx()).update(page);
            getTriplestoreIndexer().connect(StorageFactory.getCurrentRequestTx()).update(page);

            retVal = Response.ok("Index of item successfull; " + uri);
        }
        else {
            Response.ok("Can't start a single index action because there's a reindexing process running that was launched on " + ERROR_STAMP_FORMATTER.format(currentIndexAllThread.getStartStamp()));
        }

        return retVal.build();
    }

    @GET
    @javax.ws.rs.Path("/index/clear")
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public synchronized Response indexClear() throws Exception
    {
        Response.ResponseBuilder retVal = null;

        if (currentIndexAllThread == null) {
            try {
                StorageFactory.getMainPageIndexer().connect(StorageFactory.getCurrentRequestTx()).deleteAll();
                StorageFactory.getTriplestoreIndexer().connect(StorageFactory.getCurrentRequestTx()).deleteAll();
            }
            finally {
                //simulate a transaction commit for each action or we'll end up with errors.
                //Note: this means every single index call will be atomic, but the entire operation will not,
                // so on errors, you'll end up with half-indexed pages and probably errors
                //Also note that we need to re-connect for every action or the connection will be closed because of the cleanup
                StorageFactory.releaseCurrentRequestTx(false);
            }

            Logger.info("Index cleared.");

            retVal = Response.ok("Index cleared successfully.");
        }
        else {
            Response.ok("Can't start a clear index action because there's a reindexing process running that was launched on " + ERROR_STAMP_FORMATTER.format(currentIndexAllThread.getStartStamp()));
        }

        return retVal.build();
    }

    @GET
    @javax.ws.rs.Path("/index/all")
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public synchronized Response indexAll(@QueryParam("start") Integer start, @QueryParam("size") Integer size,
                                          @QueryParam("folder") List<String> folder, @QueryParam("filter") String filter,
                                          @QueryParam("depth") Integer depth)
                    throws Exception
    {
        Response.ResponseBuilder retVal = null;

        //validation
        if (start == null || start < 0) {
            start = 0;
        }
        if (size == null || size < 0) {
            //signal the logic below to run till the end
            size = -1;
        }
        if (depth == null || depth < 0) {
            depth = -1;
        }
        if (folder == null || folder.isEmpty()) {
            folder = Lists.newArrayList("/");
        }

        if (currentIndexAllThread == null) {
            //will register itself in the static variable
            currentIndexAllThread = new ReindexThread(start, size, folder, filter, depth);
            currentIndexAllThread.start();
            retVal = Response.ok("Launched new reindexation thread with start " + start + ", size " + size + ", folder " + Arrays.toString(folder.toArray()) + ", filter " + filter +
                                 ", depth " + depth);
        }
        else {
            retVal = Response.ok("Can't start an index all action because there's a reindexing process running that was launched on " +
                                 ERROR_STAMP_FORMATTER.format(currentIndexAllThread.getStartStamp()));
        }

        return retVal.build();
    }

    @GET
    @javax.ws.rs.Path("/index/all/cancel")
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public synchronized Response indexAllCancel() throws Exception
    {
        Response.ResponseBuilder retVal = null;

        if (currentIndexAllThread != null) {
            currentIndexAllThread.cancel();
            retVal = Response.ok("Reindex all process cancelled");
        }
        else {
            retVal = Response.ok("Can't cancel a reindex all process because nothing is running at the moment.");
        }

        return retVal.build();
    }

    @GET
    @javax.ws.rs.Path("/index/all/status")
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public synchronized Response indexAllStatus() throws Exception
    {
        Response.ResponseBuilder retVal = null;

        if (currentIndexAllThread != null) {
            retVal = Response.ok("Reindex all process currently running and started at " + ERROR_STAMP_FORMATTER.format(currentIndexAllThread.getStartStamp()));
        }
        else {
            retVal = Response.ok("No reindex all process currently running.");
        }

        return retVal.build();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private static class ReindexThread extends Thread implements TX.Listener, TaskProvider<RdfClassNode, RdfClassNode>
    {
        private final Integer start;
        private final Integer size;
        private final List<String> folders;
        private final String filter;
        private final int depth;
        private long startStamp;
        private boolean cancel;

        private TX transaction;
        private ResourceRepository pageRepository;
        private PageIndexConnection mainPageConn;
        private PageIndexConnection triplestoreConn;
        private ResourceRepository.IndexOption indexConnectionsOption;
        private ExecutorService executorService;

        public ReindexThread(final Integer start, final Integer size, final List<String> folders, final String filter, final int depth)
        {
            this.start = start;
            this.size = size;
            this.folders = folders;
            this.filter = filter;
            this.depth = depth;
            this.startStamp = System.currentTimeMillis();
            //reset a possibly active global cancellation
            this.cancel = false;
        }

        @Override
        public void run()
        {
            try {

                currentIndexAllThread = this;

                //instance a transaction that's connected to this thread
                this.transaction = StorageFactory.getCurrentThreadTx(this, Sync.ONE_DAY);

                //Note: this is not very kosher, but it works
                this.pageRepository = new PageRepository();

                //Note that this means we have one transaction for the entire duration of this thread
                this.mainPageConn = StorageFactory.getMainPageIndexer().connect(this.transaction);
                this.triplestoreConn = StorageFactory.getTriplestoreIndexer().connect(this.transaction);
                this.indexConnectionsOption = new PageRepository.PageIndexConnectionOption(this.mainPageConn, this.triplestoreConn);

                // Dexecutor is a small framework for (asynchronously) executing tasks that depend on each other (and detect cycles).
                // Here, we use it to create a dependency graph of RDF classes that depend on each other and process the
                // 'deepest' dependencies first, because earlier (more shallow) classes will need their dependencies to
                // be present when they get indexed.
                this.executorService = Executors.newFixedThreadPool(ThreadPoolUtil.ioIntesivePoolSize());

                DexecutorConfig<RdfClassNode, RdfClassNode> config = new DexecutorConfig<>(this.executorService, this);
                DefaultDexecutor<RdfClassNode, RdfClassNode> executor = new DefaultDexecutor<>(config);

                //This is a set of all classes that are publicly accessible. Note that this is not the same as all the classes
                // known to the system (eg. the (internal) City won't end up here because it's 'inherited' from an external ontology).
                //It's the list of classes an end-user can select as the 'type' of a page, meaning no other classes can end up being saved
                // on disk, so if we iterate files, all encountered classes should be in this set.
                //We'll sort them based on internal dependency and first index the ones that won't depend
                // on others down the line.
                for (RdfClass rdfClass : RdfFactory.getLocalPublicClasses()) {
                    RdfClassNode node = RdfClassNode.instance(rdfClass);
                    Set<RdfProperty> props = rdfClass.getProperties();

                    boolean addedDep = false;
                    if (props != null) {
                        for (RdfProperty p : props) {
                            if (p.getDataType().getType().equals(RdfClass.Type.CLASS)) {
                                RdfClassNode dep = RdfClassNode.instance(p.getDataType());

                                //link the two together
                                node.addDependency(dep);

                                //this basically means: the indexing of p.getDataType() should finish before the indexing of rdfClass
                                executor.addDependency(dep, node);

                                addedDep = true;
                            }
                        }
                    }

                    //if the class has no dependency on other classes, make sure it gets indexed
                    if (!addedDep) {
                        executor.addIndependent(node);
                    }
                }

                //for debugging: prints out the executor graph
                //                    StringBuilder builder = new StringBuilder();
                //                    executor.print(new LevelOrderTraversar<>(), new StringTraversarAction<>(builder));
                //                    Logger.info(builder.toString());

                //signal the execution should end if an exception is thrown in one of the tasks
                executor.execute(ExecutionConfig.TERMINATING);
            }
            catch (Exception e) {
                Logger.error("Caught exception while executing the reindexation of all pages of this website", e);
            }
            finally {

                try {
                    this.executorService.shutdown();
                    this.executorService.awaitTermination(1, TimeUnit.HOURS);
                }
                catch (Exception e) {
                    Logger.error("Error while shutting down long-running transaction of page reindexation", e);
                }
                finally {
                    this.executorService = null;
                }

                try {
                    StorageFactory.releaseCurrentThreadTx(false);
                }
                catch (Exception e) {
                    Logger.error("Error while ending long-running transaction of page reindexation", e);
                }
                finally {
                    this.transaction = null;
                }

                Logger.info("Reindexing " + (cancel ? "cancelled" : "completed") + " in " +
                            DurationFormatUtils.formatDuration(System.currentTimeMillis() - startStamp, "H:mm:ss") + " time");

                currentIndexAllThread = null;
            }
        }
        @Override
        public Task<RdfClassNode, RdfClassNode> provideTask(RdfClassNode node)
        {
            return new Task<RdfClassNode, RdfClassNode>()
            {
                private final RdfClass rdfClass = node.getRdfClass();
                private int pageCounter = 0;

                public RdfClassNode execute()
                {
                    Logger.info("Iterating the file system to see if we need to reindex " + rdfClass);

                    boolean keepRunning = true;
                    ExecutorService taskExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

                    try {
                        for (String folder : folders) {

                            ResourceFilter pathFilter = null;
                            if (!StringUtils.isEmpty(filter)) {
                                pathFilter = new DefaultResourceFilter(filter);
                            }
                            ResourceIterator pageIterator = pageRepository.getAll(true, URI.create(folder), pathFilter, depth);

                            //note: read-only because we won't be changing the page, only the index
                            while (pageIterator.hasNext() && keepRunning && !cancel) {
                                if (++pageCounter >= start) {

                                    final Page page = pageIterator.next().unwrap(Page.class);

                                    taskExecutor.submit(new Callable<Void>()
                                    {
                                        //synchronize the counter
                                        private final int finalPageCounter = pageCounter;

                                        @Override
                                        public Void call() throws Exception
                                        {
                                            if (!cancel) {
                                                //note that this will read and analyze the html from disk, but it's slightly optimized to only read the necessary first line,
                                                //so it should be relatively fast.
                                                //Watch out: the analyzer will read the normalized file, but we assume it might be broken until we reindexed the page,
                                                //           so we force an analysis of the original html.
                                                if (URI.create(page.createAnalyzer(true).getHtmlTypeof().value).equals(rdfClass.getCurieName())) {
                                                    try {
                                                        Logger.info("Reindexing " + rdfClass + " " + page.getPublicAbsoluteAddress() + " (" + finalPageCounter + ")");
                                                        pageRepository.reindex(page, null, indexConnectionsOption);
                                                    }
                                                    catch (Throwable e) {
                                                        Logger.error("Error while reindexing page " + page.getPublicAbsoluteAddress(), e);
                                                        transaction.setRollbackOnly();
                                                        cancel = true;
                                                    }
                                                }
                                            }

                                            return null;
                                        }
                                    });
                                }

                                keepRunning = !(size != -1 && pageCounter >= size);
                                if (!keepRunning) {
                                    Logger.info("Stopped reindexing because the maximal total size of " + size + " was reached");
                                }
                            }

                            if (!keepRunning || cancel) {
                                break;
                            }
                        }
                    }
                    catch (Throwable e) {
                        Logger.error("Error while executing reindexing task for class " + rdfClass, e);
                    }
                    finally {
                        try {
                            taskExecutor.shutdown();
                            taskExecutor.awaitTermination(1, TimeUnit.HOURS);
                        }
                        catch (InterruptedException e) {
                            Logger.error("Error while shutting down dependency executor service", e);
                        }
                    }

                    return node;
                }
            };
        }
        @Override
        public void transactionTimedOut()
        {
            if (this.transaction != null) {
                try {
                    //doing this will cut short all future reindexation (no need to continue if all will fail at the end anyway)
                    Logger.error("Closing reindexation transaction because of a timeout event");
                    this.transaction.close(true);
                }
                catch (Exception e) {
                    Logger.error("Error while closing transaction after a timeout event", e);
                }
            }
        }
        @Override
        public void transactionStatusChanged(int oldStatus, int newStatus)
        {
            //We're not interested in every single status change
            //Logger.info("TX change from " + Decoder.decodeStatus(oldStatus) + " to " + Decoder.decodeStatus(newStatus));
        }

        public long getStartStamp()
        {
            return startStamp;
        }
        public void cancel()
        {
            this.cancel = true;

            if (this.executorService != null) {
                this.executorService.shutdownNow();
                try {
                    this.executorService.awaitTermination(1, TimeUnit.MINUTES);
                }
                catch (Exception e) {
                    Logger.error("Error while shutting down task executor", e);
                }
            }
        }
    }
}

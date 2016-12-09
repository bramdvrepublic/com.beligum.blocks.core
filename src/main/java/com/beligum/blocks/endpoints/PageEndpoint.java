package com.beligum.blocks.endpoints;

import com.beligum.base.auth.models.Person;
import com.beligum.base.auth.repositories.PersonRepository;
import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.resources.ResourceFactory;
import com.beligum.base.resources.ResourceRequestImpl;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.security.Authentication;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.pages.FullPathGlobFilter;
import com.beligum.blocks.fs.pages.PageIterator;
import com.beligum.blocks.fs.pages.ReadOnlyPage;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.fs.pages.ifaces.PageStore;
import com.beligum.blocks.rdf.sources.HtmlSource;
import com.beligum.blocks.rdf.sources.HtmlStringSource;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.utils.comparators.MapComparator;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import gen.com.beligum.blocks.core.fs.html.views.modals.newblock;
import gen.com.beligum.blocks.core.messages.blocks.core;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.beligum.blocks.config.StorageFactory.*;
import static gen.com.beligum.blocks.core.constants.blocks.core.*;

/**
 * Created by bram on 2/10/16.
 */
@Path("/blocks/admin/page")
@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
public class PageEndpoint
{
    //-----CONSTANTS-----
    //leave some headroom...
    private static ExecutorService TASK_EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() == 1 ? 1 : Runtime.getRuntime().availableProcessors() - 1);

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

        if (TASK_EXECUTOR != null) {
            Logger.warn("Force-shutting down any pending asynchronous tasks...");
            TASK_EXECUTOR.shutdownNow();
            Logger.warn("Force-shutting down any pending asynchronous tasks completed.");
        }
    }
    /**
     * When a new page (a non-existing page) is requested, the (logged-in) user is presented with a list of page templates.
     * This endpoint is called when a page template is selected from that list.
     * Basically, we redirect back to the url where the page has to be created and put the name of the pagetemplate in the flashcache,
     * so the root endpoint (ApplicationEndpoint.getPageNew()) can detect what to do.
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
            throw new InternalServerErrorException(core.Entries.newPageNoUrlError.getI18nValue());
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
                throw new InternalServerErrorException(core.Entries.newPageNoDataError.getI18nValue());
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
        TemplateCache cache = HtmlParser.getTemplateCache();
        List<Map<String, String>> templates = new ArrayList<>();
        Locale browserLang = I18nFactory.instance().getOptimalLocale();
        for (HtmlTemplate template : cache.values()) {
            if (!(template instanceof PageTemplate) && template.getDisplayType() != HtmlTemplate.MetaDisplayType.HIDDEN) {
                HashMap<String, String> pageTemplate = new HashMap();

                //the order of locales in which the templates will be searched
                final Locale[] LANGS = { browserLang, R.configuration().getDefaultLanguage(), Locale.ROOT };
                pageTemplate.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.NEW_BLOCK_NAME.getValue(), template.getTemplateName());
                pageTemplate.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.NEW_BLOCK_TITLE.getValue(),
                                 this.findI18NValue(LANGS, template.getTitles(), core.Entries.emptyTemplateTitle.getI18nValue()));
                pageTemplate.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.NEW_BLOCK_DESCRIPTION.getValue(),
                                 this.findI18NValue(LANGS, template.getDescriptions(), core.Entries.emptyTemplateDescription.getI18nValue()));
                pageTemplate.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.NEW_BLOCK_ICON.getValue(), this.findI18NValue(LANGS, template.getIcons(), null));
                templates.add(pageTemplate);
            }
        }

        //sort the blocks by title
        Collections.sort(templates, new MapComparator(gen.com.beligum.blocks.core.constants.blocks.core.Entries.NEW_BLOCK_TITLE.getValue()));

        Template template = newblock.get().getNewTemplate();
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

        HtmlTemplate htmlTemplate = null;
        for (HtmlTemplate t : HtmlParser.getTemplateCache().values()) {
            if (t.getTemplateName().equals(name)) {
                htmlTemplate = t;
                break;
            }
        }

        //we'll render out a block template to a temp buffer (note: you never have to close a stringwriter)
        StringWriter blockHtml = new StringWriter();
        // Warning: tag templates are stored/searched in the cache by their relative path (eg. see TemplateCache.putByRelativePath()),
        // so make sure you don't use that key to create this resource or you'll re-create the template, instead of an instance.
        // To avoid any clashes, we'll use the name of the instance as resource URI
        Template block = R.templateEngine().getNewTemplate(new ResourceRequestImpl(URI.create(htmlTemplate.getTemplateName()), Resource.MimeType.HTML), htmlTemplate.createNewHtmlInstance());
        block.render(blockHtml);

        retVal.put(gen.com.beligum.blocks.core.constants.blocks.core.Entries.BLOCK_DATA_PROPERTY_HTML.getValue(), blockHtml.toString());
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
        try {
            //for safety and uniformity
            url = this.preprocessUri(url);

            //this just wraps the raw data coming in
            HtmlSource source = new HtmlStringSource(url, content);

            //save the file to disk and pull all the proxies etc
            Page savedPage = getPageStore().save(source, new PersonRepository().get(Authentication.getCurrentPrincipal()));

            //above method returns null if nothing changed
            if (savedPage != null) {
                //Note: transaction handling is done through the global XA transaction
                getMainPageIndexer().connect().update(savedPage);
                getTriplestoreIndexer().connect().update(savedPage);

                //make sure we evict all possible cached values (mainly in production)
                R.resourceFactory().wipeCacheFor(savedPage.getPublicAbsoluteAddress());
                R.resourceFactory().wipeCacheFor(savedPage.getPublicRelativeAddress());
            }
        }
        //TODO this is just for debugging weird issues -> throw away when found
        catch (Throwable e) {
            final String delim = "---------------------------------------------------";
            Logger.error("Caught error (and explicitly logging it) while saving page; "+url+"\n"+delim+"\n"+content+"\n"+delim, e);
            throw e;
        }

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
        this.doDeletePage(getPageStore(),
                          getMainPageIndexer().connect(),
                          getTriplestoreIndexer().connect(),
                          new PersonRepository().get(Authentication.getCurrentPrincipal()),
                          R.resourceFactory(),
                          new ReadOnlyPage(URI.create(uri)));

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
        PageStore pageStore = getPageStore();
        PageIndexConnection mainPageIndexer = getMainPageIndexer().connect();
        PageIndexConnection triplestoreIndexer = getTriplestoreIndexer().connect();
        Person currentPrincipal = new PersonRepository().get(Authentication.getCurrentPrincipal());

        Page page = new ReadOnlyPage(URI.create(uri));

        //first, delete the translations, then delete the first one
        for (Map.Entry<Locale, Page> e : page.getTranslations().entrySet()) {
            this.doDeletePage(pageStore, mainPageIndexer, triplestoreIndexer, currentPrincipal, R.resourceFactory(), e.getValue());
        }

        this.doDeletePage(pageStore, mainPageIndexer, triplestoreIndexer, currentPrincipal, R.resourceFactory(), page);

        return Response.ok().build();
    }

    @POST
    @javax.ws.rs.Path("/index")
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public synchronized Response reindex(@QueryParam("url") URI url) throws Exception
    {
        Response.ResponseBuilder retVal = null;

        if (currentIndexAllThread == null) {
            //for safety and uniformity
            url = this.preprocessUri(url);

            //note: read-only because we won't be changing the page, only the index
            Page savedPage = getPageStore().get(url, true);

            //above method returns null if nothing changed (so nothing to re-index)
            if (savedPage != null) {
                //Note: transaction handling is done through the global XA transaction
                getMainPageIndexer().connect().update(savedPage);
                getTriplestoreIndexer().connect().update(savedPage);
            }

            retVal = Response.ok("Index of item successfull; " + url);
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
                StorageFactory.getMainPageIndexer().connect().deleteAll();
                StorageFactory.getTriplestoreIndexer().connect().deleteAll();
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
    public synchronized Response indexAll(@QueryParam("start") Integer start, @QueryParam("size") Integer size, @QueryParam("folder") List<String> folder,
                                          @QueryParam("filter") String filter, @QueryParam("depth") Integer depth)
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

        if (currentIndexAllThread == null) {
            //will register itself in the static variable
            new ReindexThread(start, size, folder, filter, depth).start();
            retVal = Response.ok("Launched new reindexation thread with start " + start + ", size " + size + ", folder " + Arrays.toString(folder.toArray()) + ", filter " + filter + ", depth " + depth);
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
    private URI preprocessUri(URI url)
    {
        //avoid directory attacks
        url = url.normalize();

        //note that we need an absolute URI (eg. to have a correct root RDF context for Sesame),
        // but we allow for relative URIs to be imported -> just make them absolute based on the current settings
        if (!url.isAbsolute()) {
            url = R.configuration().getSiteDomain().resolve(url);
        }

        //Note: domain checking is done in the AbstractPage constructor later on

        return url;
    }
    private String findI18NValue(Locale[] langs, Map<Locale, String> values, String defaultValue)
    {
        String retVal = null;

        if (!values.isEmpty()) {
            for (Locale l : langs) {
                retVal = values.get(l);

                if (retVal != null) {
                    break;
                }
            }

            if (retVal == null) {
                retVal = values.values().iterator().next();
            }
        }

        if (retVal == null) {
            retVal = defaultValue;
        }

        return retVal;
    }
    private void doDeletePage(PageStore pageStore, PageIndexConnection mainPageIndexer, PageIndexConnection triplestoreIndexer, Person currentPrincipal, ResourceFactory resourceFactory, Page page) throws IOException
    {
        //fist delete the page on disk, then delete all indexes
        Page deletedPage = pageStore.delete(page.getPublicAbsoluteAddress(), currentPrincipal);

        //above method returns null if nothing changed
        if (deletedPage != null) {
            mainPageIndexer.delete(deletedPage);
            triplestoreIndexer.delete(deletedPage);

            //make sure we evict all possible cached values (mainly in production)
            resourceFactory.wipeCacheFor(deletedPage.getPublicAbsoluteAddress());
            resourceFactory.wipeCacheFor(deletedPage.getPublicRelativeAddress());
        }
    }

    private static class ReindexThread extends Thread
    {
        private final Integer start;
        private final Integer size;
        private final List<String> folders;
        private final String filter;
        private final int depth;
        private long startStamp;
        private boolean cancel;

        private PageIterator pageIterator;

        public ReindexThread(final Integer start, final Integer size, final List<String> folders, final String filter, final int depth)
        {
            this.start = start;
            this.size = size;
            this.folders = folders;
            this.filter = filter;
            this.depth = depth;

            this.startStamp = new Date().getTime();
            //reset a possibly active global cancellation
            this.cancel = false;
        }

        @Override
        public void run()
        {
            Logger.info("Launching reindexation with start " + start + ", size " + size + ", folders " + Arrays.toString(folders.toArray()) + ", filter " + filter + ", depth " + depth);

            try {
                currentIndexAllThread = this;

                //sleep a little bit to make sure the request context is gone
                //(especially for the quirky transaction handling)
                this.sleep(5000);

                this.execute();
            }
            catch (Exception e) {
                Logger.error("Caught exception while executing the reindexation of all pages of this website", e);
            }
            finally {
                //good place to (asynchronously) wipe the static variable
                currentIndexAllThread = null;
            }
        }

        public long getStartStamp()
        {
            return startStamp;
        }
        public void cancel()
        {
            this.cancel = true;

            //might be stuck in a deep filter loop, this allows us to cancel immediately
            if (this.pageIterator != null) {
                this.pageIterator.cancel();
            }
        }
        private void execute() throws IOException, InterruptedException
        {
            long startStamp = System.currentTimeMillis();
            //little trick to have a final that's not so final
            final int[] generalCounter = { 0 };
            //only counts items really processed
            int pageCounter = 0;
            boolean keepRunning = true;

            for (String folder : this.folders) {
                Logger.info("Entering next folder "+folder);

                FullPathGlobFilter pathFilter = null;
                if (!StringUtils.isEmpty(filter)) {
                    pathFilter = new FullPathGlobFilter(filter);
                }
                this.pageIterator = getPageStore().getAll(true, folder, pathFilter, depth);

                //note: read-only because we won't be changing the page, only the index
                while (pageIterator.hasNext() && keepRunning && !cancel) {
                    final Page page = pageIterator.next();

                    if (generalCounter[0] >= start) {
                        pageCounter++;

                        //TODO the async code is buggy (indexation commits are not synchronized and they overflow)
                        //                        TASK_EXECUTOR.submit(new Callable<Void>()
                        //                        {
                        //                            @Override
                        //                            public Void call() throws Exception
                        //                            {
                        boolean completed = false;
                        while (!completed) {

                            //Note that both need a seperate try-catch because the NotIndexedException case
                            // should be committed before the first one is tried again
                            try {
                                PageIndexConnection mainPageConn = StorageFactory.getMainPageIndexer().connect();
                                PageIndexConnection triplestoreConn = StorageFactory.getTriplestoreIndexer().connect();

                                Logger.info("Reindexing " + page.getPublicAbsoluteAddress() + " (" + pageCounter + "," + generalCounter[0] + ")");
                                mainPageConn.update(page);
                                triplestoreConn.update(page);
                                generalCounter[0]++;

                                //if no NotIndexedException was thrown, we can safely mark the indexation as completed
                                completed = true;
                            }
                            finally {
                                //see indexClear()
                                StorageFactory.releaseCurrentRequestTx(false);
                            }
                        }

                        //return null;
                        //}
                        //});
                    }
                    else {
                        generalCounter[0]++;
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

            //TASK_EXECUTOR.awaitTermination(1, TimeUnit.HOURS);

            Logger.info("Reindexing " + (cancel ? "cancelled" : "completed") + "; processed " + generalCounter[0] + " items in " +
                        DurationFormatUtils.formatDuration(System.currentTimeMillis() - startStamp, "H:mm:ss") + " time");
        }
    }
}

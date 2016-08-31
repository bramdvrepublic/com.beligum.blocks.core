package com.beligum.blocks.endpoints;

import com.beligum.base.auth.repositories.PersonRepository;
import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.resources.ResourceRequestImpl;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.security.Authentication;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.exceptions.NotIndexedException;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.pages.ifaces.Page;
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
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.beligum.blocks.config.StorageFactory.*;

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

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static void endAllAsyncTasksNow()
    {
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
    public Response getPageTemplate(@QueryParam("page_url") String pageUrl,
                                    @QueryParam("page_class_name") String pageTemplateName) throws URISyntaxException
    {
        if (StringUtils.isEmpty(pageUrl)) {
            throw new InternalServerErrorException(core.Entries.newPageNoUrlError.getI18nValue());
        }
        if (StringUtils.isEmpty(pageTemplateName)) {
            throw new InternalServerErrorException(core.Entries.newPageNoTemplatenameError.getI18nValue());
        }
        PageTemplate pageTemplate = (PageTemplate) HtmlParser.getTemplateCache().getByTagName(pageTemplateName);
        if (pageTemplate == null) {
            throw new InternalServerErrorException(core.Entries.newPageUnknownTemplateError.getI18nValue());
        }

        R.cacheManager().getFlashCache().put(CacheKeys.NEW_PAGE_TEMPLATE_NAME.name(), pageTemplateName);

        //redirect to the requested page with the flash cache filled in
        return Response.seeOther(new URI(pageUrl)).build();
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
        //for safety and uniformity
        url = this.preprocessUri(url);

        //this just wraps the raw data coming in
        HtmlSource source = new HtmlStringSource(url, content);

        //save the file to disk and pull all the proxies etc
        Page savedPage = getPageStore().save(source, new PersonRepository().get(Authentication.getCurrentPrincipal()));

        //above method returns null if nothing changed (so nothing to re-index)
        if (savedPage != null) {
            //Note: transaction handling is done through the global XA transaction
            getMainPageIndexer().connect().update(savedPage);
            getTriplestoreIndexer().connect().update(savedPage);
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
        //save the file to disk and pull all the proxies etc
        Page deletedPage = getPageStore().delete(URI.create(uri), new PersonRepository().get(Authentication.getCurrentPrincipal()));

        if (deletedPage != null) {
            getMainPageIndexer().connect().delete(deletedPage);
            getTriplestoreIndexer().connect().delete(deletedPage);
        }

        return Response.ok().build();
    }

    @POST
    @javax.ws.rs.Path("/reindex")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public Response reindex(@QueryParam("url") URI url) throws Exception
    {
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

        return Response.ok().build();
    }

    @GET
    @javax.ws.rs.Path("/reindex/all")
    @RequiresPermissions(value = { Permissions.PAGE_MODIFY_PERMISSION_STRING })
    public Response reindexAll(@Suspended final AsyncResponse asyncResponse, @QueryParam("start") Integer start, @QueryParam("size") Integer size) throws Exception
    {
        //validation
        if (start == null || start < 0) {
            start = 0;
        }
        if (size == null || size < 0) {
            //signal the logic below to run till the end
            size = -1;
        }

        Logger.info("Launching reindexation with start index " + start + " and total size " + size);

        final Integer finalSize = size;
        final Integer finalStart = start;
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    asyncResponse.resume(this.doReindexAll());
                }
                catch (Exception e) {
                    Logger.error("Caught exception while executing the reindexation of all pages of this website", e);
                }
            }

            private Response doReindexAll() throws IOException, InterruptedException
            {
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

                long startStamp = System.currentTimeMillis();
                //little trick to have a final that's not so final
                final int[] generalCounter = { 0 };
                //only counts items really processed
                int launchedThreads = 0;
                boolean keepRunning = true;
                //note: read-only because we won't be changing the page, only the index
                Iterator<Page> allPages = getPageStore().getAll(true);
                while (allPages.hasNext() && keepRunning) {
                    final Page page = allPages.next();

                    if (generalCounter[0] >= finalStart) {
                        launchedThreads++;
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
                                        try {
                                            PageIndexConnection mainPageConn = StorageFactory.getMainPageIndexer().connect();
                                            PageIndexConnection triplestoreConn = StorageFactory.getTriplestoreIndexer().connect();

                                            Logger.info("Reindexing " + page.getPublicAbsoluteAddress());
                                            mainPageConn.update(page);
                                            triplestoreConn.update(page);
                                            generalCounter[0]++;

                                            //if no NotIndexedException was thrown, we can safely mark the indexation as completed
                                            completed = true;
                                        }
                                        finally {
                                            //see above
                                            StorageFactory.releaseCurrentRequestTx(false);
                                        }
                                    }
                                    catch (NotIndexedException e) {
                                        try {
                                            PageIndexConnection mainPageConn = StorageFactory.getMainPageIndexer().connect();
                                            PageIndexConnection triplestoreConn = StorageFactory.getTriplestoreIndexer().connect();

                                            Page pageToIndexFirst = StorageFactory.getPageStore().get(e.getResourceNeedingIndexation(), true);
                                            Logger.info("Reindexing parent " + pageToIndexFirst.getPublicAbsoluteAddress());
                                            mainPageConn.update(pageToIndexFirst);
                                            triplestoreConn.update(pageToIndexFirst);
                                            generalCounter[0]++;
                                        }
                                        finally {
                                            //see above
                                            StorageFactory.releaseCurrentRequestTx(false);
                                        }
                                    }
                                }

                                //return null;
                            //}
                        //});
                    }
                    else {
                        generalCounter[0]++;
                    }

                    keepRunning = !(finalSize != -1 && launchedThreads >= finalSize);
                    if (!keepRunning) {
                        Logger.info("Stopped reindexing because the maximal total size of " + finalSize + " was reached");
                    }
                }

                //TASK_EXECUTOR.awaitTermination(1, TimeUnit.HOURS);

                Logger.info("Reindexing completed; processed " + generalCounter[0] + " items in " + DurationFormatUtils.formatDuration(System.currentTimeMillis() - startStamp, "H:mm:ss") + " seconds");

                return Response.ok().build();
            }
        }).start();

        return null;
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
}

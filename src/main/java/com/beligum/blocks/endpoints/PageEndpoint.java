package com.beligum.blocks.endpoints;

/**
 * Created by bas on 07.10.14.
 */

import com.beligum.base.auth.repositories.PersonRepository;
import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.security.Authentication;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.caching.PageCache;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.controllers.PersistenceControllerImpl;
import com.beligum.blocks.fs.indexes.ElasticPageIndex;
import com.beligum.blocks.fs.indexes.ifaces.PageIndex;
import com.beligum.blocks.fs.pages.SimplePageStore;
import com.beligum.blocks.fs.pages.WebPageParser;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.fs.pages.ifaces.PageStore;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.rdf.exporters.JenaExporter;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.rdf.importers.SesameImporter;
import com.beligum.blocks.rdf.sources.HtmlSource;
import com.beligum.blocks.rdf.sources.HtmlStreamSource;
import com.beligum.blocks.rdf.sources.HtmlStringSource;
import com.beligum.blocks.routing.Route;
import com.beligum.blocks.search.ElasticSearch;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.utils.comparators.MapComparator;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.Model;
import gen.com.beligum.blocks.core.fs.html.views.modals.newblock;
import gen.com.beligum.blocks.core.messages.blocks.core;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.SourceFormatter;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.hibernate.validator.constraints.NotBlank;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.filesystem.exceptions.XAApplicationException;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

import javax.transaction.xa.XAResource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;

@Path("/blocks/admin/page")
@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
public class PageEndpoint
{
    //-----CONSTANTS-----
    public static final String PAGE_TEMPLATE_NAME = "pageTemplateName";

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * Redirect back to the url where the page has to be created
     * We put the name of the pagetemplate in the flashcache
     */
    @GET
    @Path("/template")
    public Response getPageTemplate(
                    @QueryParam("page_url")
                    @NotBlank(message = "No url specified.")
                    String pageUrl,
                    @QueryParam("page_class_name")
                    @NotBlank(message = "No entity-class specified.")
                    String pageTemplateName)
                    throws Exception

    {
        PageTemplate pageTemplate = (PageTemplate) HtmlParser.getTemplateCache().get(pageTemplateName);
        R.cacheManager().getFlashCache().put(PAGE_TEMPLATE_NAME, pageTemplateName);
        return Response.seeOther(new URI(pageUrl)).build();
    }

    @POST
    @javax.ws.rs.Path("/save-new/{url:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response savePageNew(@PathParam("url") URI uri, String content) throws Exception
    {
        PersonRepository personRepository = new PersonRepository();

        //Note: the true flag: compacting helps minimizing the whitespace of the JSONLD properties
        HtmlSource source = new HtmlStringSource(content, uri, true);

        //save the file to disk and pull all the proxies etc
        //TODO this should probably honour our watch system and just write the HTML, no?
        Page savedPage = this.getPageStore().save(source, personRepository.get(Authentication.getCurrentPrincipal()));

        this.getElasticPageIndex().indexPage(savedPage);

        return Response.ok().build();
    }
    private String useOldProcessor(URI uri, String content) throws Exception
    {
        Route route = new Route(uri, PersistenceControllerImpl.instance());
        if (!route.exists()) {
            route.create();
        }
        URI blockId = route.getWebPath().getBlockId();
        WebPage localizedWebpage = ResourceFactoryImpl.instance().createWebPage(blockId, route.getLocale());
        WebPageParser pageParser = new WebPageParser(uri, localizedWebpage.getLanguage(), content, PersistenceControllerImpl.instance());

        String wouter = "<" + pageParser.getPageTemplate() + " class=\"\">" + pageParser.getParsedHtml() + "</" + pageParser.getPageTemplate() + ">";

        //
        //        SourceFormatter formatter = new SourceFormatter(new Source(wouter));
        //        formatter.setCollapseWhiteSpace(true);
        //        formatter.setIndentString("    ");
        //        formatter.setNewLine("\n");
        //        wouter = formatter.toString();

        return wouter;
    }
    private void testRdfaParsing() throws Exception
    {
        String testFile = "/home/bram/Projects/Workspace/idea/com.beligum.mot.site/src/test/resources/belgium-aalst-nieuwerkerken-blauwenbergstraat-61.html";
        String outFile = "/home/bram/Projects/Workspace/idea/com.beligum.mot.site/src/test/resources/testsave_raw.json";
        String baseUrl = "http://mot.beligum.com/v1/resource/waterwell/belgium-aalst-nieuwerkerken-blauwenbergstraat-61";
        //String baseUrl = "http://mot.beligum.com/nl/v1/resource/waterwell/belgium-aalst-nieuwerkerken-blauwenbergstraat-61";

        HtmlSource source = new HtmlStreamSource(new File(testFile).toURI(), URI.create(baseUrl));
        Model model = new SesameImporter(Importer.Format.RDFA).importDocument(source);

        try (OutputStream out = new FileOutputStream(new File(outFile))) {
            new JenaExporter(Exporter.Format.JSONLD).exportModel(model, out);
        }
    }
    private void preparseHtml(String html)
    {
        Source source = new Source(html);
        SourceFormatter formatter = new SourceFormatter(source);
        formatter.setCollapseWhiteSpace(true);
        //removes all indentation
        formatter.setIndentString("");
        //removes all newlines
        formatter.setNewLine("");
        //result is a very compact format
        String finalHtmlFormatted = formatter.toString();
    }

    private void testTransaction()
    {
        String xadiskSystemDirectory = "C:\\xadisk";
        File sampleDataDir1 = new File("C:\\data1");
        XAFileSystem xafs = null;

        try {
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(xadiskSystemDirectory, "id-1");

            xafs = XAFileSystemProxy.bootNativeXAFileSystem(configuration);

            System.out.println("\nBooting XADisk...\n");

            xafs.waitForBootup(-1);

            System.out.println("\nXADisk is now available for use.\n");

            Session session = xafs.createSessionForLocalTransaction();

            XASession sessionXA = xafs.createSessionForXATransaction();

            XAResource res = sessionXA.getXAResource();

            sessionXA.deleteFile(null);

            try {
                session.createFile(sampleDataDir1, true);
                session.createFile(new File(sampleDataDir1, "a.txt"), false);

                session.commit();

                System.out.println("\nCongratulations! You have successfully run the XADisk-Hello-World.\n");
            } catch (XAApplicationException xaae) {
                session.rollback();
                throw xaae;
            }

        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (xafs != null) {
                try {
                    xafs.shutdown();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    @POST
    @Path("/save/{url:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    // @bulk:  if true, we have to flush the bulk upload to ElasticSearch (used during import)
    public Response savePageOld(@PathParam("url") String url, @QueryParam("bulk") @DefaultValue("false") boolean bulk, String content) throws Exception
    {
        URI uri = new URI(url);
        // Analyze the url to find the correct Route
        Route route = new Route(uri, PersistenceControllerImpl.instance());
        boolean doVersion = false;

        if (!route.exists()) {
            route.create();
        }

        URI blockId = route.getWebPath().getBlockId();

        if (route.getWebPath().isNotFound()) {
            route.getWebPath().setPageOk(blockId);
            PersistenceControllerImpl.instance().savePath(route.getWebPath());

        }
        else if (route.getWebPath().isRedirect()) {
            //TODO get path to redirect
        }

        // fetch page for locale
        WebPage localizedWebpage = PersistenceControllerImpl.instance().getWebPage(blockId, route.getLocale());
        // if this page does not yet exist -> create
        if (localizedWebpage == null) {
            localizedWebpage = ResourceFactoryImpl.instance().createWebPage(blockId, route.getLocale());
        }
        else {
            doVersion = true;
        }

        //        Parse html:
        //        1. get text
        //        2. get filtered html
        //        3. get resources - update new resources with resource-tag
        //        4. get href and src attributes
        WebPageParser oldPageParser = new WebPageParser(uri, localizedWebpage.getLanguage(), localizedWebpage.getParsedHtml(false), PersistenceControllerImpl.instance());
        WebPageParser pageParser = new WebPageParser(uri, localizedWebpage.getLanguage(), content, PersistenceControllerImpl.instance());
        boolean saveNoMatterWhatForDebug = true;
        if (saveNoMatterWhatForDebug || !(pageParser.getParsedHtml().equals(localizedWebpage.getParsedHtml(true)))) {
            localizedWebpage.setPageTemplate(pageParser.getPageTemplate());
            localizedWebpage.setParsedHtml(pageParser.getParsedHtml());
            localizedWebpage.setText(pageParser.getText());
            localizedWebpage.setLinks(pageParser.getLinks());
            localizedWebpage.setResources(pageParser.getResources().keySet());
            localizedWebpage.setTemplates(pageParser.getTemplates());
            localizedWebpage.setPageTitle(pageParser.getPageTitle());

            // Put all found property values inside the resources'
            // return the resources that were changed
            WebPageParser.fillResourceProperties(pageParser.getResources(), pageParser.getResourceProperties(), oldPageParser.getResourceProperties(), PersistenceControllerImpl.instance(),
                                                 localizedWebpage.getLanguage());

            // TODO set webpage properties

            for (Resource resource : pageParser.getResources().values()) {
                if (!resource.getBlockId().equals(pageParser.getPageResource().getBlockId())) {
                    PersistenceControllerImpl.instance().saveResource(resource);
                }
            }

            // Add all the root properties on the page to the webpage
            for (URI field : pageParser.getPageResource().getFields()) {
                if (!pageParser.getPageResource().get(field, Locale.ROOT).isNull()) {
                    localizedWebpage.set(field, pageParser.getPageResource().get(field));
                }
                else if (!pageParser.getPageResource().get(field).isNull()) {
                    localizedWebpage.set(field, pageParser.getPageResource().get(field));
                }
            }

            // TODO update other pages that contain changed resources

            PersistenceControllerImpl.instance().saveWebPage(localizedWebpage, doVersion);
        }

        if (PageCache.isEnabled()) {
            PageCache.instance().flush();
        }

        // When we are importing in bulk, we do not save the bulk but wait for the user to do it
        // otherwise we now persist all changes from our request to elastic search
        if (!bulk) {
            ElasticSearch.instance().saveBulk();
        }

        return Response.ok().build();
    }

    @GET
    @Path("/blocks")
    public Response getBlocks()
    {
        TemplateCache cache = HtmlParser.getTemplateCache();
        List<Map<String, String>> templates = new ArrayList<>();
        Locale browserLang = I18nFactory.instance().getOptimalLocale();
        for (HtmlTemplate template : cache.values()) {
            if (!(template instanceof PageTemplate) && template.getDisplayType() != HtmlTemplate.MetaDisplayType.HIDDEN) {
                HashMap<String, String> pageTemplate = new HashMap();

                //the order of locales in which the templates will be searched
                final Locale[] LANGS = { browserLang, Settings.instance().getDefaultLanguage(), Locale.ROOT };
                pageTemplate.put("name", template.getTemplateName());
                pageTemplate.put("title", this.findI18NValue(LANGS, template.getTitles(), core.Entries.emptyTemplateTitle.getI18nValue()));
                pageTemplate.put("description", this.findI18NValue(LANGS, template.getDescriptions(), core.Entries.emptyTemplateDescription.getI18nValue()));
                pageTemplate.put("icon", this.findI18NValue(LANGS, template.getIcons(), null));
                templates.add(pageTemplate);
            }
        }

        //sort the blocks by title
        Collections.sort(templates, new MapComparator("title"));

        Template template = newblock.get().getNewTemplate();
        template.set("templates", templates);

        return Response.ok(template.render()).build();
    }

    @GET
    @Path("/block/{name:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBlock(@PathParam("name") String name)
    {
        HashMap<String, Object> retVal = new HashMap<>();

        HtmlTemplate htmlTemplate = null;
        for (HtmlTemplate t : HtmlParser.getTemplateCache().values()) {
            if (t.getTemplateName().equals(name)) {
                htmlTemplate = t;
                break;
            }
        }

        Template block = R.templateEngine().getNewStringTemplate(htmlTemplate.createNewHtmlInstance());
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

    @DELETE
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePage(String url) throws Exception
    {
        URI uri = new URI(url);
        Route route = new Route(uri, PersistenceControllerImpl.instance());
        URI masterPage = route.getWebPath().getBlockId();
        PersistenceControllerImpl.instance().deleteWebPage(masterPage);
        if (PageCache.isEnabled()) {
            PageCache.instance().flush();
        }
        return Response.ok().build();
    }

    //-----PRIVATE METHODS-----
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
    public PageStore getPageStore() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGE_STORE)) {
            PageStore pageStore = new SimplePageStore();
            pageStore.init();

            R.cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGE_STORE, pageStore);
        }

        return (PageStore) R.cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGE_STORE);
    }
    public PageIndex getElasticPageIndex() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.ELASTIC_PAGE_INDEX)) {
            R.cacheManager().getApplicationCache().put(CacheKeys.ELASTIC_PAGE_INDEX, new ElasticPageIndex());
        }

        return (PageIndex) R.cacheManager().getApplicationCache().get(CacheKeys.ELASTIC_PAGE_INDEX);
    }
}

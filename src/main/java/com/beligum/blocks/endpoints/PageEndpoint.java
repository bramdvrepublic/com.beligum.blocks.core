package com.beligum.blocks.endpoints;

/**
 * Created by bas on 07.10.14.
 */

import com.beligum.base.auth.repositories.PersonRepository;
import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.caching.PageCache;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.controllers.PersistenceControllerImpl;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.pages.HdfsPageStore;
import com.beligum.blocks.pages.WebPageParser;
import com.beligum.blocks.pages.ifaces.PageStore;
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
import com.hp.hpl.jena.rdf.model.*;
import gen.com.beligum.blocks.core.fs.html.views.modals.newblock;
import gen.com.beligum.blocks.core.messages.blocks.core;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.SourceFormatter;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.out.JsonLDWriter;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.hibernate.validator.constraints.NotBlank;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.semarglproject.jena.rdf.rdfa.JenaRdfaReader;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
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
    @javax.ws.rs.Path("/save/{url:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response savePageNew(@PathParam("url") URI uri, String content) throws Exception
    {
        PersonRepository personRepository = new PersonRepository();

        Route route = new Route(uri, PersistenceControllerImpl.instance());
        if (!route.exists()) {
            route.create();
        }
        URI blockId = route.getWebPath().getBlockId();
        WebPage localizedWebpage = ResourceFactoryImpl.instance().createWebPage(blockId, route.getLocale());
        WebPageParser pageParser = new WebPageParser(uri, localizedWebpage.getLanguage(), content, PersistenceControllerImpl.instance());

        String finalHtml = "<" + pageParser.getPageTemplate() + ">" + pageParser.getParsedHtml() + "</" + pageParser.getPageTemplate() + ">";

        Source source = new Source(finalHtml);
        SourceFormatter formatter = new SourceFormatter(source);
        formatter.setCollapseWhiteSpace(true);
        formatter.setIndentString("");
        formatter.setNewLine("");
        String finalHtmlFormatted = formatter.toString();
        //Logger.info(finalHtmlFormatted);

        String testFile = "/home/bram/Projects/Workspace/idea/com.beligum.mot.site/src/test/resources/testsave_raw.html";
        String parsedHtml = "";
        String baseUrl = "http://www.example.com";

        //        //Clean (HTML to XHTML)
        //        ContentHandler handler = new ToHTMLContentHandler();
        ////        ContentHandler handler = new ToXMLContentHandler();
        //        org.apache.tika.parser.html.HtmlParser parser = new org.apache.tika.parser.html.HtmlParser();
        //        Metadata metadata = new Metadata();
        //        try (InputStream stream = new FileInputStream(new File(testFile))) {
        //            parser.parse(stream, handler, metadata);
        //            if (!(handler instanceof ToHTMLContentHandler)) {
        //                xhtml += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        //                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.1//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-2.dtd\">\n";
        //            }
        //            xhtml += handler.toString();
        //        }

        // Parse str into a Document
        Document doc = Jsoup.parse(new File(testFile), null, baseUrl);
        // Clean the document (doesn't work because it strips the head out)
        //Whitelist whitelist = Whitelist.relaxed();
        //doc = new Cleaner(whitelist).clean(doc);
        // Adjust escape mode
        //doc.outputSettings().escapeMode(Entities.EscapeMode.base);
        //note: this is required! (doesn't work with html)
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        // Get back the string of the body.
        parsedHtml = doc.body().html();

        //semargl TEST
        //        Model model = ModelFactory.createDefaultModel();
        //        StreamProcessor streamProcessor = new StreamProcessor(RdfaParser.connect(JenaSink.connect(model)));
        //        streamProcessor.setProperty(RdfaParser.RDFA_VERSION_PROPERTY, RDFa.VERSION_11);
        //        //streamProcessor.setProperty(RdfaParser.ENABLE_VOCAB_EXPANSION, true);
        //        try (Reader reader = new StringReader(parsedHtml)) {
        //            streamProcessor.process(reader, baseUrl);
        //        }

        //'pure' Jena TEST
        Model model = ModelFactory.createDefaultModel();
        //net.rootdev.javardfa.jena.RDFaReader.XHTMLRDFaReader rdfaReader = new net.rootdev.javardfa.jena.RDFaReader.XHTMLRDFaReader();
        //Class.forName("net.rootdev.javardfa.jena.RDFaReader");
        JenaRdfaReader.inject();
        //JenaRdfaReader rdfaReader = new JenaRdfaReader();
        try (Reader reader = new StringReader(parsedHtml)) {
            model.read(reader, baseUrl, "RDFA");
            //rdfaReader.read(model, reader, baseUrl);
        }

        //TODO this is probably not the way...
        Model filteredModel = ModelFactory.createDefaultModel();
        StmtIterator selection = model.listStatements(new SimpleSelector(model.createProperty("http://mot.beligum.com/v1/resource/waterwell/belgium-aalst-nieuwerkerken-blauwenbergstraat-61"), null, (RDFNode)null));
        while (selection.hasNext()) {
            Statement stmt = selection.nextStatement();
            filteredModel.add(stmt.getSubject(), stmt.getPredicate(), stmt.getObject());
        }
//        filteredModel.add(selection);

        JsonLDWriter jsonLdWriter;
        File jsonLdFile = new File("/home/bram/Projects/Workspace/idea/com.beligum.mot.site/src/test/resources/testsave_raw.json");
        try (OutputStream out = new FileOutputStream(jsonLdFile)) {
            RDFDataMgr.write(out, filteredModel, org.apache.jena.riot.RDFFormat.JSONLD);
        }



//        model.listObjects();
//        //see https://github.com/jsonld-java/jsonld-java/issues/101
//        final JsonLdOptions options = new JsonLdOptions();
//        options.format = "application/jsonld";
//        String json = FileUtils.readFileToString(jsonLdFile);
//        Object compact = JsonLdProcessor.compact(new ByteArrayInputStream(json.getBytes("UTF-8")), json, options);
//        Logger.info(Json.write(compact));

        //        //Any23 TEST
        //        RDFaExtractor extractor = RDFaExtractor();

        //String testFileXHtml = "/home/bram/Projects/Workspace/idea/com.beligum.mot.site/src/test/resources/testsave_raw.xhtml";
        //        FileUtils.write(new File(testFileXHtml), xhtml);

        //        Model c = FileManager.get().loadModel("file://" + testFileXHtml);
        //        Model m = ModelFactory.createDefaultModel();
        //        StatementSink sink = new JenaStatementSink(m);
        //        XMLReader parser2 = ParserFactory.createReaderForFormat(sink, ParserFactory.Format.XHTML, Setting.OnePointOne);
        //        parser2.parse(testFileXHtml);
        //        boolean result = c.isIsomorphicWith(m);
        //        if (!result) m.write(System.err, "TTL");

        //this.getHdfsPageStore().save(uri, content, personRepository.get(Authentication.getCurrentPrincipal()));

        return Response.ok().build();
    }

    @POST
    @Path("/saveold/{url:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    // @bulk:  if true, we have to flush the bulk upload to ElasticSearch (used during import)
    public Response savePage(@PathParam("url") String url, @QueryParam("bulk") @DefaultValue("false") boolean bulk, String content) throws Exception
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

        //TODO change these to constants
        Template block = R.templateEngine().getNewStringTemplate(htmlTemplate.createNewHtmlInstance());
        retVal.put("html", block.render());
        retVal.put("inlineStyles", Lists.transform(Lists.newArrayList(htmlTemplate.getInlineStyleElementsForCurrentScope()), Functions.toStringFunction()));
        retVal.put("externalStyles", Lists.transform(Lists.newArrayList(htmlTemplate.getExternalStyleElementsForCurrentScope()), Functions.toStringFunction()));
        retVal.put("inlineScripts", Lists.transform(Lists.newArrayList(htmlTemplate.getInlineScriptElementsForCurrentScope()), Functions.toStringFunction()));
        retVal.put("externalScripts", Lists.transform(Lists.newArrayList(htmlTemplate.getExternalScriptElementsForCurrentScope()), Functions.toStringFunction()));

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
    public PageStore getHdfsPageStore() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGE_STORE)) {
            PageStore pageStore = new HdfsPageStore();
            pageStore.init();

            R.cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGE_STORE, pageStore);
        }

        return (PageStore) R.cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGE_STORE);
    }
}

package com.beligum.blocks.endpoints;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.indexes.LuceneSearchConfiguration;
import com.beligum.blocks.fs.indexes.stubs.PageStub;
import com.beligum.blocks.security.Permissions;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.spi.SearchIntegratorBuilder;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Path("debug")
@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
public class DebugEndpoint
{
    @Path("lucene")
    public Response testLucene() throws IOException
    {
        final java.nio.file.Path docDir = Settings.instance().getPageMainIndexFolder().toPath();
        if (!Files.exists(docDir)) {
            Files.createDirectories(docDir);
        }
        if (!Files.isWritable(docDir)) {
            throw new IOException("Lucene index directory is not writable, please check the path; " + docDir);
        }

        //TODO check .close()
        Directory dir = FSDirectory.open(docDir);
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        final boolean create = false;
        if (create) {
            // Create a new index in the directory, removing any
            // previously indexed documents:
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        }
        else {
            // Add new documents to an existing index:
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        try {
            SearchIntegratorBuilder searchIntegratorBuilder = new SearchIntegratorBuilder();
            SearchConfiguration config = new LuceneSearchConfiguration();
            searchIntegratorBuilder.configuration(config);
            ExtendedSearchIntegrator searchIntegrator = (ExtendedSearchIntegrator) searchIntegratorBuilder.buildSearchIntegrator();

            PageStub entity = new PageStub();
            //DocumentId idAnn = entity.getClass().getAnnotation(DocumentId.class);
            Serializable id = entity.getId();

            //See MoreLikeThisBuilder
            //TODO should we keep the fieldToAnalyzerMap around to pass to the analyzer?
            Map<String, String> fieldToAnalyzerMap = new HashMap<String, String>();
            //FIXME by calling documentBuilder we don't honor .comparingField("foo").ignoreFieldBridge(): probably not a problem in practice though
            DocumentBuilderIndexedEntity docBuilder = searchIntegrator.getIndexBinding(PageStub.class).getDocumentBuilder();
            Document doc = docBuilder.getDocument(null, entity, id, fieldToAnalyzerMap, null, new ContextualExceptionBridgeHelper(), null);

            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                writer.updateDocument(new Term("id", id.toString()), doc);
                //writer.addDocument(doc);
            }


            try (IndexReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                int numDocs = reader.numDocs();
                for ( int i = 0; i < numDocs; i++) {
                    Document d = reader.document(i);
                    System.out.println( "d=" +d);
                }

                Query q = new QueryParser("firstName", analyzer).parse("TEST");
                int hitsPerPage = 10;
                TopDocs docs = searcher.search(q, hitsPerPage);
                TopDocs docs2 = searcher.search(new FieldValueQuery("firstName"), hitsPerPage);
                ScoreDoc[] hits = docs.scoreDocs;
                ScoreDoc[] hits2 = docs2.scoreDocs;
                System.out.println("Found " + hits.length + " hits.");
                for (int i = 0; i < hits.length; ++i) {
                    int docId = hits[i].doc;
                    Document d = searcher.doc(docId);
                    System.out.println((i + 1) + ". " + d.get("id") + "\t" + d.get("firstName"));
                }
            }
        }
        catch (Exception e) {
            Logger.error("Error ", e);
        }

        return Response.ok().build();
    }
    //See org.hibernate.search.spi.SearchFactoryBuilder.initDocumentBuilders()
    private Document getDocumentBuilderFor(ExtendedSearchIntegrator searchIntegrator, Class<?> clazz)
    {


//        TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor(mappedClass);
//        final DocumentBuilderIndexedEntity<?> documentBuilder =
//                        new DocumentBuilderIndexedEntity(
//                                        mappedXClass,
//                                        typeMetadata,
//                                        configContext,
//                                        searchConfiguration.getReflectionManager(),
//                                        optimizationBlackListedTypes,
//                                        searchConfiguration.getInstanceInitializer()
//                        );

        return null;
    }
    /**
     * prepares XClasses from configuration
     */
    private static Map<XClass, Class<?>> initializeClassMappings(SearchConfiguration cfg, ReflectionManager reflectionManager) {
        Iterator<Class<?>> iter = cfg.getClassMappings();
        Map<XClass, Class<?>> map = new HashMap<XClass, Class<?>>();
        while ( iter.hasNext() ) {
            Class<?> mappedClass = iter.next();
            if ( mappedClass == null ) {
                continue;
            }

            XClass mappedXClass = reflectionManager.toXClass( mappedClass );
            if ( mappedXClass == null ) {
                continue;
            }
            map.put( mappedXClass, mappedClass );
        }
        return map;
    }


    //    @Path("infinispan")
    //    public Response testInfinispan()
    //    {
    //        Cache<String, PageStub> m_cache = MyObjectCacheFactory.getMyObjectCache();
    //
    //        int searchNumber = 7;
    //        SearchManager searchManager = Search.getSearchManager(m_cache );
    //        QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(PageStub.class).get();
    //        Query luceneQuery = queryBuilder.keyword().onField("searchNumber").matching(searchNumber).createQuery();
    //        CacheQuery cacheQuery = searchManager.getQuery(luceneQuery, PageStub.class );
    //
    //        //noinspection unchecked
    //        List result = (List)cacheQuery.list();
    //
    //
    //        return Response.ok().build();
    //    }
    //    @Path("hibernate-search")
    //    public Response testHibernateSearch()
    //    {
    //        EntityManager entityManager = new BlocksEntityManager();
    //        FullTextEntityManager fullTextEntityManager = org.hibernate.search.jpa.Search.getFullTextEntityManager(entityManager);
    //
    //        fullTextEntityManager.getTransaction().begin();
    //
    //        fullTextEntityManager.index(new PageStub());
    //
    //        fullTextEntityManager.getTransaction().commit();
    //        fullTextEntityManager.close();
    //
    //
    ////        Configuration cfg = new Configuration();
    ////        cfg.setProperty("hibernate.dialect", "com.beligum.blocks.fs.indexes.hibernate.BlocksDialect");
    ////        cfg.setProperty("hibernate.search.default.directory_provider", "filesystem");
    ////        cfg.setProperty("hibernate.search.default.indexBase", Settings.instance().getPageMainIndexFolder().getAbsolutePath());
    ////
    ////        List classes = Collections.singletonList(PageStub.class);
    ////        SessionFactory sessionFactory = cfg.buildSessionFactory();
    ////        Session session = sessionFactory.openSession();
    ////
    ////        session.close();
    //
    //        return Response.ok().build();
    //    }

    // THIS DELETES EVERYTHING, DON'T ENABLE BY DEFAULT!!!!

    //    @GET
    //    @Path("/flush")
    //    public Response flushEntities() throws Exception
    //    {
    //        Logger.warn("Database has been flushed by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");
    //        Logger.warn("Url-id mapping has been reset by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");
    //
    //        ElasticSearch.instance().getClient().admin().indices().delete(new DeleteIndexRequest("*")).actionGet();
    //
    //        ClassLoader classLoader = getClass().getClassLoader();
    //        String resourceMapping = null;
    //        String pageMapping = null;
    //        String pathMapping = null;
    //        String settings = null;
    //        try {
    //            resourceMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/resource.json"));
    //            pageMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/page.json"));
    //            pathMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/path.json"));
    //            settings = IOUtils.toString(classLoader.getResourceAsStream("elastic/settings.json"));
    //        }
    //        catch (Exception e) {
    //            Logger.error("Could not read mappings for elastic search", e);
    //        }
    //
    //        RequestContext.getEntityManager().createNativeQuery("delete from page where id > 0").executeUpdate();
    //        RequestContext.getEntityManager().createNativeQuery("delete from resource_language").executeUpdate();
    //        RequestContext.getEntityManager().createNativeQuery("delete from resource where id > 0").executeUpdate();
    //        RequestContext.getEntityManager().createNativeQuery("delete from path where id > 0").executeUpdate();
    //
    //        IndicesAdminClient esIndicesClient = ElasticSearch.instance().getClient().admin().indices();
    //        //there used to be an index for every language, but not anymore
    //        //for (Locale locale : Settings.instance().getLanguages().values()) {
    //        esIndicesClient.prepareCreate(ElasticSearch.instance().getPageIndexName(null)).setSettings(settings).addMapping(PersistenceController.WEB_PAGE_CLASS,pageMapping).execute().actionGet();
    //        esIndicesClient.prepareCreate(ElasticSearch.instance().getResourceIndexName(null)).setSettings(settings).addMapping("_default_",resourceMapping).execute().actionGet();
    //        //}
    //
    //        esIndicesClient.prepareCreate(PersistenceController.PATH_CLASS).setSettings(settings).addMapping(PersistenceController.PATH_CLASS, pathMapping).execute().actionGet();
    //
    //        return Response.ok("<ul><li>Database emptied</li><li>Cache reset</li></ul>").build();
    //    }

    //    @GET
    //    @Path("/pagetemplates/{pageTemplateName}")
    //    public Response getPageTemplatePage(
    //                    @PathParam("pageTemplateName")
    //                    String pageTemplateName,
    //                    @QueryParam("lang")
    //                    String getLanguage) throws Exception
    //    {
    //        if (StringUtils.isEmpty(getLanguage)) {
    //            getLanguage = Settings.instance().getDefaultLanguage();
    //        }
    //        PageTemplate pageTemplate = Blocks.templateCache().getPageTemplate(pageTemplateName);
    //        Template template = pagetemplate.get().getNewTemplate();
    //        template.set("DateTool", new DateTool());
    //        template.set("EscapeTool", new EscapeTool());
    //        template.set("pageTemplate", pageTemplate);
    //        template.set("activeLanguage", getLanguage);
    //        //TODO: rendering should include links ands scripts for full view of blueprint
    //        //        String resourcePath = XMLUrlIdMapper.getInstance().getUrl(pageTemplate.getId()).getSimplePath().substring(1);
    //        //        template.set("src", DebugEndpointRoutes.showTemplate(resourcePath, null, PAGE_TEMPLATE_TYPE).getAbsoluteUrl());
    //        return Response.ok(template).build();
    //    }
    //
    //    @GET
    //    @Path("/blueprints")
    //    public Response getBlueprintsPage() throws Exception
    //    {
    //        Template template = blueprints.get().getNewTemplate();
    //        template.set("blueprints", Blocks.templateCache().getBlueprints());
    //        return Response.ok(template).build();
    //    }
    //
    //    @GET
    //    @Path("/blueprints/{blueprintName}")
    //    public Response getBlueprintPage(@PathParam("blueprintName") String blueprintName, @QueryParam("lang") String getLanguage) throws Exception
    //    {
    //        if (StringUtils.isEmpty(getLanguage)) {
    //            getLanguage = Settings.instance().getDefaultLanguage();
    //        }
    //        Blueprint blueprintObj = Blocks.templateCache().getBlueprint(blueprintName);
    //        Template template = blueprint.get().getNewTemplate();
    //        template.set("DateTool", new DateTool());
    //        template.set("EscapeTool", new EscapeTool());
    //        template.set("blueprint", blueprintObj);
    //        template.set("activeLanguage", getLanguage);
    //        //TODO: rendering should include links ands scripts for full view of blueprint
    //        BlocksTemplateRenderer renderer = Blocks.factory().createTemplateRenderer();
    //        template.set("src", renderer.render(blueprintObj, null, Settings.instance().getDefaultLanguage()));
    //        return Response.ok(template).build();
    //    }
    //
    //    @GET
    //    @Path("/sitemap")
    //    public Response viewSiteMap(@QueryParam("lang") String getLanguage) throws UrlIdMappingException
    //    {
    //        ArrayList<String> languages = Settings.instance().getLanguages();
    //
    //        Template template = sitemap.get().getNewTemplate();
    //        BlocksUrlDispatcher sitemap = Blocks.urlDispatcher();
    //        template.set("urlmap", sitemap);
    //        template.set("languages", languages);
    //
    //        return Response.ok(template).build();
    //    }
    //
    //    @GET
    //    @Path("src/blueprints")
    //    @Produces("text/plain")
    //    public Response getBlueprintsCache(@QueryParam("lang") String getLanguage) throws Exception
    //    {
    //        if (getLanguage == null)
    //            getLanguage = Settings.instance().getDefaultLanguage();
    //
    //        String cache = "";
    //        for (Blueprint blueprint : Blocks.templateCache().getBlueprints()) {
    //            cache += "----------------------------------" + blueprint.getBlueprintName() + "---------------------------------- \n\n" + blueprint.getValue() + "\n\n\n\n\n\n";
    //        }
    //        return Response.ok(cache).build();
    //    }
    //
    //    @GET
    //    @Path("src/pagetemplates")
    //    @Produces("text/plain")
    //    public Response getPageTemplateCache(@QueryParam("lang") String getLanguage) throws Exception
    //    {
    //        if (getLanguage == null)
    //            getLanguage = Settings.instance().getDefaultLanguage();
    //
    //        String cache = "";
    //        for (PageTemplate pageTemplate : Blocks.templateCache().getPageTemplates()) {
    //            cache += "----------------------------------" + pageTemplate.getBlueprintName() + "---------------------------------- \n\n" + pageTemplate.getValue() + "\n\n\n\n\n\n";
    //        }
    //        return Response.ok(cache).build();
    //    }
    //
    //    //
    //    //    @GET
    //    //    @Path("/show/{resourcePath:.+}")
    //    //    @Produces("text/html")
    //    //    public Response showTemplate(
    //    //                    @PathParam("resourcePath")
    //    //                    @DefaultValue("")
    //    //                    String resourcePath,
    //    //                    @QueryParam("fragment")
    //    //                    @DefaultValue("")
    //    //                    String fragment,
    //    //                    @QueryParam("type")
    //    //                    String typeName)
    //    //                    throws Exception
    //    //    {
    //    //        URL url = renderUrl(resourcePath, fragment);
    //    //        Class<? extends AbstractTemplate> type = determineType(typeName);
    //    //        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
    //    //        AbstractTemplate template = (AbstractTemplate) RedisDatabase.getInstance().fetchLastVersion(id, type);
    //    //        if(template instanceof EntityTemplate) {
    //    //            return Response.ok(((EntityTemplate) template).renderEntityInPageTemplate(template.getLanguage())).build();
    //    //        }
    //    //        else if(template instanceof PageTemplate){
    //    //            Blueprint defaultBlueprint = BlueprintsCache.getInstance().get(ParserConstants.DEFAULT_BLUEPRINT);
    //    //            return Response.ok(TemplateParser.renderTemplate(TemplateParser.parse(template.getTemplate()), Settings.instance().getSiteDomainUrl(), id.getLanguage(), template.getLinks(), template.getScripts()).outerHtml()).build();
    //    //        }
    //    //        else{
    //    //            return Response.ok(TemplateParser.renderTemplate(template, id.getLanguage())).build();
    //    //        }
    //    //    }
    //    //
    //    //    @GET
    //    //    @Path("/hash/{resourcePath:.+}")
    //    //    @Produces("text/html")
    //    //    public Response getTemplateHash(
    //    //                    @PathParam("resourcePath")
    //    //                    @DefaultValue("")
    //    //                    String resourcePath,
    //    //                    @QueryParam("fragment")
    //    //                    @DefaultValue("")
    //    //                    String fragment,
    //    //                    @QueryParam("type")
    //    //                    String typeName)
    //    //                    throws Exception
    //    //    {
    //    //        URL url = renderUrl(resourcePath, fragment);
    //    //        Class<? extends AbstractTemplate> type = this.determineType(typeName);
    //    //        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
    //    //        AbstractTemplate template = (AbstractTemplate) RedisDatabase.getInstance().fetchLastVersion(id, type);
    //    //        String retVal = "";
    //    //        Map<String, String> hash = template.toHash();
    //    //        List<String> keys = new ArrayList<>(hash.keySet());
    //    //        Collections.sort(keys);
    //    //        for(String key : keys){
    //    //            String fieldContent = hash.get(key);
    //    //            fieldContent = fieldContent.replace("<", "&lt;");
    //    //            fieldContent = fieldContent.replace(">", "&gt;");
    //    //            retVal += key + "  ---->  " + fieldContent + "<br/><br/>";
    //    //        }
    //    //        return Response.ok(retVal).build();
    //    //    }
    //    //
    //    //    @GET
    //    //    @Path("/hash/allversions/{resourcePath:.+}")
    //    //    @Produces("text/html")
    //    //    public Response getTemplateHashForAllVersions(@PathParam("resourcePath")
    //    //                                                  @DefaultValue("")
    //    //                                                  String resourcePath,
    //    //                                                  @QueryParam("fragment")
    //    //                                                  @DefaultValue("")
    //    //                                                  String fragment,
    //    //                                                  @QueryParam("type")
    //    //                                                  String typeName) throws Exception
    //    //    {
    //    //        Class<? extends AbstractTemplate> type = determineType(typeName);
    //    //        URL url = renderUrl(resourcePath, fragment);
    //    //        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
    //    //        List<AbstractTemplate> versions = RedisDatabase.getInstance().fetchVersionList(id, type);
    //    //        String retVal = "";
    //    //        for(AbstractTemplate template : versions) {
    //    //            if(template != null) {
    //    //                retVal += "----------------------------------" + template.getId() + "---------------------------------- <br/><br/>";
    //    //                Map<String, String> hash = template.toHash();
    //    //                List<String> keys = new ArrayList<>(hash.keySet());
    //    //                Collections.sort(keys);
    //    //                for (String key : keys) {
    //    //                    String fieldContent = hash.get(key);
    //    //                    fieldContent = fieldContent.replace("<", "&lt;");
    //    //                    fieldContent = fieldContent.replace(">", "&gt;");
    //    //                    retVal += key + "  ---->  " + fieldContent + "<br/><br/>";
    //    //                }
    //    //                retVal += "<br/><br/><br/>";
    //    //            }
    //    //            else{
    //    //                retVal += "----------------------------------FOUND NULL TEMPLATE----------------------------------";
    //    //                retVal += "<br/><br/><br/><br/><br/>";
    //    //            }
    //    //        }
    //    //        return Response.ok(retVal).build();
    //    //    }
    //    //
    //    //    @GET
    //    //    @Path("/src/allversions/{resourcePath:.+}")
    //    //    @Produces("text/plain")
    //    //    public Response getTemplateSrcForAllVersions(@PathParam("resourcePath")
    //    //                                                 @DefaultValue("")
    //    //                                                 String resourcePath,
    //    //                                                 @QueryParam("fragment")
    //    //                                                 @DefaultValue("")
    //    //                                                 String fragment,
    //    //                                                 @QueryParam("type")
    //    //                                                 String typeName) throws Exception
    //    //    {
    //    //        Class<? extends AbstractTemplate> type = determineType(typeName);
    //    //        URL url = renderUrl(resourcePath, fragment);
    //    //        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
    //    //        List<AbstractTemplate> versions = RedisDatabase.getInstance().fetchVersionList(id, type);
    //    //        String retVal = "";
    //    //        for(AbstractTemplate template : versions) {
    //    //            if(template != null) {
    //    //                retVal += "----------------------------------" + template.getId() + "---------------------------------- \n \n";
    //    //                Map<BlocksID, String> languageTemplates = template.getTemplates();
    //    //                for(BlocksID languagedId : languageTemplates.keySet()){
    //    //                    retVal += "----------------------------------" + languagedId.getLanguage() + "----------------------------------  \n";
    //    //                    String toBeAdded = languageTemplates.get(languagedId);
    //    //                    retVal += toBeAdded;
    //    //                }
    //    //
    //    //                retVal += "\n\n\n";
    //    //            }
    //    //            else{
    //    //                retVal += "----------------------------------FOUND NULL TEMPLATE----------------------------------";
    //    //                retVal += "\n\n\n\n\n";
    //    //            }
    //    //        }
    //    //        return Response.ok(retVal).build();
    //    //    }
    //
    //    private URL renderUrl(String resourcePath, String fragment) throws MalformedURLException
    //    {
    //        if (!StringUtils.isEmpty(fragment)) {
    //            resourcePath += "#" + fragment;
    //        }
    //        return new URL(Settings.instance().getSiteDomain() + "/" + resourcePath);
    //    }
    //
    //    private Class<? extends AbstractTemplate> determineType(String typeName){
    //        Class<? extends AbstractTemplate> type;
    //        if(!StringUtils.isEmpty(typeName)) {
    //            switch (typeName) {
    //                case ENTTIY_INSTANCE_TYPE:
    //                    type = EntityTemplate.class;
    //                    break;
    //                case BLUEPRINT_TYPE:
    //                    type = Blueprint.class;
    //                    break;
    //                case PAGE_TEMPLATE_TYPE:
    //                    type = PageTemplate.class;
    //                    break;
    //                case XML_TEMPLATE_TYPE:
    //                    type = UrlIdMapping.class;
    //                    break;
    //                default:
    //                    type = EntityTemplate.class;
    //                    break;
    //            }
    //        }
    //        else{
    //            type = EntityTemplate.class;
    //        }
    //        return type;
    //    }

}

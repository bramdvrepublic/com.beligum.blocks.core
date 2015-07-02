package com.beligum.blocks.database;

/**
* Created by wouter on 5/06/15.
*/
public class OBlocksDatabase
{
    // The name of the path. This field should be prepended with the getLanguage code e.g. nl_name, fr_name
    // this is the name of the property in the database
    public final static String PATH_NAME_FIELD = "name";

    // the verb property name in the OrientDB
    public final static String WEB_NODE_STATUS_FIELD = "statuscode";

    // The object property in the OrientDB
    public final static String WEB_NODE_PAGE_FIELD = "page";

    public final static String ROOT_WEB_NODE_HOST_NAME = "hostname";

    // The classname of a node in the OrientDB
    public final static String WEB_NODE_CLASS = "WebNode";

    // The classname of a root node in the OrientDB
    public final static String ROOT_WEB_NODE_CLASS = "WebRootNode";

    // the classname for the path in the Orient DB
    public final static String PATH_CLASS_NAME = "WebPath";

    public static String MASTER_WEB_PAGE_CLASS = "MasterWebPage";
    public static String WEB_PAGE_CLASS = "WebPage";
    public static String WEB_PAGE_LOCALIZED_CLASS = "LocalizedWebPage";
    public static String WEB_PAGE_VERSIONED_CLASS = "versionedWebPage";
    public static String WEB_PAGE_PREVIOUS = "previouspage";

    public static String WEB_PAGE_HTML = "html";
    public static String WEB_PAGE_TEMPLATE = "template";
    public static String WEB_PAGE_TEXT = "text";
    public static String WEB_PAGE_RESOURCES = "resources";
    public static String WEB_PAGE_LINKS = "links";
    public static String WEB_PAGE_PROPERTIES = "properties";
    public static String WEB_PAGE_TEMPLATES = "templates";

    public static final String RESOURCE_CLASS = "DefaultResource";
    public static final String LOCALIZED_RESOURCE_CLASS = "LocalizedResource";
    public final static String RESOURCE_TYPE_FIELD = "@rdftype";
    public static final String RESOURCE_LOCALIZED_FIELD = "localized";

    public static final String RESOURCE_CREATED_BY = "createdBy";
    public static final String RESOURCE_UPDATED_BY = "updatedBy";
    public static final String RESOURCE_CREATED_AT = "createdAt";
    public static final String RESOURCE_UPDATED_AT = "updatedAt";

//    private OrientGraphFactory graphFactory;
//    private ThreadLocal<OrientBaseGraph> graph;
//
//
//    public static OBlocksDatabase instance;
//
//    private OBlocksDatabase() {
//        OrientBaseGraph graph = this.getGraph();
//        createSchemaOnStartup(graph);
//    }
//
//    public static OBlocksDatabase instance() {
//        if (OBlocksDatabase.instance == null) {
//            OBlocksDatabase.instance = new OBlocksDatabase();
//        }
//        return OBlocksDatabase.instance;
//    }
//
////    @Override
////    public WebPage createWebPage(Locale locale)
////    {
////        URI uri = RdfTools.createLocalResourceId(WEB_PAGE_CLASS);
////        return createWebPage(uri, locale);
////    }
//
//
//    @Override
//    public WebPage createWebPage(URI masterWebPage, URI id, Locale locale)
//    {
////        Graph graph = getGraph();
////
////        WebPage retVal;
////        retVal = masterWebPage.getPageForLocale(locale);
////        if (retVal == null) {
////            Vertex v = graph.addVertex("class:" + WEB_PAGE_CLASS);
////            v.setProperty(ParserConstants.JSONLD_ID, id.toString());
////            v.setProperty(ParserConstants.JSONLD_LANGUAGE, locale.getLanguage());
////
////            Edge e = graph.addEdge(null, ((OMasterWebPage)masterWebPage).getVertex(), v, WEB_PAGE_LOCALIZED_CLASS);
////            e.setProperty(ParserConstants.JSONLD_LANGUAGE, locale.getLanguage());
////            retVal = new OWebPage(v, locale);
////        }
////
////
////        this.touch(retVal);
//        return null;
//    }
//
////    @Override
////    public MasterWebPage getMasterWebPage(URI id)
////    {
////        MasterWebPage retVal = null;
////
////        OrientBaseGraph graph = getGraph();
////        Iterable<Vertex> vertices = graph.command(new OSQLSynchQuery("select from " + MASTER_WEB_PAGE_CLASS + " WHERE @id = '" + id + "' fetchplan *:0")).execute();
////
////            for (Vertex v: vertices) {
////            retVal = new OMasterWebPage(v);
////            break;
////        }
////        return retVal;
////    }
//
//    @Override
//    public WebPage getWebPage(URI masterWebPage, Locale locale)
//    {
//        WebPage retVal = null;
////        OrientBaseGraph graph = getGraph();
////        Iterable<Vertex> vertices = graph.command(new OSQLSynchQuery("select from " + WEB_PAGE_CLASS + " WHERE @id = '" + id + "' fetchplan *:0")).execute();
////
////        for (Vertex v: vertices) {
////            retVal = new OWebPage(v, locale);
////            break;
////        }
//        return retVal;
//    }
//
//    @Override
//    public WebPage getWebPage(URI id)
//    {
//        WebPage retVal = null;
////        OrientBaseGraph graph = getGraph();
////        Iterable<Vertex> vertices = graph.command(new OSQLSynchQuery("select from " + WEB_PAGE_CLASS + " WHERE @id = '" + id + "' fetchplan *:0")).execute();
////
////        for (Vertex v: vertices) {
////            retVal = new OWebPage(v, locale);
////            break;
////        }
//        return retVal;
//    }
//
//    @Override
//    public WebPage deleteWebPage(URI id, Locale locale)
//    {
//        Graph graph = getGraph();
//        Vertex v = graph.addVertex("class:" + WEB_PAGE_CLASS);
//        v.setProperty(ParserConstants.JSONLD_ID, id.toString());
//        v.setProperty(ParserConstants.JSONLD_LANGUAGE, locale.getLanguage());
//        WebPage webPage = new OWebPage(v, locale);
//        return webPage;
//    }
//
//    @Override
//    public WebPage saveWebPage(WebPage webPage, boolean doVersion) {
//        Graph graph = getGraph();
//
//        if (doVersion) {
//            Vertex versionedVertex = graph.addVertex("class:" + WEB_PAGE_CLASS);
//            OVersionedWebPage versioned = new OVersionedWebPage(versionedVertex, webPage);
//
//            Iterable<Edge> edges = ((OWebPage) webPage).getVertex().query().direction(Direction.OUT).labels(WEB_PAGE_PREVIOUS).edges();
//            Edge previous = null;
//            for (Edge edge : edges) {
//                previous = edge;
//                break;
//            }
//            if (previous != null) {
//                Vertex previousVersionedVertex = previous.getVertex(Direction.IN);
//                previous.remove();
//                graph.addEdge(null, ((OWebPage) webPage).getVertex(), versionedVertex, WEB_PAGE_PREVIOUS);
//                graph.addEdge(null, versionedVertex, previousVersionedVertex, WEB_PAGE_PREVIOUS);
//            }
//            else {
//                graph.addEdge(null, ((OWebPage) webPage).getVertex(), versionedVertex, WEB_PAGE_PREVIOUS);
//            }
//        }
//
//        webPage = (WebPage)this.touch(webPage);
//        this.getGraph().commit();
//        addToLucene(((OrientVertex)webPage.getValue()).getRecord().getIdentity().toString(), webPage.toJson(), webPage.getLanguage(), TYPE.WEBPAGE);
//
//        return webPage;
//    }
//
//
//
//    @Override
//    public Resource createResource(URI id, URI rdfType, Locale getLanguage)
//    {
//        Vertex v = new OrientVertex();
//
//        Resource retVal = null;
//        ODocument defaultVertex = getVertexWithBlockId(id);
//        if (defaultVertex == null) {
//            defaultVertex = createDefaultVertex(id, rdfType);
//        } else {
//            Logger.debug("Use found document");
//        }
//        if (getLanguage == null || getLanguage.equals(Locale.ROOT)) getLanguage = BlocksConfig.instance().getDefaultLanguage();
//        ODocument localized = getOrCreateLocalizedVertex(defaultVertex, getLanguage);
//        retVal = new OrientResource(defaultVertex, localized);
//        this.getDatabase().save(defaultVertex);
//        this.getDatabase().save(localized);
//        this.touch(retVal);
//        return retVal;
//    }
//
//    @Override
//    public Resource getResource(URI id, Locale getLanguage)
//    {
//        Resource retVal = null;
//        ODocument defaultVertex = getVertexWithBlockId(id);
//        if (defaultVertex != null) {
//            ODocument localized = getOrCreateLocalizedVertex(defaultVertex, getLanguage);
//            retVal = new OrientResource(defaultVertex, localized);
//        }
//        return retVal;
//    }
//
//    @Override
//    public Resource saveResource(Resource resource) {
//        this.touch(resource);
//        getDatabase().save((ODocument) resource.getValue());
//        this.getGraph().commit();
//        addToLucene(((ODocument) resource.getValue()).getIdentity().toString(), resource.toJson(), resource.getLanguage(), TYPE.RESOURCE);
//        return resource;
//    }
//
//    @Override
//    public Resource deleteResource(Resource resource) {
//        return resource;
//    }
//
//
//    @Override
//    public Node createNode(Object value, Locale getLanguage)
//    {
//        Node retVal = null;
//        if (value instanceof ODocument) {
//            retVal = this.asResource((ODocument)value, getLanguage);
//        } else {
//            retVal = new OrientNode(value, getLanguage);
//        }
//
//        return retVal;
//    }
//    @Override
//    public WebPath getPath(URI masterPage, Locale locale)
//    {
//        return null;
//    }
//    @Override
//    public WebPath getPath(Path path, Locale locale)
//    {
//        return null;
//    }
//    @Override
//    public WebPath createPath(URI masterPage, Path path, Locale locale)
//    {
//        return null;
//    }
//
//    // ------- PROTECTED METHODS ---------
//    public ODocument getOrCreateLocalizedVertex(ODocument defaultVertex, Locale getLanguage) {
//        ODocument retVal = getLocalizedVertexForDefault(defaultVertex, getLanguage);
//        if (retVal == null) {
//            // If resource does not yet exist, create it
//            retVal = createLocalizedVertex(defaultVertex, getLanguage);
//        }
//        return retVal;
//    }
//
//    public ODocument createLocalizedVertex(ODocument defaultVertex, Locale getLanguage) {
//        ODocument retVal = new ODocument(LOCALIZED_RESOURCE_CLASS);
//        retVal.field(ParserConstants.JSONLD_LANGUAGE, getLanguage);
//        defaultVertex.field(localizedVersionField(getLanguage.getLanguage()), retVal);
//        return retVal;
//    }
//
//
//    public ODocument createDefaultVertex(URI id, URI rdfType) {
//        if (id == null) throw new NullPointerException();
//
//        ODocument vertex = new ODocument(RESOURCE_CLASS);
//        List list = new ArrayList<String>();
//        list.add(rdfType.toString());
//        if (rdfType != null) vertex.field(OBlocksDatabase.RESOURCE_TYPE_FIELD, list);
//        vertex.field(ParserConstants.JSONLD_ID, id.toString());
//        return vertex;
//    }
//
//
//    // ------- PRIVATE METHODS ---------
//
//
//
//    private ODocument getVertexWithBlockId(URI id)
//    {
//        ODocument retVal = null;
//        Iterable<ODocument> docs = getDatabase().command(new OSQLSynchQuery<ODocument>("select FROM " + RESOURCE_CLASS + " WHERE " + ParserConstants.JSONLD_ID + " = '" + id.toString() +"'")).execute();
//        if (docs != null) {
//            Iterator<ODocument> iterator = docs.iterator();
//            if (iterator.hasNext()) {
//                retVal = iterator.next();
//            }
//        }
//        return retVal;
//    }
//
//    protected ODocument getLocalizedVertexForDefault(ODocument defaultVertex, Locale getLanguage) {
//        ODocument retVal = defaultVertex.field(localizedVersionField(getLanguage.getLanguage()));
//        return retVal;
//    }
//
//    private Resource asResource(ODocument vertex, Locale getLanguage)
//    {
//        ODocument defaultResource = vertex;
//        Resource retVal = null;
//        if (vertex.getClassName() !=null && vertex.getClassName().equals(RESOURCE_CLASS)) {
//            ODocument localized = getOrCreateLocalizedVertex(defaultResource, getLanguage);
//            retVal = new OrientResource(defaultResource, localized);
//        } else {
//            retVal = new OrientResource(defaultResource, null);
//        }
//        return retVal;
//    }
//
//    // TODO Th
//    public OrientBaseGraph getGraph()
//    {
//        if (this.graphFactory == null) {
////            OGlobalConfiguration.USE_WAL.setValue(false);
//            OGlobalConfiguration.INDEX_MANUAL_LAZY_UPDATES.setValue(-1);
//            this.graphFactory = new OrientGraphFactory("remote:/localhost/mot", "admin", "admin").setupPool(1, 10);
////            this.graphFactory = new OrientGraphFactory("plocal:/Users/wouter/orientDB/databases/mot", "admin", "admin").setupPool(1, 10);
//        }
//
//        if (this.graph == null) {
////            final Graph  db = graphFactory.getNoTx();
//            final OrientBaseGraph  db = graphFactory.getNoTx();
//            this.graph = new ThreadLocal<OrientBaseGraph>() {
//                protected OrientBaseGraph initialValue() {
//                    return db;
//                }
//            };
//        }
//        return this.graph.get();
//    }
//
//
//    public ODatabaseDocumentTx getDatabase()
//    {
//        OrientBaseGraph graph = getGraph();
//        ODatabaseRecordThreadLocal.INSTANCE.set(graph.getRawGraph());
//        return graph.getRawGraph();
//
//
//    }
//
//
//    private void createSchemaOnStartup(OrientBaseGraph graph) {
//        if (graph.getVertexType(RESOURCE_CLASS) == null) {
//            OrientVertexType defaultResourceClass = graph.createVertexType(RESOURCE_CLASS);
//            defaultResourceClass.createProperty(ParserConstants.JSONLD_ID, OType.STRING);
//            defaultResourceClass.createProperty(RESOURCE_TYPE_FIELD, OType.EMBEDDEDSET);
////            graph.createIndex(RESOURCE_TYPE_FIELD, Vertex.class, new Parameter("class", RESOURCE_CLASS));
////            graph.createIndex(ParserConstants.JSONLD_ID, Vertex.class, new Parameter("type", "UNIQUE"), new Parameter("class", RESOURCE_CLASS));
//        }
//
//        if (graph.getVertexType(LOCALIZED_RESOURCE_CLASS) == null) {
//            OrientVertexType localizedResourceClass = graph.createVertexType(LOCALIZED_RESOURCE_CLASS);
//            localizedResourceClass.createProperty(ParserConstants.JSONLD_LANGUAGE, OType.STRING);
//        }
//
//        if (graph.getVertexType(WEB_NODE_CLASS) == null) {
//            OrientVertexType nodeClass = graph.createVertexType(WEB_NODE_CLASS);
//            nodeClass.createProperty(WEB_NODE_STATUS_FIELD, OType.INTEGER);
//            nodeClass.createProperty(WEB_NODE_PAGE_FIELD, OType.STRING);
////            graph.createIndex(WEB_NODE_PAGE_FIELD, Vertex.class, new Parameter("class", WEB_NODE_CLASS));
////            graph.createIndex(WEB_NODE_STATUS_FIELD, Vertex.class, new Parameter("class", WEB_NODE_CLASS));
//        }
//
//        if (graph.getVertexType(ROOT_WEB_NODE_CLASS) == null) {
//            OrientVertexType rootNodeClass = graph.createVertexType(ROOT_WEB_NODE_CLASS, WEB_NODE_CLASS);
//            rootNodeClass.createProperty(ROOT_WEB_NODE_HOST_NAME, OType.STRING);
////            graph.createIndex(ROOT_WEB_NODE_HOST_NAME, Vertex.class, new Parameter("class", ROOT_WEB_NODE_CLASS));
//        }
//
//        if (graph.getEdgeType(PATH_CLASS_NAME) == null) {
//            OrientEdgeType pathClass = graph.createEdgeType(PATH_CLASS_NAME);
//            pathClass.createProperty(PATH_NAME_FIELD, OType.STRING);
////            graph.createIndex(PATH_NAME_FIELD, Vertex.class, new Parameter("class", PATH_CLASS_NAME));
//            for (Locale locale: BlocksConfig.instance().getLanguages().values()) {
//                String field = getLocalizedNameField(locale);
//                pathClass.createProperty(field, OType.STRING);
////                graph.createIndex(field, Vertex.class, new Parameter("class", PATH_CLASS_NAME));
//            }
//        }
//
//        if (graph.getEdgeType(WEB_PAGE_LOCALIZED_CLASS) == null) {
//            OrientEdgeType pathClass = graph.createEdgeType(WEB_PAGE_LOCALIZED_CLASS);
//            pathClass.createProperty(ParserConstants.JSONLD_LANGUAGE, OType.STRING);
//
//        }
//
//    }
//
//    /*
//    * Fieldname of the localized version of a resource
//    * */
//    private String localizedVersionField(String getLanguage) {
//        return RESOURCE_LOCALIZED_FIELD + "_" + getLanguage;
//    }
//
//    // STATIC METHODS
//
//    // TODO give this a better place
//    public static String getLocalizedNameField(Locale locale) {
//        String retVal = PATH_NAME_FIELD;
//        if (!locale.equals(Locale.ROOT)) {
//            retVal = locale.getLanguage() + "_" + PATH_NAME_FIELD;
//        }
//        return retVal;
//    }
//
//    private DocumentInfo touch(DocumentInfo resource) {
//        if (resource.getCreatedAt() == null) {
//            resource.setCreatedAt(Calendar.getInstance());
//        }
//
//        if (resource.getCreatedBy() == null) {
//            resource.setCreatedBy(BlocksConfig.instance().getCurrentUserName());
//        }
//
//        resource.setUpdatedAt(Calendar.getInstance());
//        resource.setUpdatedBy(BlocksConfig.instance().getCurrentUserName());
//        return resource;
//    }
//
//    private void addToLucene(String id, String json, Locale locale, TYPE type) {
//        String index;
//        String name = BlocksDatabase.resource;
//        if (type.equals(TYPE.RESOURCE)) {
//            index = ElasticSearchServer.instance().getResourceIndexName(locale);
//        } else {
//            index = ElasticSearchServer.instance().getPageIndexName(locale);
//            name = BlocksDatabase.webpage;
//        }
//        ElasticSearchServer.instance().getBulk().add(ElasticSearchClient.instance().getClient().prepareIndex(index, name)
//                                                                        .setSource(json)
//                                                                        .setId(id).request());
//
//    }
//
//    private enum TYPE {
//        RESOURCE, WEBPAGE
//    }

}

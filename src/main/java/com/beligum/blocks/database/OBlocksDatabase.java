package com.beligum.blocks.database;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.database.interfaces.BlocksDatabase;
import com.beligum.blocks.pages.OWebPage;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.resources.OrientNode;
import com.beligum.blocks.resources.OrientResource;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.beligum.blocks.routing.OWebNode;
import com.beligum.blocks.routing.OWebPath;
import com.beligum.blocks.routing.ifaces.WebNode;
import com.beligum.blocks.routing.ifaces.WebPath;
import com.beligum.blocks.utils.RdfTools;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Parameter;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.*;

import java.net.URI;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by wouter on 5/06/15.
 */
public class OBlocksDatabase implements BlocksDatabase
{
    // The name of the path. This field should be prepended with the language code e.g. nl_name, fr_name
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

    public static String WEB_PAGE_CLASS = "WebPage";
    public static String WEB_PAGE_TEMPLATE = "title";
    public static String WEB_PAGE_HTML = "html";
    public static String WEB_PAGE_RESOURCES = "resources";
    public static String WEB_PAGE_LINKS = "links";
    public static String WEB_PAGE_PROPERTIES = "properties";


    public static final String RESOURCE_CLASS = "DefaultResource";
    public static final String LOCALIZED_RESOURCE_CLASS = "LocalizedResource";
    public final static String RESOURCE_TYPE_FIELD = "@rdftype";
    public static final String RESOURCE_LOCALIZED_FIELD = "localized";

    public static final String RESOURCE_CREATED_BY = "createdBy";
    public static final String RESOURCE_UPDATED_BY = "updatedBy";
    public static final String RESOURCE_CREATED_AT = "createdAt";
    public static final String RESOURCE_UPDATED_AT = "updatedAt";

    private OrientGraphFactory graphFactory;
    private ThreadLocal<OrientGraph> graph;


    public static OBlocksDatabase instance;

    private OBlocksDatabase() {
        OrientGraph graph = (OrientGraph)this.getGraph();
        createSchemaOnStartup(graph);
    }

    public static OBlocksDatabase instance() {
        if (OBlocksDatabase.instance == null) {
            OBlocksDatabase.instance = new OBlocksDatabase();
        }
        return OBlocksDatabase.instance;
    }

    @Override
    public WebPage createWebPage(Locale locale)
    {
        URI uri = RdfTools.createLocalResourceId(WEB_PAGE_CLASS);
        return createWebPage(uri, locale);
    }

    @Override
    public WebPage createWebPage(URI id, Locale locale)
    {
        OrientGraph graph = getGraph();
        Vertex v = graph.addVertex("class:" + WEB_PAGE_CLASS);
        v.setProperty(ParserConstants.JSONLD_ID, id.toString());
        v.setProperty(ParserConstants.JSONLD_LANGUAGE, locale.getLanguage());
        WebPage webPage = new OWebPage(v, locale);
        return webPage;
    }


    @Override
    public WebPage getWebPage(URI id, Locale locale)
    {
        WebPage retVal = null;
        OrientGraph graph = getGraph();
        Iterable<Vertex> vertices = graph.command(new OSQLSynchQuery("select from " + WEB_PAGE_CLASS + " WHERE @id = '" + id + "' fetchplan *:0")).execute();
        for (Vertex v: vertices) {
            retVal = new OWebPage(v, locale);
            break;
        }
        return retVal;
    }

    @Override
    public WebPage deleteWebPage(URI id, Locale locale)
    {
        OrientGraph graph = getGraph();
        Vertex v = graph.addVertex("class:" + WEB_PAGE_CLASS);
        v.setProperty(ParserConstants.JSONLD_ID, id.toString());
        v.setProperty(ParserConstants.JSONLD_LANGUAGE, locale.getLanguage());
        WebPage webPage = new OWebPage(v, locale);
        return webPage;
    }

    @Override
    public WebPage save(WebPage webPage) {
        return webPage;
    }


    @Override
    public WebNode createRootWebNode(String host)
    {
        WebNode retVal;

        Vertex vertex = null;
        OrientGraph graph = getGraph();

        Iterable<Vertex> vertices = graph.command(new OSQLSynchQuery("select from " + ROOT_WEB_NODE_CLASS + " WHERE " + ROOT_WEB_NODE_HOST_NAME + " = '" + host + "' fetchplan *:0")).execute();

        for (Vertex v: vertices) {
            vertex = v;
            break;
        }
        if (vertex == null) {
            vertex = graph.addVertex("class:" + ROOT_WEB_NODE_CLASS);
            vertex.setProperty(ROOT_WEB_NODE_HOST_NAME, host);
            retVal = new OWebNode(vertex);
        } else {
            retVal = new OWebNode(vertex);
        }
        retVal.setStatusCode(404);
        graph.commit();
        graph.begin();
        return retVal;
    }


    @Override
    public WebNode getRootWebNode(String host)
    {
        WebNode retVal= null;

        Vertex vertex = null;
        OrientGraph graph = getGraph();
        Iterable<Vertex> vertices = graph.command(new OSQLSynchQuery("select from " + ROOT_WEB_NODE_CLASS + " WHERE " + ROOT_WEB_NODE_HOST_NAME + " = '" + host + "' fetchplan *:0")).execute();
        for (Vertex v: vertices) {
            vertex = v;
            break;
        }
        if (vertex != null) {
            retVal = new OWebNode(vertex);
        }
        return retVal;
    }

    /*
 * Create a new node in the database with a single path, starting from a node
 * @param from   the node to start from
 * @param path   a string that defines a single connection (path) between two nodes
 * @param locale the locale from the path
 * */

    @Override
    public WebNode createNode(WebNode from, String path, Locale locale) {
        OrientGraph graph = getGraph();
        Vertex v = graph.addVertex("class:" + WEB_NODE_CLASS);
        OWebNode retVal = new OWebNode(v);
        retVal.setStatusCode(404);
        Edge e = graph.addEdge(null, ((OWebNode)from).getVertex(), v, PATH_CLASS_NAME);
        WebPath webPath = new OWebPath(e);
        webPath.setName(path, locale);
        graph.commit();
        graph.begin();
        return retVal;
    }


    @Override
    public Resource createResource(URI id, URI rdfType, Locale language)
    {
        Vertex v = new OrientVertex();

        Resource retVal = null;
        ODocument defaultVertex = getVertexWithBlockId(id);
        if (defaultVertex == null) {
            defaultVertex = createDefaultVertex(id, rdfType);
        } else {
            Logger.debug("Use found document");
        }
        if (language == null || language.equals(Locale.ROOT)) language = BlocksConfig.instance().getDefaultLanguage();
        ODocument localized = getOrCreateLocalizedVertex(defaultVertex, language);
        retVal = new OrientResource(defaultVertex, localized);

        return retVal;
    }

    @Override
    public Resource getResource(URI id, Locale language)
    {
        Resource retVal = null;
        ODocument defaultVertex = getVertexWithBlockId(id);
        if (defaultVertex != null) {
            ODocument localized = getOrCreateLocalizedVertex(defaultVertex, language);
            retVal = new OrientResource(defaultVertex, localized);
        }
        return retVal;
    }

    @Override
    public Resource saveResource(Resource resource) {
        return resource;
    }

    @Override
    public Resource deleteResource(Resource resource) {
        return resource;
    }


    @Override
    public Node createNode(Object value, Locale language)
    {
        Node retVal = null;
        if (value instanceof ODocument) {
            retVal = this.asResource((ODocument)value, language);
        } else {
            retVal = new OrientNode(value, language);
        }

        return retVal;
    }



    // ------- PROTECTED METHODS ---------
    public ODocument getOrCreateLocalizedVertex(ODocument defaultVertex, Locale language) {
        ODocument retVal = getLocalizedVertexForDefault(defaultVertex, language);
        if (retVal == null) {
            // If resource does not yet exist, create it
            retVal = createLocalizedVertex(defaultVertex, language);
        }
        return retVal;
    }

    public ODocument createLocalizedVertex(ODocument defaultVertex, Locale language) {
        ODocument retVal = new ODocument(LOCALIZED_RESOURCE_CLASS);
        retVal.field(ParserConstants.JSONLD_LANGUAGE, language);
        defaultVertex.field(localizedVersionField(language.getLanguage()), retVal);
        return retVal;
    }


    public ODocument createDefaultVertex(URI id, URI rdfType) {
        if (id == null) throw new NullPointerException();

        ODocument vertex = new ODocument(RESOURCE_CLASS);
        if (rdfType != null) vertex.field(ParserConstants.JSONLD_TYPE, rdfType);
        vertex.field(ParserConstants.JSONLD_ID, id.toString());
        return vertex;
    }


    // ------- PRIVATE METHODS ---------



    private ODocument getVertexWithBlockId(URI id)
    {
        ODocument retVal = null;
        Iterable<ODocument> docs = getDatabase().command(new OSQLSynchQuery<ODocument>("select FROM " + RESOURCE_CLASS + " WHERE " + ParserConstants.JSONLD_ID + " = '" + id.toString() +"'")).execute();
        if (docs != null) {
            Iterator<ODocument> iterator = docs.iterator();
            if (iterator.hasNext()) {
                retVal = iterator.next();
            }
        }
        return retVal;
    }

    protected ODocument getLocalizedVertexForDefault(ODocument defaultVertex, Locale language) {
        ODocument retVal = defaultVertex.field(localizedVersionField(language.getLanguage()));
        return retVal;
    }

    private Resource asResource(ODocument vertex, Locale language)
    {
        ODocument defaultResource = vertex;
        Resource retVal = null;
        if (vertex.getClassName() !=null && vertex.getClassName().equals(RESOURCE_CLASS)) {
            ODocument localized = getOrCreateLocalizedVertex(defaultResource, language);
            retVal = new OrientResource(defaultResource, localized);
        } else {
            retVal = new OrientResource(defaultResource, null);
        }
        return retVal;
    }

    // TODO Th
    public OrientGraph getGraph()
    {
        if (this.graphFactory == null) {
            this.graphFactory = new OrientGraphFactory("remote:/localhost/cms", "admin", "admin").setupPool(1, 10);
        }

        if (this.graph == null) {
            final OrientGraph  db = graphFactory.getTx();
            ODatabaseRecordThreadLocal.INSTANCE.set(db.getRawGraph());
            this.graph = new ThreadLocal<OrientGraph>() {
                protected OrientGraph initialValue() {
                    return db;
                }
            };
        }
        return this.graph.get();
    }

    public ODatabaseDocumentTx getDatabase()
    {
        OrientGraph graph = getGraph();
        ODatabaseRecordThreadLocal.INSTANCE.set(getGraph().getRawGraph());
        return graph.getRawGraph();
    }


    private void createSchemaOnStartup(OrientGraph graph) {
        if (graph.getVertexType(RESOURCE_CLASS) == null) {
            OrientVertexType defaultResourceClass = graph.createVertexType(RESOURCE_CLASS);
            defaultResourceClass.createProperty(ParserConstants.JSONLD_ID, OType.STRING);
            defaultResourceClass.createProperty(RESOURCE_TYPE_FIELD, OType.EMBEDDEDSET);
            graph.createIndex(RESOURCE_TYPE_FIELD, Vertex.class, new Parameter("class", RESOURCE_CLASS));
            graph.createIndex(ParserConstants.JSONLD_ID, Vertex.class, new Parameter("type", "UNIQUE"), new Parameter("class", RESOURCE_CLASS));
        }

        if (graph.getVertexType(LOCALIZED_RESOURCE_CLASS) == null) {
            OrientVertexType localizedResourceClass = graph.createVertexType(LOCALIZED_RESOURCE_CLASS);
            localizedResourceClass.createProperty(ParserConstants.JSONLD_LANGUAGE, OType.STRING);
        }

        if (graph.getVertexType(WEB_NODE_CLASS) == null) {
            OrientVertexType nodeClass = graph.createVertexType(WEB_NODE_CLASS);
            nodeClass.createProperty(WEB_NODE_STATUS_FIELD, OType.INTEGER);
            nodeClass.createProperty(WEB_NODE_PAGE_FIELD, OType.STRING);
            graph.createIndex(WEB_NODE_PAGE_FIELD, Vertex.class, new Parameter("class", WEB_NODE_CLASS));
            graph.createIndex(WEB_NODE_STATUS_FIELD, Vertex.class, new Parameter("class", WEB_NODE_CLASS));
        }

        if (graph.getVertexType(ROOT_WEB_NODE_CLASS) == null) {
            OrientVertexType rootNodeClass = graph.createVertexType(ROOT_WEB_NODE_CLASS, WEB_NODE_CLASS);
            rootNodeClass.createProperty(ROOT_WEB_NODE_HOST_NAME, OType.STRING);
            graph.createIndex(ROOT_WEB_NODE_HOST_NAME, Vertex.class, new Parameter("class", ROOT_WEB_NODE_CLASS));
        }

        if (graph.getEdgeType(PATH_CLASS_NAME) == null) {
            OrientEdgeType pathClass = graph.createEdgeType(PATH_CLASS_NAME);
            pathClass.createProperty(PATH_NAME_FIELD, OType.STRING);
            graph.createIndex(PATH_NAME_FIELD, Vertex.class, new Parameter("class", PATH_CLASS_NAME));
            for (Locale locale: BlocksConfig.instance().getLanguages().values()) {
                String field = getLocalizedNameField(locale);
                pathClass.createProperty(field, OType.STRING);
                graph.createIndex(field, Vertex.class, new Parameter("class", PATH_CLASS_NAME));
            }
        }

    }

    /*
    * Fieldname of the localized version of a resource
    * */
    private String localizedVersionField(String language) {
        return RESOURCE_LOCALIZED_FIELD + "_" + language;
    }

    // STATIC METHODS

    // TODO give this a better place
    public static String getLocalizedNameField(Locale locale) {
        String retVal = PATH_NAME_FIELD;
        if (!locale.equals(Locale.ROOT)) {
            retVal = locale.getLanguage() + "_" + PATH_NAME_FIELD;
        }
        return retVal;
    }

}

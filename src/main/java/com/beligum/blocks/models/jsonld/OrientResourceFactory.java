package com.beligum.blocks.models.jsonld;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.models.jsonld.interfaces.Node;
import com.beligum.blocks.models.jsonld.interfaces.Resource;
import com.beligum.blocks.models.jsonld.interfaces.ResourceFactory;
import com.beligum.blocks.models.jsonld.jackson.ResourceJsonDeserializer;
import com.beligum.blocks.models.jsonld.jackson.ResourceJsonLDDeserializer;
import com.beligum.blocks.models.jsonld.jackson.ResourceJsonLDSerializer;
import com.beligum.blocks.models.jsonld.jackson.ResourceJsonSerializer;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTxPooled;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 13/05/15.
 */
public class OrientResourceFactory implements ResourceFactory
{
    private static OrientResourceFactory instance;


    private ODatabaseDocumentPool dbPool;
    private OrientGraphFactory graphFactory;
    private ThreadLocal<OrientGraph> graph;
    private OServer server;
    private ObjectMapper mapper;


    private OServer startServer() throws Exception
    {
        OServer server = OServerMain.create();
        server.startup(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<orient-server>"
                        + "<network>"
                        + "<protocols>"
                        + "<protocol name=\"binary\" implementation=\"com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary\"/>"
                        + "<protocol name=\"http\" implementation=\"com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb\"/>"
                        + "</protocols>"
                        + "<listeners>"
                        + "<listener ip-address=\"0.0.0.0\" port-range=\"2424-2430\" protocol=\"binary\"/>"
                        + "<listener ip-address=\"0.0.0.0\" port-range=\"2480-2490\" protocol=\"http\"/>"
                        + "</listeners>"
                        + "</network>"
                        + "<users>"
                        + "<user name=\"root\" password=\"admin\" resources=\"*\"/>"
                        + "</users>"
                        + "<properties>"
//                        + "<entry name=\"orientdb.www.path\" value=\"/Users/wouter/orientDB/www/\"/>"
                        + "<entry name=\"orientdb.config.file\" value=\"/Users/wouter/orientDB/config/orientdb-server-config.xml\"/>"
                        + "<entry name=\"server.cache.staticResources\" value=\"false\"/>"
                        + "<entry name=\"log.console.level\" value=\"info\"/>"
                        + "<entry name=\"log.file.level\" value=\"fine\"/>"
                        + "<entry name=\"/Users/wouter/orientDB/databases\" value=\"server.database.path\"/>"
                        //The following is required to eliminate an error or warning "Error on resolving property: ORIENTDB_HOME"
                        + "<entry name=\"plugin.dynamic\" value=\"false\"/>"
                        + "</properties>" + "</orient-server>");
        server.activate();
        return server;

    }

    public void stopServer() {
        if (this.server != null) {
            server.shutdown();
        }
    }

    // We define a partial schema for the default properties of a resource
    private OrientResourceFactory() {

        try {
//            this.server = startServer();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        ODatabaseDocumentTx graph = (ODatabaseDocumentTx)this.getGraph();

//        if (graph.getVertexType(Resource.ABSTRACT_CLASS) == null) {
//            OrientVertexType abstractResourceClass = graph.createVertexType(Resource.ABSTRACT_CLASS);
//        }
//
//        if (graph.getVertexType(Resource.DEFAULT_CLASS) == null) {
//            OrientVertexType defaultResourceClass = graph.createVertexType(Resource.DEFAULT_CLASS, Resource.ABSTRACT_CLASS);
//            defaultResourceClass.createProperty(Resource.ID, OType.STRING);
//            defaultResourceClass.createProperty(Resource.TYPE, OType.EMBEDDEDSET);
//            graph.createIndex(Resource.TYPE, Vertex.class, new Parameter("class", Resource.ABSTRACT_CLASS));
//            graph.createIndex(Resource.ID, Vertex.class, new Parameter("type", "UNIQUE"), new Parameter("class", Resource.DEFAULT_CLASS));
//        }
//        if (graph.getVertexType(Resource.LOCALIZED_CLASS) == null) {
//            OrientVertexType localizedResourceClass = graph.createVertexType(Resource.LOCALIZED_CLASS, Resource.ABSTRACT_CLASS);
//            localizedResourceClass.createProperty(Resource.LANGUAGE, OType.STRING);
//        }
//        if (graph.getVertexType(Resource.VERSIONED_CLASS) == null) {
//            graph.createVertexType(Resource.VERSIONED_CLASS, Resource.ABSTRACT_CLASS);
//        }

        final SimpleModule module = new SimpleModule("customerSerializationModule", new Version(1, 0, 0, "static version"));
        module.addSerializer(Resource.class, new ResourceJsonSerializer());
        module.addDeserializer(Resource.class, new ResourceJsonDeserializer());

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(module);
        this.mapper = objectMapper;


    }

    public static OrientResourceFactory instance() {
        if (OrientResourceFactory.instance == null) {
            OrientResourceFactory.instance = new OrientResourceFactory();
        }
        return OrientResourceFactory.instance;
    }

    public ODatabaseDocumentTx getGraph()
    {   if (this.graphFactory == null) {
//        this.graphFactory = new OrientGraphFactory("plocal:/Users/wouter/orientDB/databases/cms", "admin", "admin").setupPool(1, 10);
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
        ODatabaseRecordThreadLocal.INSTANCE.set(this.graph.get().getRawGraph());
        return this.graph.get().getRawGraph();
    }

    public ODatabaseDocument getDatabase()
    {
        return ODatabaseDocumentPool.global().acquire("remote:/localhost/cms", "admin", "admin");

    }


    public Resource createResource(String id, String rdfType, Locale language)
    {
        Resource retVal = null;
        ODocument defaultVertex = getVertexWithBlockId(id);
        if (defaultVertex == null) {
            defaultVertex = createDefaultVertex(id, rdfType);
        } else {
            Logger.debug("Use found document");
        }
        if (language == null || language.equals(Locale.ROOT)) language = Blocks.config().getDefaultLanguage();
        ODocument localized = getOrCreateLocalizedVertex(defaultVertex, language);
        retVal = new OrientResource(defaultVertex, localized);

        return retVal;
    }

    public Resource createResource(String id, Locale language)
    {
        return createResource(id, null, language);
    }




    public Resource asResource(ODocument vertex, Locale language)
    {
        ODocument defaultResource = vertex;
        Resource retVal = null;
        if (vertex.getClassName().equals(Resource.DEFAULT_CLASS)) {
            ODocument localized = getOrCreateLocalizedVertex(defaultResource, language);
            retVal = new OrientResource(defaultResource, localized);
        }
        return retVal;
    }

    @Override
    public Node asNode(Boolean value, Locale language)
    {
        return new OrientNode(value, language);
    }
    @Override
    public Node asNode(String value, Locale language)
    {
        return new OrientNode(value, language);
    }
    @Override
    public Node asNode(Integer value, Locale language)
    {
        return new OrientNode(value, language);
    }
    @Override
    public Node asNode(Long value, Locale language)
    {
        return new OrientNode(value, language);
    }
    @Override
    public Node asNode(Double value, Locale language)
    {
        return new OrientNode(value, language);
    }
    @Override
    public Node asNode(List value, Locale language)
    {
        return new OrientNode(value, language);
    }

    @Override
    public Node asNode(Object value, Locale language)
    {
        return new OrientNode(value, language);
    }

    public Resource getResourceWithBlockId(String id, Locale language)
    {
        Resource retVal = null;
        ODocument defaultVertex = getVertexWithBlockId(id);
        if (defaultVertex != null) {
            ODocument localized = getOrCreateLocalizedVertex(defaultVertex, language);
            retVal = new OrientResource(defaultVertex, localized);
        }
        return retVal;
    }

    public void save(Resource resource)
    {
        ((OrientResource)resource).getDefaultVertex().save();
    }

    @Override
    public String toJson(Resource resource)
    {
        String retVal = null;
        try {
            retVal = this.mapper.writeValueAsString(resource);
        } catch (Exception e) {
            Logger.error("Could not serialize resource.");
        }
        return retVal;
    }



    public ODocument getOrCreateLocalizedVertex(ODocument defaultVertex, Locale language) {
        ODocument retVal = getLocalizedVertexForDefault(defaultVertex, language);
        if (retVal == null) {
            // If resource does not yet exist, create it
            retVal = createLocalizedVertex(defaultVertex, language);
        }
        return retVal;
    }

    public ODocument createLocalizedVertex(ODocument defaultVertex, Locale language) {
        ODocument retVal = new ODocument(Resource.LOCALIZED_CLASS);
        retVal.field(Resource.LANGUAGE, language);
        defaultVertex.field(localizedVersion(language.getLanguage()), retVal);
        return retVal;
    }


    public ODocument createDefaultVertex(String id, String rdfType) {
        if (id == null) throw new NullPointerException();

        ODocument vertex = new ODocument(Resource.DEFAULT_CLASS);
        if (rdfType != null) vertex.field(Resource.TYPE, rdfType);
        vertex.field(Resource.ID, id);
        return vertex;
    }


    // ---------------- PRIVATE METHODS -------------------------------

    private ODocument getVertexWithBlockId(String id)
    {
        ODocument retVal = null;
        Iterable<ODocument> docs = getDatabase().command(new OSQLSynchQuery<ODocument>("select FROM " + Resource.DEFAULT_CLASS + " WHERE " + Resource.ID + " = '" + id));
        if (docs != null) {
            Iterator<ODocument> iterator = docs.iterator();
            if (iterator.hasNext()) {
                retVal = iterator.next();
            }
        }
        return retVal;
    }



    protected ODocument getLocalizedVertexForDefault(ODocument defaultVertex, Locale language) {
        ODocument retVal = defaultVertex.field(localizedVersion(language.getLanguage()));

        return retVal;
    }




    private String localizedVersion(String language) {
        return Resource.LOCALIZED + "_" + language;
    }


}

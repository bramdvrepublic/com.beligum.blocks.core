package com.beligum.blocks.controllers;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.beligum.blocks.models.resources.jackson.ResourceJsonDeserializer;
import com.beligum.blocks.models.resources.jackson.ResourceJsonSerializer;
import com.beligum.blocks.models.resources.orient.OrientResourceFactory;
import com.beligum.blocks.routing.nodes.ORouteNodeFactory;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.Parameter;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

import java.util.Locale;

/**
 * Created by wouter on 1/06/15.
 */
public class OrientResourceController
{

    private static OrientResourceController instance;


    private OrientGraphFactory graphFactory;
    private ThreadLocal<OrientGraph> graph;
    private ObjectMapper mapper;

    protected OrientResourceController() {

        OrientGraph graph = this.getGraph();

        if (graph.getVertexType(OrientResourceFactory.DEFAULT_CLASS) == null) {
            OrientVertexType defaultResourceClass = graph.createVertexType(OrientResourceFactory.DEFAULT_CLASS);
            defaultResourceClass.createProperty(ParserConstants.JSONLD_ID, OType.STRING);
            defaultResourceClass.createProperty(OrientResourceFactory.TYPE_FIELD, OType.EMBEDDEDSET);
            graph.createIndex(OrientResourceFactory.TYPE_FIELD, Vertex.class, new Parameter("class", OrientResourceFactory.DEFAULT_CLASS));
            graph.createIndex(ParserConstants.JSONLD_ID, Vertex.class, new Parameter("type", "UNIQUE"), new Parameter("class", OrientResourceFactory.DEFAULT_CLASS));
        }

        if (graph.getVertexType(OrientResourceFactory.LOCALIZED_CLASS) == null) {
            OrientVertexType localizedResourceClass = graph.createVertexType(OrientResourceFactory.LOCALIZED_CLASS);
            localizedResourceClass.createProperty(ParserConstants.JSONLD_LANGUAGE, OType.STRING);
        }

        if (graph.getVertexType(ORouteNodeFactory.NODE_CLASS) == null) {
            OrientVertexType nodeClass = graph.createVertexType(ORouteNodeFactory.NODE_CLASS);
            nodeClass.createProperty(ORouteNodeFactory.STATUS_FIELD, OType.INTEGER);
            nodeClass.createProperty(ORouteNodeFactory.PAGE_FIELD, OType.STRING);
            graph.createIndex(ORouteNodeFactory.PAGE_FIELD, Vertex.class, new Parameter("class", ORouteNodeFactory.NODE_CLASS));
            graph.createIndex(ORouteNodeFactory.STATUS_FIELD, Vertex.class, new Parameter("class", ORouteNodeFactory.NODE_CLASS));
        }

        if (graph.getVertexType(ORouteNodeFactory.ROOT_NODE_CLASS) == null) {
            OrientVertexType rootNodeClass = graph.createVertexType(ORouteNodeFactory.ROOT_NODE_CLASS, ORouteNodeFactory.NODE_CLASS);
            rootNodeClass.createProperty(ORouteNodeFactory.ROOT_HOST_NAME, OType.STRING);
            graph.createIndex(ORouteNodeFactory.ROOT_HOST_NAME, Vertex.class, new Parameter("class", ORouteNodeFactory.ROOT_NODE_CLASS));
        }

        if (graph.getEdgeType(ORouteNodeFactory.PATH_CLASS_NAME) == null) {
            OrientVertexType pathClass = graph.createVertexType(ORouteNodeFactory.PATH_CLASS_NAME);
            pathClass.createProperty(ORouteNodeFactory.NAME_FIELD, OType.STRING);
            graph.createIndex(ORouteNodeFactory.NAME_FIELD, Vertex.class, new Parameter("class", ORouteNodeFactory.PATH_CLASS_NAME));
            for (Locale locale: BlocksConfig.instance().getLanguages().values()) {
                String field = ORouteNodeFactory.getLocalizedNameField(locale);
                pathClass.createProperty(field, OType.STRING);
                graph.createIndex(field, Vertex.class, new Parameter("class", ORouteNodeFactory.PATH_CLASS_NAME));
            }
        }

        final SimpleModule module = new SimpleModule("customerSerializationModule", new Version(1, 0, 0, "static version"));
        module.addSerializer(Resource.class, new ResourceJsonSerializer());
        module.addDeserializer(Resource.class, new ResourceJsonDeserializer());

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(module);
        this.mapper = objectMapper;
    }

    public static OrientResourceController instance() {
        if (OrientResourceController.instance == null) {
            OrientResourceController.instance = new OrientResourceController();
        }
        return OrientResourceController.instance;
    }


    public ODatabaseDocumentTx getDatabase()
    {
        OrientGraph graph = getGraph();
        ODatabaseRecordThreadLocal.INSTANCE.set(getGraph().getRawGraph());
        return graph.getRawGraph();
    }

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


}

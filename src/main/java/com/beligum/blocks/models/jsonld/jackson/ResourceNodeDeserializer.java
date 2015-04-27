package com.beligum.blocks.models.jsonld.jackson;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.jsonld.JsonLDContext;
import com.beligum.blocks.models.jsonld.JsonLDGraph;
import com.beligum.blocks.models.jsonld.ResourceNode;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Created by wouter on 27/04/15.
 */
public class ResourceNodeDeserializer extends JsonDeserializer<ResourceNode>
{

    public ResourceNode deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException
    {
        // We get a graph and a context
        // expand the context
        // build the tree from the graph
        JsonLDGraph graph = null;
        JsonNode treeNode = jp.getCodec().readTree(jp);
        JsonLDContext context = null;
        JsonNode graphNode = null;
        if (treeNode.isArray()) {
            graphNode = treeNode;
        } else if (treeNode.isObject() && treeNode.has(ParserConstants.JSONLD_GRAPH)) {
            graphNode = treeNode.get(ParserConstants.JSONLD_GRAPH);
            if (treeNode.has(ParserConstants.JSONLD_CONTEXT)) {
                context = new JsonLDContext(treeNode.get(ParserConstants.JSONLD_CONTEXT));
            }

            graph = new JsonLDGraph(graphNode, context);

        } else if (treeNode.isObject() && treeNode.has(ParserConstants.JSONLD_ID)) {
            graphNode = treeNode;
            if (treeNode.has(ParserConstants.JSONLD_CONTEXT)) {
                context = new JsonLDContext(treeNode.get(ParserConstants.JSONLD_CONTEXT));
            }
            graph = new JsonLDGraph(graphNode, context);

        }  else {
            throw new IOException("Expected an object or list for jsonld object. Scalar found");
        }



        return graph.getFirstResource();
    }


}

package com.beligum.blocks.models.jsonld.jackson;

import com.beligum.base.server.RequestContext;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.jsonld.JsonLDContext;
import com.beligum.blocks.models.jsonld.JsonLDGraph;
import com.beligum.blocks.models.jsonld.interfaces.Resource;
import com.beligum.blocks.utils.UrlTools;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Locale;

/**
 * Created by wouter on 27/04/15.
 */
public class ResourceJsonLDDeserializer extends JsonDeserializer<Resource>
{

    public Resource deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException
    {
        // We get a graph and a context
        // expand the context
        // build the tree from the graph
        Resource retVal = null;
        JsonLDGraph graph = null;
        JsonNode treeNode = jp.getCodec().readTree(jp);
        JsonLDContext context = null;

        Locale language  = UrlTools.getLanguage(RequestContext.getJaxRsRequest().getUriInfo().getRequestUri());
        if (language != null) {
            language = Blocks.config().getDefaultLanguage();
        }

        JsonNode graphNode = null;
        if (treeNode.isArray()) {
            graphNode = treeNode;
        } else if (treeNode.isObject() && treeNode.has(ParserConstants.JSONLD_GRAPH)) {
            graphNode = treeNode.get(ParserConstants.JSONLD_GRAPH);
            if (treeNode.has(ParserConstants.JSONLD_CONTEXT)) {
                context = new JsonLDContext(treeNode.get(ParserConstants.JSONLD_CONTEXT));
            }
            graph = new JsonLDGraph(graphNode, context, language);
            // TODO find base resource with id and return this

        } else if (treeNode.isObject() && treeNode.has(ParserConstants.JSONLD_ID)) {
            String id = treeNode.get(ParserConstants.JSONLD_ID).asText();
            graphNode = treeNode;
            if (treeNode.has(ParserConstants.JSONLD_CONTEXT)) {
                context = new JsonLDContext(treeNode.get(ParserConstants.JSONLD_CONTEXT));
            }
            graph = new JsonLDGraph(graphNode, context, language);
            retVal = graph.getMainResource(id);
        }  else {
            throw new IOException("Expected an object or list for jsonld object. Scalar found");
        }


        return retVal;
    }


}

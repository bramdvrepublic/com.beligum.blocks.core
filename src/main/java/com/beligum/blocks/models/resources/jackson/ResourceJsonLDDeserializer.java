package com.beligum.blocks.models.resources.jackson;

import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.resources.JsonLDGraph;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.beligum.blocks.routing.nodes.ORouteNodeFactory;
import com.beligum.blocks.routing.RouteImpl;
import com.beligum.blocks.routing.ifaces.Route;
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
//        JsonLDContext context = null;

        Locale language  = Locale.ROOT;
        try {
            Route route = new RouteImpl(RequestContext.getJaxRsRequest().getUriInfo().getRequestUri(), ORouteNodeFactory.instance());
            language = route.getLocale();
        }
        catch (Exception e) {
            Logger.error("Could not detect current locale, we use Locale.ROOT. This should not happen");
        }

        if (language != null) {
            language = BlocksConfig.instance().getDefaultLanguage();
        }

        JsonNode graphNode = null;
        if (treeNode.isArray()) {
            graphNode = treeNode;
        } else if (treeNode.isObject() && treeNode.has(ParserConstants.JSONLD_GRAPH)) {
            graphNode = treeNode.get(ParserConstants.JSONLD_GRAPH);
            if (treeNode.has(ParserConstants.JSONLD_CONTEXT)) {
//                context = new JsonLDContext(treeNode.get(ParserConstants.JSONLD_CONTEXT));
            }
            graph = new JsonLDGraph(graphNode, language);
            // TODO find base resource with id and return this

        } else if (treeNode.isObject() && treeNode.has(ParserConstants.JSONLD_ID)) {
            String id = treeNode.get(ParserConstants.JSONLD_ID).asText();
            graphNode = treeNode;
            if (treeNode.has(ParserConstants.JSONLD_CONTEXT)) {
//                context = new JsonLDContext(treeNode.get(ParserConstants.JSONLD_CONTEXT));
            }
            graph = new JsonLDGraph(graphNode, language);
            retVal = graph.getMainResource(id);
        }  else {
            throw new IOException("Expected an object or list for resources object. Scalar found");
        }


        return retVal;
    }


}

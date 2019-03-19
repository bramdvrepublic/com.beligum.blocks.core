package com.beligum.blocks.rdf.mappers;

import com.beligum.base.utils.Logger;
import com.beligum.base.utils.json.Json;
import com.beligum.blocks.filesystem.index.LucenePageIndexer;
import com.beligum.blocks.filesystem.index.entries.RdfIndexer;
import com.beligum.blocks.filesystem.pages.PageModel;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfMapper;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfResource;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.rdf4j.model.*;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class DefaultRdfMapper implements RdfMapper
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final ObjectMapper jsonMapper;
    private Map<String, PageModel> subModels;

    //-----CONSTRUCTORS-----
    public DefaultRdfMapper()
    {
        this.jsonMapper = Json.getObjectMapper();
    }

    //-----PUBLIC METHODS-----
    @Override
    public JsonNode toJson(Page page) throws IOException
    {
        ObjectNode retVal = null;

        Model pageRdfModel = page.readRdfModel();

        //getting the resource address is slowly implemented (for now), so make sure we cache it
        URI pageResource = page.getAbsoluteResourceAddress();

        //this will hold a sub-object for all different subjects in the model
        Map<URI, ObjectNode> subObjects = new LinkedHashMap<>();
        //this will hold which predicate links the sub-objects to the main object (retVal)
        Map<URI, RdfProperty> subObjectMapping = new LinkedHashMap<>();

        for (Statement triple : pageRdfModel) {

            if (triple.getSubject() instanceof IRI) {

                URI subject = RdfTools.iriToUri((IRI)triple.getSubject());

                //check if this statement is about the main resource of the page
                boolean isMain = subject.equals(pageResource);

                //create a new sub-object if we need to
                ObjectNode node = subObjects.get(subject);
                if (!subObjects.containsKey(subject)) {
                    subObjects.put(subject, node = this.jsonMapper.createObjectNode());
                    if (isMain) {
                        retVal = node;
                    }
                }

                RdfProperty predicate = RdfFactory.lookup(triple.getPredicate(), RdfProperty.class);
                if (predicate != null) {

                    Value value = triple.getObject();

                    //put the value in the json object
                    node.put(predicate.getCurie().toString(), value.stringValue());

                    //if the value is a resource, store it, we'll use it later to hook the subobjects to the main retVal
                    if (value instanceof IRI || predicate.getDataType().equals(XSD.anyURI)) {
                        subObjectMapping.put(URI.create(value.stringValue()), predicate);
                    }
                }
                else {
                    Logger.error("Encountered an unknown RDF predicate while mapping to JSON; " + triple);
                }
            }
            else {
                throw new IOException("Encountered a subject that's not an IRI; this shouldn't happen; " + triple);
            }
        }

        //        this.subModels = RdfTools.extractRdfModels(page);
        //        if (this.subModels.isEmpty()) {
        //            throw new IOException("Page (sub) model generation yielded an empty set; this shouldn't happen since it should always contain at least one model: the main one");
        //        }
        //
        //        //this will use the same keys as the subModels map: the relative path of the subject (right of domain)
        //        Map<String, ObjectNode> subObjects = new LinkedHashMap<>();
        //
        //        for (Map.Entry<String, PageModel> e : this.subModels.entrySet()) {
        //
        //            String modelId = e.getKey();
        //            PageModel model = e.getValue();
        //
        //            ObjectNode subObj = this.jsonMapper.createObjectNode();
        //            subObjects.put(modelId, subObj);
        //
        //            for (Statement stmt : model.getSubModel()) {
        //
        //                RdfResource predicate = RdfFactory.lookup(stmt.getPredicate());
        //
        //                if (predicate != null) {
        //
        //                    Value value = stmt.getObject();
        //
        //                    subObj.put(predicate.getCurie().toString(), value.stringValue());
        //                }
        //
        ////                URI predicateCurie = RdfTools.fullToCurie(URI.create(stmt.getPredicate().toString()));
        ////                if (predicateCurie != null) {
        ////                    RdfProperty predicate = RdfFactory.getProperty(predicateCurie);
        ////                    if (predicate != null) {
        ////
        ////                        //This performs the main indexing operation:
        ////                        // ask the RDF property to index itself to the lucene index
        ////                        RdfIndexer.IndexResult value = predicate.indexValue(rdfIndexer, subModel.getSubResource(), stmt.getObject(), subModel.getPage().getLanguage(), options);
        ////
        ////                        //index it with the default analyzer so we can search it lowercase, without punctuation, etc...
        ////                        rdfIndexer.indexStringField(LucenePageIndexer.CUSTOM_FIELD_ALL, value.stringValue);
        ////
        ////                        //also index the raw value to the constant all field
        ////                        rdfIndexer.indexConstantField(LucenePageIndexer.CUSTOM_FIELD_ALL_VERBATIM, value.indexValue.toString());
        ////
        ////                        Set<String> sortField = sortFieldMapping.get(predicate);
        ////                        if (sortField == null) {
        ////                            sortFieldMapping.put(predicate, sortField = new LinkedHashSet<>());
        ////                        }
        ////                        //makes sense to sort on the human-readable value (eg. 'Belgium' instead of '/resource/Country/1938216')
        ////                        String sortValue = value.stringValue;
        ////                        //this was introduced after experiencing a "java.lang.IllegalArgumentException: DocValuesField "mot:text" is too large, must be <= 32766"
        ////                        //see https://issues.apache.org/jira/browse/LUCENE-4583
        ////                        //Makes sense to crop this to a reasonable value since sorting on thousand-characters-long sort values is't really a valid use case, right?
        ////                        final int MAX_SORT_VALUE_LENGTH = 128;
        ////                        if (sortValue.length() > MAX_SORT_VALUE_LENGTH) {
        ////                            sortValue = sortValue.substring(0, MAX_SORT_VALUE_LENGTH);
        ////                        }
        ////                        sortField.add(sortValue);
        ////                    }
        ////                    else {
        ////                        Logger.error("Encountered unknown RDF predicate (" + predicateCurie + "); this probably means something is wrong or something won't get indexed; " + stmt);
        ////                    }
        ////                }
        ////                else {
        ////                    Logger.error("Unable to build RDF curie from predicate (" + stmt.getPredicate() + "); this probably means something is wrong or something won't get indexed; " + stmt);
        ////                }
        //            }
        //
        //            if (model.isMain()) {
        //                retVal = subObj;
        //            }
        //            else {
        //                //TODO
        //            }
        //        }

        Logger.info(this.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(retVal));

        if (true) throw new RuntimeException("DEBUG");

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.base.utils.Logger;
import com.beligum.base.utils.json.Json;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.filesystem.index.solr.SolrConfigs;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.RDFS;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonPageIndexEntry extends AbstractPageIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private ObjectMapper jsonMapper;
    private ObjectNode rootNode;

    //-----CONSTRUCTORS-----
    public JsonPageIndexEntry(Page page) throws IOException
    {
        super(generateId(page));

        this.jsonMapper = Json.getObjectMapper();
        this.parse(page);
    }

    //-----PUBLIC METHODS-----
    @Override
    public String toString()
    {
        String retVal = null;

        try {
            retVal = this.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.rootNode);
        }
        catch (JsonProcessingException e) {
            Logger.error("Error while rendering JSON node", e);
        }

        return retVal;
    }
    public byte[] toBytes() throws IOException
    {
        byte[] retVal = null;

        try {
            retVal = this.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this.rootNode);
        }
        catch (JsonProcessingException e) {
            Logger.error("Error while rendering JSON node", e);
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void parse(Page page) throws IOException
    {
        Model pageRdfModel = page.readRdfModel();

        //for now, this is implemented rather inefficient, so make sure we cache it
        URI pageResource = page.getAbsoluteResourceAddress();

        String pageId = page.getPublicRelativeAddress().toString();

        //this will hold a reference to the json-object for all different subjects in the model
        Map<URI, ObjectNode> subObjects = new LinkedHashMap<>();
        //this will hold URIs that are values in the ObjectNode, for the RdfProperty
        Map<URI, Map.Entry<ObjectNode, RdfProperty>> subObjectMapping = new LinkedHashMap<>();

        for (Statement triple : pageRdfModel) {

            if (triple.getSubject() instanceof IRI) {

                URI subject = RdfTools.iriToUri((IRI) triple.getSubject());

                //check if this statement is about the main resource of the page
                boolean isMain = subject.equals(pageResource);

                //create a new sub-object if we need to
                ObjectNode node = subObjects.get(subject);
                if (!subObjects.containsKey(subject)) {

                    // The id of the Solr doc is the relative main URI of the resource.
                    // Note: for pages, it's the public SEO-friendly URI, not the subject!
                    String id = isMain ? pageId : StringFunctions.getRightOfDomain(subject).toString();

                    //create a new object and fill it with the first internal fields like id, etc
                    node = this.initializeObjectFields(this.jsonMapper.createObjectNode(), id);

                    //save the node, mapped to it's subject, so we can look it op when it's referenced from other triples
                    subObjects.put(subject, node);

                    if (isMain) {
                        if (this.rootNode == null) {
                            this.rootNode = node;
                        }
                        else {
                            throw new IOException("Encountered a double main subject initialization situation; this shouldn't happen; " + triple);
                        }
                    }
                }

                RdfProperty predicate = RdfFactory.lookup(triple.getPredicate(), RdfProperty.class);
                if (predicate != null) {

                    Value value = triple.getObject();

                    //put the value in the json object
                    this.putProperty(node, predicate, value.stringValue());

                    // If the value is a resource, store it, we'll use it later to hook subobjects to their parents
                    // This means the value of this triple is possibly a reference to (the subject-URI of) another object
                    // in this model (that might come later) .
                    if (value instanceof IRI || predicate.getDataType().equals(XSD.anyURI)) {
                        subObjectMapping.put(URI.create(value.stringValue()), new AbstractMap.SimpleEntry<>(node, predicate));
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

        //now branch sub-objects into other objects where possible
        for (Map.Entry<URI, ObjectNode> m : subObjects.entrySet()) {

            URI subObjectSubject = m.getKey();
            ObjectNode subObject = m.getValue();

            // This won't necessarily resolve: the keys here in the mapping are the values of properties in other objects
            // Note that the fact it's not present in the map will always happen; it means the model had a subject URI
            // that isn't linked to the main object and this is always the case with our RDFa models because the "rdfa:usesVocabulary" property
            // is attached as a predicate to the public (human readable) page address, but that URI is never attached to the resource URI further down the model
            if (subObjectMapping.containsKey(subObjectSubject)) {

                //this pair holds the object and its property to which we should attach
                Map.Entry<ObjectNode, RdfProperty> mapping = subObjectMapping.get(subObjectSubject);

                //this attaches the subObject to the object in the mapping, using the property in the mapping
                this.putProperty(mapping.getKey(), mapping.getValue(), subObject);
            }
        }

        this.rootNode.put("isParent", true);
        ObjectNode childNode = this.jsonMapper.createObjectNode();
        childNode.put("id", "/en/blah/child");
        this.putProperty(childNode, RDFS.label, "subdoc test 1");
        this.rootNode.set("child", childNode);
    }
    private void putProperty(ObjectNode node, RdfProperty predicate, String value)
    {
        node.put(predicate.getCurie().toString(), value);
    }
    private void putProperty(ObjectNode node, RdfProperty predicate, JsonNode object)
    {
        node.set(predicate.getCurie().toString(), object);
    }
    private ObjectNode initializeObjectFields(ObjectNode object, String id)
    {
        object.put(SolrConfigs.CORE_SCHEMA_FIELD_ID, id);

        return object;
    }
}

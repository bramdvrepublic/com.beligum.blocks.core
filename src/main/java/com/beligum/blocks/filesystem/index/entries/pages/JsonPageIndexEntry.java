package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.base.utils.Logger;
import com.beligum.base.utils.json.Json;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexEntry;
import com.beligum.blocks.filesystem.index.solr.SolrConfigs;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.net.URI;
import java.util.*;

public class JsonPageIndexEntry extends AbstractPageIndexEntry
{
    //-----CONSTANTS-----
    public interface JsonNodeVisitor
    {
        String getPathDelimiter();

        void visit(String fieldName, JsonNode fieldValue, String path);
    }

    private static Collection<IndexEntryField> INTERNAL_FIELDS = Sets.newHashSet(IndexEntry.id,
                                                                                 IndexEntry.tokenisedId,
                                                                                 IndexEntry.label,
                                                                                 IndexEntry.description,
                                                                                 IndexEntry.image,
                                                                                 PageIndexEntry.parentId,
                                                                                 PageIndexEntry.resource,
                                                                                 PageIndexEntry.typeOf,
                                                                                 PageIndexEntry.language,
                                                                                 PageIndexEntry.canonicalAddress,
                                                                                 PageIndexEntry.object
    );

    //-----VARIABLES-----
    private ObjectMapper jsonMapper;
    private ObjectNode rootNode;

    //-----CONSTRUCTORS-----
    /**
     * Private constructor: only for serialization
     */
    protected JsonPageIndexEntry()
    {
        super(null);
    }
    public JsonPageIndexEntry(Page page) throws IOException
    {
        super(generateId(page));

        this.jsonMapper = Json.getObjectMapper();
        this.parse(page);
    }

    //-----PUBLIC METHODS-----
    @Override
    public Iterable<IndexEntryField> getInternalFields()
    {
        return INTERNAL_FIELDS;
    }
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
    public void iterateObjectNodes(JsonNodeVisitor visitor) throws IOException
    {
        this.findChildBoundaries(this.rootNode, visitor);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void parse(Page page) throws IOException
    {
        Model pageRdfModel = page.readRdfModel();

        //for now, this is implemented rather inefficient, so make sure we cache it
        //Also, note that URIs in an RDF model are always absolute
        URI pageResource = page.getAbsoluteResourceAddress();

        //this will hold a reference to all json-objects for all different subjects in the model
        Map<URI, ObjectNode> subObjects = new LinkedHashMap<>();
        //this will hold URIs that are values in the ObjectNode, for the RdfProperty
        Map<URI, Map.Entry<ObjectNode, RdfProperty>> subObjectMapping = new LinkedHashMap<>();

        for (Statement triple : pageRdfModel) {

            if (triple.getSubject() instanceof IRI) {

                URI subject = RdfTools.iriToUri((IRI) triple.getSubject());

                //create a new sub-object if we need to
                ObjectNode node = subObjects.get(subject);
                if (!subObjects.containsKey(subject)) {

                    //check if this statement is about the main resource of the page
                    boolean isMainSubject = subject.equals(pageResource);

                    //create a new object and fill it with the first internal fields like id, etc
                    node = this.initializeInternalFields(this.jsonMapper.createObjectNode(), page, subject, isMainSubject);

                    //save the node, mapped to it's subject, so we can look it op when it's referenced from other triples
                    subObjects.put(subject, node);

                    if (isMainSubject) {
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

        //do some validation, the root node should be initialized now, otherwise we have nothing to return
        if (this.rootNode == null) {
            throw new IOException("Encountered an RDF model without a main subject; this shouldn't happen; " + page);
        }

        // Now branch sub-objects into other parent objects where possible
        // Note that, conceptually, this (in theory) doesn't necessarily means we're attaching to the root node
        // We might be branching deeper sub-objects into each other as well.
        // but note that deeper sub-objects that are not "attached" to the root node will be lost, cause that's the only
        // one we're saving as a class field
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
                //Note that it supports both single values and array values, depending on the multiplicity of the field
                this.addProperty(mapping.getKey(), mapping.getValue(), subObject);
            }
        }
    }
    private void findChildBoundaries(JsonNode fieldValue, JsonNodeVisitor visitor) throws IOException
    {
        //standardized initial values so the method below works as expected
        this.findChildBoundaries("", fieldValue, "", visitor);
    }
    private void findChildBoundaries(String fieldName, JsonNode fieldValue, String currentPath, JsonNodeVisitor visitor) throws IOException
    {
        if (fieldValue.isContainerNode()) {

            if (fieldValue.isObject()) {

                //we discovered an object; update the path using the field name of this object
                if (!currentPath.endsWith(visitor.getPathDelimiter())) {
                    currentPath += visitor.getPathDelimiter();
                }
                currentPath += fieldName;

                visitor.visit(fieldName, fieldValue, currentPath);

                Iterator<Map.Entry<String, JsonNode>> fields = fieldValue.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    findChildBoundaries(field.getKey(), field.getValue(), currentPath, visitor);
                }
            }
            else if (fieldValue.isArray()) {

                Iterator<JsonNode> elements = fieldValue.elements();
                while (elements.hasNext()) {
                    findChildBoundaries(fieldName, elements.next(), currentPath, visitor);
                }
            }
            else {
                throw new IOException("Encountered JSON container node that's not an object, nor an array; this shouldn't happen; " + fieldValue);
            }
        }
        else {
            //NOOP: no need to go down, the node is not a container (object or array)
        }
    }
    private String putProperty(ObjectNode node, RdfProperty predicate, String value)
    {
        String fieldName = this.toFieldName(predicate);

        node.put(fieldName, value);

        return fieldName;
    }
    private String putProperty(ObjectNode node, RdfProperty predicate, JsonNode object)
    {
        String fieldName = this.toFieldName(predicate);

        node.set(fieldName, object);

        return fieldName;
    }
    private String addProperty(ObjectNode node, RdfProperty predicate, JsonNode object)
    {
        String fieldName = this.toFieldName(predicate);

        // this will support both array-based subObjects when there are multiple objects mapped on the same field
        // and standard subnode (not in an array) when there's only one.
        JsonNode existingField = node.get(fieldName);
        if (existingField == null) {
            node.set(fieldName, object);
        }
        else {
            //if the existing field is not an array, convert it
            if (!existingField.isArray()) {
                node.remove(fieldName);
                node.putArray(fieldName).add(existingField);
            }

            //note: withArray() will create the array field if it doesn't exist (but that should never happen)
            node.withArray(fieldName).add(object);
        }

        return fieldName;
    }
    private JsonNode getProperty(ObjectNode node, RdfProperty predicate)
    {
        String fieldName = this.toFieldName(predicate);

        //this returns null if no such field exists
        return node.get(fieldName);
    }
    private String toFieldName(RdfProperty predicate)
    {
        return predicate.getCurie().toString();
    }
    private ObjectNode initializeInternalFields(ObjectNode object, Page page, URI subject, boolean mainSubject)
    {
        for (IndexEntryField field : this.getInternalFields()) {
            object.put(field.getName(), field.ge tValue(this));
        }

        // The id of the Solr doc is the relative main URI of the resource.
        // Note: for pages, it's the public SEO-friendly URI, not the subject!
        object.put(SolrConfigs.CORE_SCHEMA_FIELD_ID, mainSubject ? AbstractPageIndexEntry.generateId(page) : AbstractPageIndexEntry.generateId(subject));

        //TODO add the others

        return object;
    }
}

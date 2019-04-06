package com.beligum.blocks.index.entries;

import com.beligum.base.utils.Logger;
import com.beligum.base.utils.json.Json;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.index.ifaces.IndexEntryField;
import com.beligum.blocks.index.ifaces.PageIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceSummarizer;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;

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

    public static final RdfProperty TYPEOF_PROPERTY = RDF.type;
    private static final IRI TYPEOF_PROPERTY_IRI = RdfTools.uriToIri(TYPEOF_PROPERTY.getUri());

    /**
     * Note: this is a bit of an elaborate implementation just to type-sync two methods:
     * - getInternalFields()
     * - initializeInternalFields()
     */
    private static Set<IndexEntryField> INTERNAL_FIELDS = Sets.newHashSet(id,
                                                                          tokenisedId,
                                                                          label,
                                                                          description,
                                                                          image,
                                                                          parentId,
                                                                          resource,
                                                                          typeOf,
                                                                          language,
                                                                          canonicalAddress);

    //-----VARIABLES-----
    private ObjectMapper jsonMapper;
    private ObjectNode jsonNode;
    //private JsonPageIndexEntry parent;

    //-----CONSTRUCTORS-----
    /**
     * Private constructor: only for serialization
     */
    protected JsonPageIndexEntry()
    {
        super();
    }
    protected JsonPageIndexEntry(Page page) throws IOException
    {
        this(PageIndexEntry.generateId(page), page.getPublicAbsoluteAddress(), page.getAbsoluteResourceAddress(), page.getCanonicalAddress(), page.getLanguage(), page.readRdfModel(), null);
    }
    /**
     * To build a JSON node from an RDF model, we also need a root resource URI
     * so we know which subject to treat as the root node. The other subjects in the model
     * will be attached to this root node if it references them, otherwise, they are discarded.
     * Next to a root resource URI, we also need the possibility to set a custom id URI.
     * This is because, eg. for pages, we use the public page address as it's id, instead
     * of the resource id (because it might not be unique since multiple pages can be describing it)
     */
    protected JsonPageIndexEntry(String id, URI absolutePublicPageUri, URI absoluteRootResourceUri, URI canonicalAddress, Locale language, Model rdfModel, JsonPageIndexEntry parent) throws IOException
    {
        super(id);

        this.jsonMapper = Json.getObjectMapper();
        this.parse(absolutePublicPageUri, absoluteRootResourceUri, canonicalAddress, language, parent, rdfModel);
    }
    protected JsonPageIndexEntry(String json) throws IOException
    {
        //note: we don't have an ID yet, see below
        super();

        this.jsonMapper = Json.getObjectMapper();
        this.jsonNode = this.jsonMapper.readValue(json, ObjectNode.class);

        //Load the internal json fields into the local variables
        for (IndexEntryField field : this.getInternalFields()) {
            JsonNode node = this.jsonNode.get(field.getName());
            if (node != null) {
                field.setValue(this, node.textValue());
            }
        }
    }

    //-----PUBLIC METHODS-----
    public JsonPageIndexEntry create(URI absolutePublicPageUri, URI absoluteRootResourceUri, URI canonicalAddress, Locale language, Model rdfModel, JsonPageIndexEntry parent) throws IOException
    {
        return this.create(PageIndexEntry.generateId(absoluteRootResourceUri), absolutePublicPageUri, absoluteRootResourceUri, canonicalAddress, language, rdfModel, parent);
    }
    public JsonPageIndexEntry create(String id, URI absolutePublicPageUri, URI absoluteRootResourceUri, URI canonicalAddress, Locale language, Model rdfModel, JsonPageIndexEntry parent)
                    throws IOException
    {
        return new JsonPageIndexEntry(id, absolutePublicPageUri, absoluteRootResourceUri, canonicalAddress, language, rdfModel, parent);
    }
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
            retVal = this.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.jsonNode);
        }
        catch (JsonProcessingException e) {
            Logger.error("Error while rendering JSON node", e);
        }

        return retVal;
    }
    public ObjectNode getJsonNode()
    {
        return jsonNode;
    }
    public byte[] toBytes() throws IOException
    {
        byte[] retVal = null;

        try {
            retVal = this.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this.jsonNode);
        }
        catch (JsonProcessingException e) {
            Logger.error("Error while rendering JSON node", e);
        }

        return retVal;
    }
    /**
     * General iterator method that visits all objects and sub-objects in the json node tree
     */
    public void iterateObjectNodes(JsonNodeVisitor visitor) throws IOException
    {
        this.findChildBoundaries(this.jsonNode, visitor);
    }

    //-----PROTECTED METHODS-----
    protected JsonField createField(RdfProperty property) throws IOException
    {
        return new JsonField(property);
    }

    //-----PRIVATE METHODS-----
    /**
     * Iterate the entire RDF model and search for the root resource uri as the base RDF submodel to create
     * an JSON node from. Then, iterate all other RDF to see if we can add them as subnodes to the root node.
     */
    private void parse(URI absolutePublicPageUri, URI absoluteRootResourceUri, URI canonicalAddress, Locale language, JsonPageIndexEntry parent, Model rdfModel) throws IOException
    {
        //this will hold a reference to all json-objects for all different subjects in the model
        Map<URI, JsonPageIndexEntry> subObjects = new LinkedHashMap<>();

        //this will hold URIs that are values in the ObjectNode, for the RdfProperty
        Map<URI, Map.Entry<ObjectNode, RdfProperty>> subObjectMapping = new LinkedHashMap<>();

        IRI rootResourceIri = RdfTools.uriToIri(absoluteRootResourceUri);
        if (!rdfModel.subjects().contains(rootResourceIri)) {
            throw new IOException("Couldn't find resource URI in RDF model; " + absoluteRootResourceUri);
        }

        // Now "zoom-in" on the subject and add all RDF properties to the node
        Model rootModel = rdfModel.filter(rootResourceIri, null, null);

        // Extract the type from the graph so we know what we're talking about
        RdfClass type = RdfFactory.lookup(Models.objectIRI(rootModel.filter(rootResourceIri, TYPEOF_PROPERTY_IRI, null)).orElse(null), RdfClass.class);
        if (type == null) {
            throw new IOException("Encountered an RDF model without a type; this shouldn't happen; " + rootModel);
        }

        //create a new object and fill it with the first internal fields like id, etc
        this.jsonNode = this.initializeInternalFields(this.jsonMapper.createObjectNode(), absoluteRootResourceUri, canonicalAddress, language, type, parent, rootModel);

        //save the node, mapped to it's subject, so we can look it op when it's referenced from other triples
        subObjects.put(absoluteRootResourceUri, this);

        for (Statement triple : rootModel) {

            RdfProperty predicate = RdfFactory.lookup(triple.getPredicate(), RdfProperty.class);
            if (predicate != null) {

                Value value = triple.getObject();

                //put the value in the json object
                this.addProperty(this.jsonNode, value, predicate, language);

                // If the value is a resource, store it, we'll use it later to hook subobjects to their parents
                // This means the value of this triple is possibly a reference to (the subject-URI of) another object
                // in this model (that might come later) .
                if (value instanceof IRI || predicate.getDataType().equals(XSD.anyURI)) {
                    subObjectMapping.put(URI.create(value.stringValue()), new AbstractMap.SimpleEntry<>(this.jsonNode, predicate));
                }
            }
            else {
                Logger.error("Encountered an unknown RDF predicate '" + predicate + "' while mapping to JSON. This property will be ignored and excluded from the JSON object (you may want to resolve this); " + triple);
            }
        }

        // The model might have more subjects than just the root resource (eg. sub-objects).
        // so do a recursive call and parse those other submodels to JSON nodes
        for (Resource subjectIri : rdfModel.subjects()) {
            if (!subjectIri.equals(rootResourceIri)) {
                URI subjectUri = RdfTools.iriToUri((IRI) subjectIri);

                //we'll skip the triples that describe the public page since these only describe some marginal RDFa properties,
                //and only get us in trouble later on because they don't have typeOf, etc.
                if (!subjectUri.equals(absolutePublicPageUri)) {
                    //note: this create() is a means to have polymophic constructor (it creates the same new class instance as this instance)
                    //also note that the language of sub objects is always the same as the language of the parent
                    Model subModel = rdfModel.filter(subjectIri, null, null);
                    //note: the canonical address of a subresource is just the relative counterpart of its resource uri, right?
                    JsonPageIndexEntry subEntry = this.create(absolutePublicPageUri, subjectUri, StringFunctions.getRightOfDomain(subjectUri), language, subModel, this);
                    subObjects.put(RdfTools.iriToUri((IRI) subjectIri), subEntry);
                }
            }
        }

        // Now branch sub-objects into other parent objects where possible
        // Note that, conceptually, this (in theory) doesn't necessarily means we're attaching to the root node
        // We might be branching deeper sub-objects into each other as well.
        // but note that deeper sub-objects that are not "attached" to the root node will be lost, cause that's the only
        // one we're saving as a class field
        for (Map.Entry<URI, JsonPageIndexEntry> m : subObjects.entrySet()) {

            URI subObjectSubject = m.getKey();

            // This won't necessarily resolve: the keys here in the mapping are the values of properties in other objects
            // Note that the fact it's not present in the map will always happen; it means the model had a subject URI
            // that isn't linked to the main object and this is always the case with our RDFa models because the "rdfa:usesVocabulary" property
            // is attached as a predicate to the public (human readable) page address, but that URI is never attached to the resource URI further down the model
            if (subObjectMapping.containsKey(subObjectSubject)) {

                JsonPageIndexEntry subNode = m.getValue();

                //this pair holds the object and its property to which we should attach
                Map.Entry<ObjectNode, RdfProperty> mapping = subObjectMapping.get(subObjectSubject);

                //this attaches the subObject to the object in the mapping, using the property in the mapping
                //Note that it supports both single values and array values, depending on the multiplicity of the field
                this.addProperty(mapping.getKey(), subNode.getJsonNode(), mapping.getValue(), language);
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
    private ObjectNode initializeInternalFields(ObjectNode object, URI rootResourceUri, URI canonicalAddress, Locale language, RdfClass type,
                                                JsonPageIndexEntry parent, Model rootModel) throws IOException
    {
        this.setParentId(PageIndexEntry.generateParentId(parent));
        this.setResource(PageIndexEntry.generateResource(rootResourceUri));
        this.setTypeOf(PageIndexEntry.generateTypeOf(type));
        this.setLanguage(PageIndexEntry.generateLanguage(language));
        this.setCanonicalAddress(PageIndexEntry.generateCanonicalAddress(canonicalAddress));

        ResourceSummarizer summarizer = type.getSummarizer();
        if (summarizer != null) {
            ResourceSummarizer.SummarizedResource summary = summarizer.summarize(type, rootModel);
            if (summary != null) {
                this.setLabel(summary.getLabel());
                this.setDescription(summary.getDescription());
                this.setImage(summary.getImage() == null ? null : summary.getImage().toString());
            }
            else {
                throw new IOException("RDF class summarizer returned a null summary; this shouldn't happen; " + type);
            }
        }
        else {
            throw new IOException("Encountered an RDF class with a null summarizer; this shouldn't happen; " + type);
        }

        for (IndexEntryField e : INTERNAL_FIELDS) {
            if (e.hasValue(this)) {
                object.put(e.getName(), e.getValue(this));
            }
            else {
                throw new IOException("Encountered an uninitialized internal field, this probably means some internal fields have changed without updating this method; " + e);
            }
        }

        return object;
    }
    private JsonField addProperty(ObjectNode node, Object value, RdfProperty predicate, Locale language) throws IOException
    {
        JsonField field = this.createField(predicate);

        // this will support both array-based subObjects when there are multiple objects mapped on the same field
        // and standard subnode (not in an array) when there's only one.
        JsonNode existingField = node.get(field.getName());
        if (existingField == null) {
            if (value instanceof JsonNode) {
                node.set(field.getName(), (JsonNode) value);
            }
            else if (value instanceof Value) {
                node.put(field.getName(), field.serialize((Value) value, predicate, language));
            }
            else {
                throw new IOException("Unimplemented value type, this shouldn't happen; "+value);
            }
        }
        else {
            //if the existing field is not an array, convert it to one
            if (!existingField.isArray()) {
                node.remove(field.getName());
                node.putArray(field.getName()).add(existingField);
            }

            //note: withArray() will create the array field if it doesn't exist (but that should never happen)
            if (value instanceof JsonNode) {
                node.withArray(field.getName()).add((JsonNode) value);
            }
            else if (value instanceof Value) {
                node.withArray(field.getName()).add(field.serialize((Value) value, predicate, language));
            }
            else {
                throw new IOException("Unimplemented value type, this shouldn't happen; "+value);
            }
        }

        return field;
    }
}

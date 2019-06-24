package com.beligum.blocks.index.entries;

import com.beligum.base.utils.Logger;
import com.beligum.base.utils.json.Json;
import com.beligum.blocks.config.WidgetType;
import com.beligum.blocks.index.fields.JsonField;
import com.beligum.blocks.index.fields.ResourceTypeField;
import com.beligum.blocks.index.ifaces.*;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;

import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class JsonPageIndexEntry extends AbstractIndexEntry implements PageIndexEntry
{
    //-----CONSTANTS-----
    public interface JsonNodeVisitor
    {
        String getPathDelimiter();

        void visit(String fieldName, JsonNode fieldValue, String path);
    }

    public static final RdfProperty TYPEOF_PROPERTY = RDF.type;
    private static final IRI TYPEOF_PROPERTY_IRI = RdfTools.uriToIri(TYPEOF_PROPERTY.getUri());

    //-----VARIABLES-----
    @XmlTransient
    private ObjectNode jsonNode;

    //-----CONSTRUCTORS-----
    /**
     * Private constructor: only for serialization
     */
    protected JsonPageIndexEntry()
    {
        super();
    }
    /**
     * To build a JSON node from an RDF model, we also need a root resource URI
     * so we know which subject to treat as the root node. The other subjects in the model
     * will be attached to this root node if it references them, otherwise, they are discarded.
     * Next to a root resource URI, we also need the possibility to set a custom id URI.
     * This is because, eg. for pages, we use the public page address as it's id, instead
     * of the resource id (because it might not be unique since multiple pages can be describing it)
     */
    protected JsonPageIndexEntry(URI id, URI absolutePublicPageUri, URI absoluteRootResourceUri, Locale language, Model rdfModel, JsonPageIndexEntry parent) throws IOException
    {
        super(id);

        this.parse(absolutePublicPageUri, absoluteRootResourceUri, language, parent, rdfModel);
    }
    protected JsonPageIndexEntry(String json) throws IOException
    {
        //note: we don't have an ID yet, see below
        super();

        this.jsonNode = Json.getObjectMapper().readValue(json, ObjectNode.class);

        //Load the internal json fields into the local variables
        for (IndexEntryField field : this.getInternalFields()) {
            JsonNode node = this.jsonNode.get(field.getName());
            if (node != null) {
                field.setValue(this, node.textValue());
            }
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean isExternal()
    {
        return false;
    }
    @Override
    public String toString()
    {
        String retVal = null;

        try {
            retVal = Json.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this.jsonNode);
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
            retVal = Json.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(this.jsonNode);
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
    protected Iterable<IndexEntryField> getInternalFields()
    {
        return INTERNAL_FIELDS;
    }
    protected JsonPageIndexEntry create(URI absolutePublicPageUri, URI absoluteRootResourceUri, Locale language, Model rdfModel, JsonPageIndexEntry parent) throws IOException
    {
        return this.create(absoluteRootResourceUri, absolutePublicPageUri, absoluteRootResourceUri, language, rdfModel, parent);
    }
    protected JsonPageIndexEntry create(URI id, URI absolutePublicPageUri, URI absoluteRootResourceUri, Locale language, Model rdfModel, JsonPageIndexEntry parent) throws IOException
    {
        return new JsonPageIndexEntry(id, absolutePublicPageUri, absoluteRootResourceUri, language, rdfModel, parent);
    }
    protected JsonField createField(RdfProperty property) throws IOException
    {
        return new JsonField(property);
    }

    //-----PRIVATE METHODS-----
    /**
     * Iterate the entire RDF model and search for the root resource uri as the base RDF model to create
     * an JSON node from. Then, iterate all other RDF to see if we can add them as subnodes to the root node.
     */
    private void parse(URI absolutePublicPageUri, URI absoluteRootResourceUri, Locale language, JsonPageIndexEntry parentObj, Model rdfModel) throws IOException
    {
        //this will hold a reference to all json-objects for all different subjects in the model
        Map<URI, JsonPageIndexEntry> subObjects = new LinkedHashMap<>();

        IRI rootResourceIri = RdfTools.uriToIri(absoluteRootResourceUri);
        if (!rdfModel.subjects().contains(rootResourceIri)) {
            throw new IOException("Couldn't find resource URI in RDF model; " + absoluteRootResourceUri);
        }

        // Now "zoom-in" on the subject and add all RDF properties to the node
        Model rootModel = rdfModel.filter(rootResourceIri, null, null);

        // Extract the type from the graph so we know what we're talking about
        RdfClass type = this.extractModelTypeof(rootResourceIri, rootModel);
        if (type == null) {
            throw new IOException("Encountered an RDF model without a type; this shouldn't happen; " + rootModel);
        }

        //        if (type.getParentProperty() != null) {
        //            if (parentObj != null) {
        //                throw new IOException("Encountered an RDF class with a configured parent property, but it's parent object is not empty. Don't know which one to choose, please fix this; " + type);
        //            }
        //            else {
        //                Model rdfParentUriModel = rootModel.filter(rootResourceIri, RdfTools.uriToIri(type.getParentProperty().getUri()), null);
        //                if (rdfParentUriModel != null && rdfParentUriModel.size() > 0) {
        //                    if (rdfParentUriModel.size() > 1) {
        //                        throw new IOException("Encountered an RDF parent property which value is more than one; this shouldn't happen; " + rdfParentUriModel);
        //                    }
        //                    Value rdfParentUriValue = rdfParentUriModel.iterator().next().getObject();
        //                    if (rdfParentUriValue instanceof IRI || type.getParentProperty().getDataType().equals(XSD.anyURI)) {
        //                        this.setParentUri(URI.create(rdfParentUriValue.stringValue()));
        //                    }
        //                    else {
        //                        throw new IOException("Encountered an RDF parent property, but it doesn't seem to hold a resource URI value. This is a configuration error and needs to be fixed; " +
        //                                              rdfParentUriModel);
        //                    }
        //                }
        //            }
        //        }

        //create a new object and fill it with the first internal fields like uri, resource, etc
        this.jsonNode = Json.getObjectMapper().createObjectNode();
        this.initializeInternalFields(this.jsonNode, absoluteRootResourceUri, language, type, parentObj, rootModel);

        //save the node, mapped to it's subject, so we can look it op when it's referenced from other triples
        subObjects.put(absoluteRootResourceUri, this);

        // The model might have more subjects than just the root resource (eg. sub-objects).
        // We'll iterate the subjects and check all non-root ones and build a map of sub-nodes first.
        // This will do a recursive call and parse those other submodels to JSON nodes.
        // This way, they're ready to be attached to the root node below
        for (Resource subject : rdfModel.subjects()) {

            IRI subjectIri = (IRI) subject;

            //skip the root resource, we'll parse it in the loop below, we're looking for sub-objects
            if (!subjectIri.equals(rootResourceIri)) {

                URI subjectUri = RdfTools.iriToUri(subjectIri);

                //we'll skip the triples that describe the public page since these only describe some marginal RDFa properties,
                //and only get us in trouble later on because they don't have typeOf, etc.
                if (!subjectUri.equals(absolutePublicPageUri)) {

                    // zoom-in on the subject and recursively call the parser
                    Model subModel = rdfModel.filter(subjectIri, null, null);

                    // Note that we skip the subModels that don't have a typeof, because
                    // regular <a> tags get sereialized to their own subject as well and would crash later on
                    RdfClass subModelType = this.extractModelTypeof(subjectIri, subModel);
                    if (subModelType != null) {
                        // note: this create() is a means to have a polymorphic constructor (it creates the same new class instance as this instance)
                        // also note that the language of sub objects is always the same as the language of the parent
                        subObjects.put(subjectUri, this.create(absolutePublicPageUri, subjectUri, language, subModel, this));
                    }
                }
            }
        }

        // This is the main loop: iterate the root model and connect everything together.
        for (Statement triple : rootModel) {

            RdfProperty property = RdfFactory.lookup(triple.getPredicate(), RdfProperty.class);
            if (property != null && property.getDataType() != null) {

                Value value = triple.getObject();

                JsonField field = this.createField(property);

                // If the value is a resource, we'll store its reference:
                // - if its a sub-resource, the data of it will be in the RDF model and we need to 'instantiate' these in the JSON tree
                //   by saving the reference to the right property now and create + attach the sub-object later on (see below)
                // - if it's a reference to a (local or external) resource that has an endpoint, we'll store some metadata (pulled from the ResourceProxy)
                //   about that resource in this JSON object (eg. for sorting/filtering on the human readable label of the resource instead of its URI)
                // - in all other cases, store it as a literal
                if (value instanceof IRI || property.getDataType().equals(XSD.anyURI)) {

                    URI resourceUri = URI.create(value.stringValue());

                    // check if it's a subresource that was present in the RDF model and already parsed into an object
                    // if so, we'll use that as the "proxy" of this object (which is in fact the entire sub-object, not a proxy)
                    if (subObjects.containsKey(resourceUri)) {

                        ObjectNode subObject = subObjects.get(resourceUri).getJsonNode();

                        // Note: don't set this as a proxy because it's not; it holds the true values of this sub-object
                        subObject.put(ResourceIndexEntry.resourceTypeField.getName(), ResourceIndexEntry.resourceTypeField.SUB_VALUE);

                        this.addProperty(this.jsonNode, subObject, field, language);
                    }
                    // If we have an endpoint, we'll contact it to get a resource proxy and attach that into the JSON node
                    // using a separate "_proxy" suffixed field
                    else if (property.getDataType().getEndpoint() != null) {

                        ResourceProxy resourceProxy = property.getDataType().getEndpoint().getResource(property.getDataType(), resourceUri, language);
                        if (resourceProxy != null) {

                            // convert the ResourceProxy object to a Json node
                            ObjectNode resourceNode = this.copyInternalFields(resourceProxy, Json.getObjectMapper().createObjectNode(), true);

                            if (type.getParentProperty() != null && type.getParentProperty().equals(property)) {
                                if (this.getParentUri() != null) {
                                    throw new IOException(
                                                    "Encountered an RDF class with a configured parent property, but it's parent object is not empty. Don't know which one to choose, please fix this; " +
                                                    type);
                                }
                                else {
                                    this.setParentUri(resourceProxy.getUri());
                                    this.addProperty(this.jsonNode, resourceNode, new JsonField("parent"), language);
                                }
                            }

                            // make sure we flag the node as a proxy
                            resourceNode.put(ResourceIndexEntry.resourceTypeField.getName(), ResourceIndexEntry.resourceTypeField.PROXY_VALUE);

                            // lastly, hook the sub-node in the main node using the "_proxy" suffix
                            this.addProperty(this.jsonNode, resourceNode, field, language);
                        }
                        //we didn't get a resource value from the endpoint; this shouldn't really happen because how did we get our hands on the URI in the first place anyway?
                        else {
                            throw new IOException("Unable to serialize resource value because it's resource endpoint returned null; " + triple);
                        }
                    }
                    // You can activate this code below to index resources as sub-objects as well, but it seems to overcomplicate things a lot...
//                    else if (WidgetType.Resource.equals(property.getWidgetType())) {
//
//                        ObjectNode valueNode = Json.getObjectMapper().createObjectNode();
//
//                        // note that we only know the resource of the target object is this URI,
//                        // it's uri property might be different, we just don't know: we have no endpoint,
//                        // so we can't query it. Note that we index all local uris relatively
//                        // Also note solr can't make documents without identifier, so it'll generate one automatically,
//                        // based on the ID of the parent!
//                        valueNode.put(ResourceIndexEntry.resourceField.getName(), ResourceIndexEntry.resourceField.serialize(value, language));
//
//                        // let's not call this a proxy, but a stub instead
//                        valueNode.put(ResourceIndexEntry.resourceTypeField.getName(), ResourceIndexEntry.resourceTypeField.serialize(Type.STUB));
//
//                        this.addProperty(this.jsonNode, valueNode, field, language);
//                    }
                    else {
                        // we'll store xsd:anyURI as a literal value
                        this.addProperty(this.jsonNode, value, field, language);
                    }
                }
                else {
                    //if it's a literal value, put the value in the json object
                    this.addProperty(this.jsonNode, value, field, language);
                }
            }
            else {
                Logger.error("Encountered an unknown or invalid RDF predicate '" + triple.getPredicate() + "' while mapping to JSON." +
                             " This property will be ignored and excluded from the JSON object (this is probably a stale property and you may want to resolve this); " + triple);
            }
        }

        // flag the node as default when no specific resource type is set
        if (!this.jsonNode.has(ResourceIndexEntry.resourceTypeField.getName())) {
            this.jsonNode.put(ResourceIndexEntry.resourceTypeField.getName(), ResourceIndexEntry.resourceTypeField.DEFAULT_VALUE);
        }

        // See https://github.com/republic-of-reinvention/com.stralo.framework/issues/60
        // for why we changed our mind from filling in empty fields with null to just omitting them.
        this.copyInternalFields(this, this.jsonNode, true);
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
    private void initializeInternalFields(ObjectNode object, URI rootResourceUri, Locale language, RdfClass type, JsonPageIndexEntry parent, Model rootModel) throws IOException
    {
        this.setResource(PageIndexEntry.resourceField.create(rootResourceUri));
        this.setTypeOf(type);
        this.setLanguage(language);
        if (this.getParentUri() != null) {
            throw new IOException("Overwriting parentUri during initialization, this probably means something is wrong; " + this);
        }
        else {
            // Note: this handles null-valued parents okay
            this.setParentUri(PageIndexEntry.parentUriField.create(parent));
        }

        ResourceSummarizer summarizer = type.getSummarizer();
        if (summarizer != null) {
            ResourceSummary summary = summarizer.summarize(type, rootModel);
            if (summary != null) {
                this.setLabel(summary.getLabel());
                this.setDescription(summary.getDescription());
                this.setImage(summary.getImage());
            }
            else {
                throw new IOException("RDF class summarizer returned a null summary; this shouldn't happen; " + type);
            }
        }
        else {
            throw new IOException("Encountered an RDF class with a null summarizer; this shouldn't happen; " + type);
        }
    }
    private ObjectNode copyInternalFields(ResourceProxy resourceProxy, ObjectNode object, boolean omitEmptyFields)
    {
        // now iterate all internal fields of this object and copy them to the Json node
        for (IndexEntryField e : INTERNAL_FIELDS) {
            // Virtual fields shouldn't be copied to the serialized JSON representation eg. because they
            // are implemented as Solr copyFields (eg. tokenizedUri)
            if (!e.isVirtual()) {
                if (e.hasValue(resourceProxy)) {
                    object.put(e.getName(), e.getValue(resourceProxy));
                }
                else {
                    if (!omitEmptyFields) {
                        object.putNull(e.getName());
                    }
                }
            }
        }

        return object;
    }
    /**
     * Add the value (of the specified property in the specified language) to the json object,
     * auto-detecting if this is the first property and converting the field to an array if (and only if) the
     * field was already present in the json object.
     */
    private IndexEntryField addProperty(ObjectNode node, Object value, IndexEntryField field, Locale language) throws IOException
    {
        // this will support both array-based subObjects when there are multiple objects mapped on the same field
        // and standard subnode (not in an array) when there's only one.
        JsonNode existingField = node.get(field.getName());
        if (existingField == null) {
            if (value instanceof JsonNode) {
                node.set(field.getName(), (JsonNode) value);
            }
            else if (value instanceof Value) {
                Value rdfValue = (Value) value;

                node.put(field.getName(), field.serialize(rdfValue, language));
            }
            else {
                throw new IOException("Unimplemented value type, this shouldn't happen; " + value);
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
                Value rdfValue = (Value) value;

                node.withArray(field.getName()).add(field.serialize(rdfValue, language));
            }
            else {
                throw new IOException("Unimplemented value type, this shouldn't happen; " + value);
            }
        }

        return field;
    }
    private RdfClass extractModelTypeof(IRI resource, Model model)
    {
        return RdfFactory.lookup(Models.objectIRI(model.filter(resource, TYPEOF_PROPERTY_IRI, null)).orElse(null), RdfClass.class);
    }
}

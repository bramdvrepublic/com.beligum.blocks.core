package com.beligum.blocks.rdf.mappers;

import com.beligum.base.utils.Logger;
import com.beligum.base.utils.json.Json;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfMapper;
import com.beligum.blocks.rdf.ifaces.RdfOntology;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfResource;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.rdf.ontologies.RDFS;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.eclipse.rdf4j.model.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DefaultRdfMapper implements RdfMapper
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private ObjectMapper jsonMapper;
    private SolrClient solrClient;

    //-----CONSTRUCTORS-----
    public DefaultRdfMapper()
    {
        this.jsonMapper = Json.getObjectMapper();

        try {
            final String coreName = "pages";

            SolrClient tempSolrClient = new HttpSolrClient.Builder("http://localhost:8983/solr/").build();
            CoreAdminRequest request = new CoreAdminRequest();
            request.setAction(CoreAdminParams.CoreAdminAction.STATUS);
            CoreAdminResponse cores = request.process(tempSolrClient);

            // List of the cores
            List<String> coreList = new ArrayList<>();
            for (int i = 0; i < cores.getCoreStatus().size(); i++) {
                coreList.add(cores.getCoreStatus().getName(i));
            }

            boolean newCore = !coreList.contains(coreName);
            if (newCore) {

                Path solrFolder = Paths.get("/home/bram/Programs/solr-8.0.0/server/solr/");
                Path coreFolder = solrFolder.resolve("configsets/" + coreName);
                Path coreConfigFolder = coreFolder.resolve("conf");
                if (!Files.exists(coreFolder)) {
                    Files.createDirectories(coreFolder);

                    Files.createDirectories(coreConfigFolder);
                    FileUtils.copyFile(solrFolder.resolve("configsets/_default/conf/solrconfig.xml").toFile(), coreConfigFolder.resolve("solrconfig.xml").toFile());
                    FileUtils.writeStringToFile(coreConfigFolder.resolve("schema.xml").toFile(), "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                                                                                                 "<schema name=\"default-config\" version=\"1.6\">\n" +
                                                                                                 "    <field name=\"id\" type=\"string\" indexed=\"true\" stored=\"true\" required=\"true\" multiValued=\"false\" />\n" +
                                                                                                 "    <field name=\"_version_\" type=\"plong\" indexed=\"false\" stored=\"false\"/>\n" +
                                                                                                 "    <field name=\"_root_\" type=\"string\" indexed=\"true\" stored=\"false\" docValues=\"false\" />\n" +
                                                                                                 "    <field name=\"_nest_path_\" type=\"_nest_path_\" /><fieldType name=\"_nest_path_\" class=\"solr.NestPathField\" />\n" +
                                                                                                 "    <field name=\"_text_\" type=\"text_general\" indexed=\"true\" stored=\"false\" multiValued=\"true\"/>\n" +
                                                                                                 "    <uniqueKey>id</uniqueKey>\n" +
                                                                                                 "    <fieldType name=\"string\" class=\"solr.StrField\" sortMissingLast=\"true\" docValues=\"true\" />\n" +
                                                                                                 "    <fieldType name=\"strings\" class=\"solr.StrField\" sortMissingLast=\"true\" multiValued=\"true\" docValues=\"true\" />\n" +
                                                                                                 "    <fieldType name=\"boolean\" class=\"solr.BoolField\" sortMissingLast=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"booleans\" class=\"solr.BoolField\" sortMissingLast=\"true\" multiValued=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"pint\" class=\"solr.IntPointField\" docValues=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"pfloat\" class=\"solr.FloatPointField\" docValues=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"plong\" class=\"solr.LongPointField\" docValues=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"pdouble\" class=\"solr.DoublePointField\" docValues=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"pints\" class=\"solr.IntPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"pfloats\" class=\"solr.FloatPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"plongs\" class=\"solr.LongPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"pdoubles\" class=\"solr.DoublePointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"random\" class=\"solr.RandomSortField\" indexed=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"ignored\" stored=\"false\" indexed=\"false\" multiValued=\"true\" class=\"solr.StrField\" />\n" +
                                                                                                 "    <fieldType name=\"pdate\" class=\"solr.DatePointField\" docValues=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"pdates\" class=\"solr.DatePointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
                                                                                                 "    <fieldType name=\"binary\" class=\"solr.BinaryField\"/>\n" +
                                                                                                 "    <fieldType name=\"text_general\" class=\"solr.TextField\" positionIncrementGap=\"100\" multiValued=\"true\">\n" +
                                                                                                 "      <analyzer type=\"index\">\n" +
                                                                                                 "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                                                                 "        <!--<filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" />-->\n" +
                                                                                                 "        <!-- in this example, we will only use synonyms at query time\n" +
                                                                                                 "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"index_synonyms.txt\" ignoreCase=\"true\" expand=\"false\"/>\n" +
                                                                                                 "        <filter class=\"solr.FlattenGraphFilterFactory\"/>\n" +
                                                                                                 "        -->\n" +
                                                                                                 "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                                                                 "      </analyzer>\n" +
                                                                                                 "      <analyzer type=\"query\">\n" +
                                                                                                 "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                                                                 "        <!--<filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" />-->\n" +
                                                                                                 "        <!--<filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"true\"/>-->\n" +
                                                                                                 "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                                                                 "      </analyzer>\n" +
                                                                                                 "    </fieldType>\n" +
                                                                                                 "</schema>\n");

                    //FileUtils.copyDirectory(solrFolder.resolve("configsets/_default/conf").toFile(), coreFolder.resolve("conf").toFile());
                }

                CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
                createRequest.setCoreName(coreName);
                createRequest.setInstanceDir(coreFolder.toAbsolutePath().toString());
                createRequest.process(tempSolrClient);

                Logger.info("");
            }

            this.solrClient = new HttpSolrClient.Builder("http://localhost:8983/solr/" + coreName).build();

            //see https://lucene.apache.org/solr/guide/7_0/major-changes-in-solr-7.html#schemaless-improvements
            if (newCore) {
                // Solrj does not support the config API yet.
                GenericSolrRequest rq = new GenericSolrRequest(SolrRequest.METHOD.POST, "/config", new ModifiableSolrParams());
                rq.setContentWriter(new RequestWriter.StringPayloadContentWriter("{ \"set-user-property\": { \"update.autoCreateFields\": \"false\" } }",
                                                                                 CommonParams.JSON_MIME));
                rq.process(this.solrClient);
            }

            SchemaResponse.FieldsResponse fieldsResponse = new SchemaRequest.Fields().process(this.solrClient);
            Map<String, String> allFields = new LinkedHashMap<>();
            for (Map<String, Object> f : fieldsResponse.getFields()) {
                String name = f.get("name").toString();
                String type = f.get("type").toString();
                allFields.put(name, type);
            }

            for (RdfOntology o : RdfFactory.getPublicOntologies()) {
                for (RdfProperty p : o.getAllProperties()) {
                    String propField = toSolrField(p);
                    String propType = toSolrFieldType(p);

                    if (propType != null) {
                        if (allFields.containsKey(propField)) {
                            //TODO update it
                        }
                        else {
                            new SchemaRequest.AddField(new ImmutableMap.Builder<String, Object>()
                                                                       .put("name", propField)
                                                                       .put("type", propType)
                                                                       .build())
                                            .process(this.solrClient);
                        }
                    }
                    else {
                        //TODO delete if it exists
                    }
                }
            }

            Logger.info("");
        }
        catch (Exception e) {
            Logger.error(e);
        }
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

                URI subject = RdfTools.iriToUri((IRI) triple.getSubject());

                //check if this statement is about the main resource of the page
                boolean isMain = subject.equals(pageResource);

                //create a new sub-object if we need to
                ObjectNode node = subObjects.get(subject);
                if (!subObjects.containsKey(subject)) {
                    subObjects.put(subject, node = this.jsonMapper.createObjectNode());
                    if (isMain) {
                        if (retVal == null) {
                            retVal = node;
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

        //now attach the sub-objects to the main object
        for (Map.Entry<URI, ObjectNode> m : subObjects.entrySet()) {
            //skip the retval because we're attaching the others to it
            if (!m.getValue().equals(retVal)) {
                RdfProperty mappingProperty = subObjectMapping.get(m.getKey());
                if (mappingProperty != null) {
                    this.putProperty(retVal, mappingProperty, m.getValue());
                }
                else {
                    //note: this will happen; it means the model had a subject URI that isn't linked to the main object
                    // and this is always the case with our RDFa models because the "rdfa:usesVocabulary" property is attached as a predicate
                    // to the public (human readable) page address, but that URI is never attached to the resource URI further down the model
                }
            }
        }

        //TODO this is not right, just a DEBUG impl
        retVal.put("id", pageResource.toString());

        String jsonStr = this.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(retVal);

        this.saveToSolr(retVal);

        this.querySolr(retVal);

        if (true) throw new RuntimeException("DEBUG");

        return retVal;
    }
    private boolean querySolr(JsonNode json) throws IOException
    {
        try {

            SolrQuery query = new SolrQuery();
            //query.setQuery("*:*");
            query.set("q", QueryParser.escape(toSolrField(RDFS.label)) + ":*");
            QueryResponse response = this.solrClient.query(query);

            SolrDocumentList docList = response.getResults();

            Logger.info("Found " + docList.getNumFound() + " docs");

            for (SolrDocument doc : docList) {
                for (String fieldName : doc.getFieldNames()) {
                    Object fieldValue = doc.getFieldValue(fieldName);
                    Logger.info(fieldName + " - " + fromSolrField(fieldName) + " - " + fieldValue);
                }
                //Logger.info((String) doc.getFieldValue("id"), "123456");
            }

            //return !docList.isEmpty();
            return false;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }
    private void saveToSolr(JsonNode json) throws IOException
    {
        try {
            byte[] jsonBytes = this.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(json);
            JSONUpdateRequest request = new JSONUpdateRequest(new ByteArrayInputStream(jsonBytes));
            UpdateResponse response = request.process(this.solrClient);

            //v1
            //this.solrClient.add(this.makeSolrDoc(json));

            this.solrClient.commit();
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }
    private static SolrInputDocument makeSolrDoc(JsonNode jsonNode)
    {
        try {
            SolrInputDocument doc = new SolrInputDocument();

            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();

                JsonNode value = field.getValue();
                if (value.isContainerNode()) {
                    //                    ArrayNode array = (ArrayNode) value;
                    //
                    //                    for (int i = 0; i < array.length(); i++) {
                    //                        doc.addField(key, array.get(i));
                    //                    }
                    //TODO
                }
                else {
                    //see https://lucene.apache.org/solr/guide/7_4/defining-fields.html
                    doc.addField(toSolrField(field.getKey()), value.textValue());
                }
            }

            doc.addField("name", "Kenmore Dishwasher");

            return doc;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void putProperty(ObjectNode node, RdfProperty predicate, String value)
    {
        node.put(predicate.getCurie().toString(), value);
    }
    private void putProperty(ObjectNode node, RdfProperty predicate, JsonNode object)
    {
        node.set(predicate.getCurie().toString(), object);
    }
    private static String toSolrField(RdfProperty property)
    {
        return toSolrField(property.getCurie().toString());
    }
    private static String toSolrField(String property)
    {
        return property/*.replace(":", "_")*/;
    }
    private static RdfProperty fromSolrField(String field) throws IOException
    {
        //see https://lucene.apache.org/solr/guide/7_4/defining-fields.html
        if (!field.startsWith("_")) {
            return RdfFactory.lookup(field.replaceFirst("_", ":"), RdfProperty.class);
        }
        else {
            return null;
        }
    }
    private static String toSolrFieldType(RdfProperty property) throws IOException
    {
        String retVal = null;

        if (property.getDataType() != null) {

            //Note: for an overview possible values, check com.beligum.blocks.config.InputType
            if (property.getDataType().equals(XSD.boolean_)) {
                retVal = "boolean";
            }
            //because both date and time are strict dates, we'll use the millis (long) since epoch as the index value
            else if (property.getDataType().equals(XSD.date) || property.getDataType().equals(XSD.dateTime)) {
                retVal = "pdate";
            }
            //we don't have a date for time, so we'll use the millis since midnight as the index value
            else if (property.getDataType().equals(XSD.time)) {
                retVal = "plong";
            }
            else if (property.getDataType().equals(XSD.int_)
                     || property.getDataType().equals(XSD.integer)
                     || property.getDataType().equals(XSD.negativeInteger)
                     || property.getDataType().equals(XSD.unsignedInt)
                     || property.getDataType().equals(XSD.nonNegativeInteger)
                     || property.getDataType().equals(XSD.nonPositiveInteger)
                     || property.getDataType().equals(XSD.positiveInteger)
                     || property.getDataType().equals(XSD.short_)
                     || property.getDataType().equals(XSD.unsignedShort)
                     || property.getDataType().equals(XSD.byte_)
                     || property.getDataType().equals(XSD.unsignedByte)) {
                retVal = "pint";
            }
            else if (property.getDataType().equals(XSD.language)) {
                retVal = "string";
            }
            else if (property.getDataType().equals(XSD.long_)
                     || property.getDataType().equals(XSD.unsignedLong)) {
                retVal = "plong";
            }
            else if (property.getDataType().equals(XSD.float_)) {
                retVal = "pfloat";
            }
            else if (property.getDataType().equals(XSD.double_)
                     //this is doubtful, but let's take the largest one
                     // Note we could also try to fit as closely as possible, but that would change the type per value (instead of per 'column'), and that's not a good idea
                     || property.getDataType().equals(XSD.decimal)) {
                retVal = "pdouble";
            }
            else if (property.getDataType().equals(XSD.string)
                     || property.getDataType().equals(XSD.normalizedString)
                     || property.getDataType().equals(RDF.langString)
                     //this is a little tricky, but in the end it's just a string, right?
                     || property.getDataType().equals(XSD.base64Binary)) {
                retVal = "string";
            }
            else if (property.getDataType().equals(RDF.HTML)) {
                retVal = "string";
            }
            else if (property.getDataType().equals(XSD.anyURI)) {
                retVal = "string";
            }
            else {
                //TODO
                //throw new IOException("Encountered RDF property '" + property + "' with unsupported datatype; " + property.getDataType());
            }
        }

        return retVal;
    }

    //    https://github.com/bbende/embeddedsolrserver-example/blob/master/src/test/java/org/apache/solr/EmbeddedSolrServerFactory.java
    //    private static SolrClient create(final String solrHome, final String configSetHome, final String coreName) throws IOException, SolrServerException
    //    {
    //        final File solrHomeDir = new File(solrHome);
    //        if (solrHomeDir.exists()) {
    //            FileUtils.deleteDirectory(solrHomeDir);
    //            solrHomeDir.mkdirs();
    //        } else {
    //            solrHomeDir.mkdirs();
    //        }
    //
    //        final SolrResourceLoader loader = new SolrResourceLoader(solrHomeDir.toPath());
    //        final Path configSetPath = Paths.get(configSetHome).toAbsolutePath();
    //
    //        final NodeConfig config = new NodeConfig.NodeConfigBuilder("embeddedSolrServerNode", loader)
    //                        .setConfigSetBaseDirectory(configSetPath.toString())
    //                        .build();
    //
    //        final EmbeddedSolrServer embeddedSolrServer = new EmbeddedSolrServer(config, coreName);
    //
    //        final CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
    //        createRequest.setCoreName(coreName);
    //        createRequest.setConfigSet(coreName);
    //        embeddedSolrServer.request(createRequest);
    //
    //        return embeddedSolrServer;
    //    }
}

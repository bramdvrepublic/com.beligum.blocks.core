/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.filesystem.index.solr;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexConnection;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexer;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfOntology;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.apache.curator.utils.PathUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.CoreStatus;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.*;
import org.apache.solr.util.SolrCLI;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bram on 3/24/19.
 */
public class SolrPageIndexer implements PageIndexer
{
    //-----CONSTANTS-----
    public static final String SOLR_CORE_NAME = "pages";

    //-----VARIABLES-----
    private SolrClient solrClient;

    //-----CONSTRUCTORS-----
    public SolrPageIndexer(StorageFactory.Lock storageFactoryLock) throws IOException
    {
        this.reinit();
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexConnection connect(TX tx) throws IOException
    {
        return null;
    }
    @Override
    public void reboot() throws IOException
    {

    }
    @Override
    public void shutdown() throws IOException
    {

    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void reinit() throws IOException
    {
        try {
            if (this.solrClient != null) {
                this.solrClient.close();
                this.solrClient = null;
            }

            String coreName = SOLR_CORE_NAME;
            this.solrClient = this.initEmbedded(coreName);

            boolean coreFound = false;
            CoreAdminRequest request = new CoreAdminRequest();
            request.setAction(CoreAdminParams.CoreAdminAction.STATUS);
            CoreAdminResponse cores = request.process(this.solrClient);
            for (int i = 0; i < cores.getCoreStatus().size() && !coreFound; i++) {
                coreFound = coreName.equals(cores.getCoreStatus().getName(i));
            }

            if (!coreFound) {
                CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
                createRequest.setCoreName(coreName);
                createRequest.setConfigSet(coreName);
                this.solrClient.request(createRequest);

                // Solrj does not support the config API yet.
                GenericSolrRequest rq = new GenericSolrRequest(SolrRequest.METHOD.POST, "/config", new ModifiableSolrParams());
                rq.setContentWriter(new RequestWriter.StringPayloadContentWriter("{ \"set-user-property\": { \"update.autoCreateFields\": \"false\" } }", CommonParams.JSON_MIME));
                rq.process(this.solrClient);
            }

            //now check if the schema is still correct
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
            throw new IOException("Error while initializing/creating embedded Solr server; " + SOLR_CORE_NAME, e);
        }
    }
    private SolrClient initEmbedded(String coreName) throws IOException
    {
        Path solrHomeDir = Paths.get(Settings.instance().getPageMainIndexFolder());
        if (!Files.exists(solrHomeDir)) {
            Files.createDirectories(solrHomeDir);
        }
        if (!Files.isWritable(solrHomeDir)) {
            throw new IOException("Solr home directory is not writable, please check the permissions; " + solrHomeDir);
        }

        Path solrXml = solrHomeDir.resolve(SolrXmlConfig.SOLR_XML_FILE);
        if (!Files.exists(solrXml)) {
            Files.write(solrXml, DefaultConfig.SOLR_XML.getBytes(Charsets.UTF_8));
        }

        NodeConfig solrConfig = SolrXmlConfig.fromSolrHome(solrHomeDir);
        Path configSetsDir = solrConfig.getConfigSetBaseDirectory();
        Path coreConfigDir = configSetsDir.resolve(coreName).resolve("conf");
        if (!Files.exists(coreConfigDir)) {
            Files.createDirectories(coreConfigDir);
        }
        if (!Files.isWritable(coreConfigDir)) {
            throw new IOException("Solr core home directory is not writable, please check the permissions; " + coreConfigDir);
        }

        Path coreXml = coreConfigDir.resolve("solrconfig.xml");
        if (!Files.exists(coreXml)) {
            Files.write(coreXml, DefaultConfig.CORE_XML.getBytes(Charsets.UTF_8));
        }

        Path coreSchema = coreConfigDir.resolve("schema.xml");
        if (!Files.exists(coreSchema)) {
            Files.write(coreSchema, DefaultConfig.CORE_SCHEMA.getBytes(Charsets.UTF_8));
        }

//        final SolrResourceLoader loader = new SolrResourceLoader(solrHomeDir);
//        final Path configSetPath = Paths.get(configSetHome).toAbsolutePath();
//        final NodeConfig config = new NodeConfig.NodeConfigBuilder("embeddedSolrServerNode", loader)
//                                .setConfigSetBaseDirectory(configSetsDir.toString())
//                                .build();

        return new EmbeddedSolrServer(solrConfig, coreName);
    }
    private SolrClient initRemote(String coreName) throws IOException, SolrServerException
    {
        //note that this connects to the general solr server, not the specific requested core!
        return new HttpSolrClient.Builder("http://localhost:8983/solr/").build();

        //        if (newCore) {
        //
        //            Path solrFolder = Paths.get("/home/bram/Programs/solr-8.0.0/server/solr/");
        //            Path coreFolder = solrFolder.resolve("configsets/" + coreName);
        //            Path coreConfigFolder = coreFolder.resolve("conf");
        //            if (!Files.exists(coreFolder)) {
        //                Files.createDirectories(coreFolder);
        //
        //                Files.createDirectories(coreConfigFolder);
        //                FileUtils.copyFile(solrFolder.resolve("configsets/_default/conf/solrconfig.xml").toFile(), coreConfigFolder.resolve("solrconfig.xml").toFile());
        //                FileUtils.writeStringToFile(coreConfigFolder.resolve("schema.xml").toFile(), "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
        //                                                                                             "<schema name=\"default-config\" version=\"1.6\">\n" +
        //                                                                                             "    <field name=\"id\" type=\"string\" indexed=\"true\" stored=\"true\" required=\"true\" multiValued=\"false\" />\n" +
        //                                                                                             "    <field name=\"_version_\" type=\"plong\" indexed=\"false\" stored=\"false\"/>\n" +
        //                                                                                             "    <field name=\"_root_\" type=\"string\" indexed=\"true\" stored=\"false\" docValues=\"false\" />\n" +
        //                                                                                             "    <field name=\"_nest_path_\" type=\"_nest_path_\" /><fieldType name=\"_nest_path_\" class=\"solr.NestPathField\" />\n" +
        //                                                                                             "    <field name=\"_text_\" type=\"text_general\" indexed=\"true\" stored=\"false\" multiValued=\"true\"/>\n" +
        //                                                                                             "    <uniqueKey>id</uniqueKey>\n" +
        //                                                                                             "    <fieldType name=\"string\" class=\"solr.StrField\" sortMissingLast=\"true\" docValues=\"true\" />\n" +
        //                                                                                             "    <fieldType name=\"strings\" class=\"solr.StrField\" sortMissingLast=\"true\" multiValued=\"true\" docValues=\"true\" />\n" +
        //                                                                                             "    <fieldType name=\"boolean\" class=\"solr.BoolField\" sortMissingLast=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"booleans\" class=\"solr.BoolField\" sortMissingLast=\"true\" multiValued=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"pint\" class=\"solr.IntPointField\" docValues=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"pfloat\" class=\"solr.FloatPointField\" docValues=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"plong\" class=\"solr.LongPointField\" docValues=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"pdouble\" class=\"solr.DoublePointField\" docValues=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"pints\" class=\"solr.IntPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"pfloats\" class=\"solr.FloatPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"plongs\" class=\"solr.LongPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"pdoubles\" class=\"solr.DoublePointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"random\" class=\"solr.RandomSortField\" indexed=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"ignored\" stored=\"false\" indexed=\"false\" multiValued=\"true\" class=\"solr.StrField\" />\n" +
        //                                                                                             "    <fieldType name=\"pdate\" class=\"solr.DatePointField\" docValues=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"pdates\" class=\"solr.DatePointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
        //                                                                                             "    <fieldType name=\"binary\" class=\"solr.BinaryField\"/>\n" +
        //                                                                                             "    <fieldType name=\"text_general\" class=\"solr.TextField\" positionIncrementGap=\"100\" multiValued=\"true\">\n" +
        //                                                                                             "      <analyzer type=\"index\">\n" +
        //                                                                                             "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
        //                                                                                             "        <!--<filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" />-->\n" +
        //                                                                                             "        <!-- in this example, we will only use synonyms at query time\n" +
        //                                                                                             "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"index_synonyms.txt\" ignoreCase=\"true\" expand=\"false\"/>\n" +
        //                                                                                             "        <filter class=\"solr.FlattenGraphFilterFactory\"/>\n" +
        //                                                                                             "        -->\n" +
        //                                                                                             "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
        //                                                                                             "      </analyzer>\n" +
        //                                                                                             "      <analyzer type=\"query\">\n" +
        //                                                                                             "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
        //                                                                                             "        <!--<filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" />-->\n" +
        //                                                                                             "        <!--<filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"true\"/>-->\n" +
        //                                                                                             "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
        //                                                                                             "      </analyzer>\n" +
        //                                                                                             "    </fieldType>\n" +
        //                                                                                             "</schema>\n");
        //
        //                //FileUtils.copyDirectory(solrFolder.resolve("configsets/_default/conf").toFile(), coreFolder.resolve("conf").toFile());
        //            }
        //
        //            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        //            createRequest.setCoreName(coreName);
        //            createRequest.setInstanceDir(coreFolder.toAbsolutePath().toString());
        //            createRequest.process(tempSolrClient);
        //        }
        //
        //        return new HttpSolrClient.Builder("http://localhost:8983/solr/" + coreName).build();
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
}

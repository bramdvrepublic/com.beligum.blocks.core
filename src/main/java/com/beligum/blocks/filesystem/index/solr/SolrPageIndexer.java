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

import ch.qos.logback.classic.Level;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.entries.pages.JsonPageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexConnection;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexer;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfOntology;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrXmlConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by bram on 3/24/19.
 *
 * Interesting reads:
 * http://blog-archive.griddynamics.com/2013/09/solr-block-join-support.html
 * https://issues.apache.org/jira/browse/SOLR-12768
 * https://issues.apache.org/jira/browse/SOLR-5211
 * https://www.slideshare.net/lucidworks/json-in-solr-from-top-to-bottom-alexander-rafalovitch-united-nations
 * https://issues.apache.org/jira/browse/SOLR-12298
 * https://issues.apache.org/jira/browse/SOLR-12362
 * https://issues.apache.org/jira/browse/SOLR-7672?jql=project%20%3D%20SOLR%20AND%20text%20~%20_nest_path_
 * http://yonik.com/solr-nested-objects/
 * https://gitbox.apache.org/repos/asf?p=lucene-solr.git;h=32a97c1
 * https://gitbox.apache.org/repos/asf?p=lucene-solr.git;a=blob;f=solr/solr-ref-guide/src/uploading-data-with-index-handlers.adoc;h=1c090a09693e7f49fba59df438c49b93dd18fea6;hb=32a97c1
 * https://gitbox.apache.org/repos/asf?p=lucene-solr.git;a=blob;f=solr/solr-ref-guide/src/indexing-nested-documents.adoc;h=ada865e97089f8c3ef738b67959801d6698fa55c;hb=32a97c1
 *
 */
public class SolrPageIndexer implements PageIndexer
{
    //-----CONSTANTS-----
    private static final String SOLR_CORE_NAME = "pages";
    private static final String CORE_CONF_DIR = "conf";

    //-----VARIABLES-----
    private SolrClient solrClient;
    private boolean useSchemaless;

    //-----CONSTRUCTORS-----
    public SolrPageIndexer(StorageFactory.Lock storageFactoryLock) throws IOException
    {
        //The default INFO log level is too verbose for solr in production
        //note: this only needs to be done once, don't put it in init()
        if (R.configuration().getProduction()) {
            R.configuration().getLogConfig().setLogLevel("org.apache.solr", Level.WARN);
        }

        // Note that we don't use schemaless mode for now (we want tight control over the fields,
        // mainly to understand what's happening). This flag is added to easily switch to schemaless
        // during debugging and for possible future use (eg. it works)
        this.useSchemaless = false;

        this.init();
    }

    //-----PUBLIC METHODS-----
    @Override
    public synchronized PageIndexConnection connect(TX tx) throws IOException
    {
        return new SolrPageIndexConnection(this, tx);
    }
    @Override
    public synchronized void reboot() throws IOException
    {
        try {
            this.shutdown();
        }
        finally {
            this.init();
        }
    }
    @Override
    public synchronized void shutdown() throws IOException
    {
        // See https://lucidworks.com/2013/08/23/understanding-transaction-logs-softcommit-and-commit-in-sorlcloud/
        // Stop ingesting documents
        // Issue a hard commit or wait until the autoCommit interval expires.
        // Stop the Solr servers.
        if (this.solrClient != null) {

            Logger.info("Shutting down Solr server...");

            try {
                this.solrClient.commit(true, true);
            }
            catch (SolrServerException e) {
                Logger.error("Error while flushing the Solr client while shutting down", e);
            }

            //note that this will shutdown the core container when the instance is an embedded server
            this.solrClient.close();
            this.solrClient = null;
        }
    }

    //-----PROTECTED METHODS-----
    //TODO re-think concurrency
    synchronized SolrClient getSolrClient() throws IOException
    {
        return this.solrClient;
    }

    //-----PRIVATE METHODS-----
    private void init() throws IOException
    {
        final String coreName = SOLR_CORE_NAME;

        boolean success = false;
        try {
            Logger.info("Booting up (embedded) Solr server");

            //For now, we'll always work with an embedded server, might change in the future, see initRemote()
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
                //see below: we won't be using separate directories for the core and the home; for us, they're the same
                createRequest.setInstanceDir(".");
                this.solrClient.request(createRequest);

                StringBuilder propertyConfigs = new StringBuilder("{" +
                                                                  // So (for now) our strategy is to disable auto soft commit and do a manual soft commit after each update
                                                                  // (although the article below says otherwise).
                                                                  // The autoCommit (real commit to disk) value of 15s is just the default value that's repeated here, just to be sure.
                                                                  // See https://stackoverflow.com/questions/5623307/solrj-disable-autocommit
                                                                  // but also https://lucidworks.com/2013/08/23/understanding-transaction-logs-softcommit-and-commit-in-sorlcloud/
                                                                  "  \"unset-property\": [" +
                                                                  "    \"updateHandler.autoCommit.maxDocs\"," +
                                                                  "  ]," +
                                                                  "  \"set-property\": {" +
                                                                  "    \"updateHandler.autoCommit.maxTime\": \"15000\"," +
                                                                  "    \"updateHandler.autoCommit.openSearcher\": \"false\"" +
                                                                  "  }," +
                                                                  "  \"set-property\": {" +
                                                                  "    \"updateHandler.autoSoftCommit.maxDocs\": \"-1\"," +
                                                                  "    \"updateHandler.autoSoftCommit.maxTime\": \"-1\"" +
                                                                  "  }," +
                                                                  "}");

                if (!this.useSchemaless) {
                    // This disables auto-field-creation of schemaless mode.
                    // By configuring Solr to not create it's fields automatically, we enforce strict control over which fields are allowed
                    // and how they're translated to the index.
                    propertyConfigs.append("  \"set-user-property\": {" +
                                           "    \"update.autoCreateFields\": \"false\"," +
                                           "  },");
                }

                // See https://lucene.apache.org/solr/guide/7_4/config-api.html
                new GenericSolrRequest(SolrRequest.METHOD.POST, "/config", new ModifiableSolrParams())
                                .setContentWriter(new RequestWriter.StringPayloadContentWriter(propertyConfigs.toString(), CommonParams.JSON_MIME))
                                .process(this.solrClient);
            }

            // Let's always call this during startup: ontologies may have changed
            if (!this.useSchemaless) {
                try {
                    this.initSchema();
                }
                catch (Exception e) {
                    throw new IOException("Error while initializing Solr schema; " + SOLR_CORE_NAME, e);
                }
            }

            success = true;
        }
        catch (Exception e) {
            throw new IOException("Error while initializing/creating Solr server; " + SOLR_CORE_NAME, e);
        }
        finally {
            //if the booting of (especially a new) server didn't succeed,
            //make sure it's closed cleanly, otherwise we'll have dangling lock files
            if (!success && this.solrClient != null) {
                this.solrClient.close();
            }
        }
    }
    private void initSchema() throws IOException, SolrServerException
    {
        //now check if the schema is still correct
        SchemaResponse.FieldsResponse fieldsResponse = new SchemaRequest.Fields().process(this.solrClient);
        Map<String, Map<String, Object>> existingFields = new LinkedHashMap<>();
        for (Map<String, Object> f : fieldsResponse.getFields()) {
            existingFields.put(f.get("name").toString(), f);
        }

        //we will wipe the fields from this map when they were processed, to iterare when all is done to detect the stale fields to be deleted
        Set<String> existingFieldsTracker = new HashSet<>(existingFields.keySet());

        //iterate all properties in all public ontologies and check if their field and type is known in the solr schema
        for (RdfOntology o : RdfFactory.getRelevantOntologies()) {
            //We only iterate the properties of the public ontologies; no need to check every single referenced ontology
            if (o.isPublic()) {
                //iterate all properties of all classes, since instances of these classes will
                //end up in the index after all
                for (RdfClass c : o.getAllClasses()) {
                    for (RdfProperty p : c.getProperties()) {

                        //translate the property to a solr field
                        SolrField rdfField = new SolrField(p);

                        //we need at least a name and a type
                        if (rdfField.getName() != null && rdfField.getType() != null) {

                            ImmutableMap<String, Object> rdfFieldMap = rdfField.toMap();

                            //if the field is known, make sure it didn't change
                            if (existingFields.containsKey(rdfField.getName())) {

                                //delete it from the set so we can check for stale fields in the loop below
                                existingFieldsTracker.remove(rdfField.getName());

                                //now compare the properties of the two
                                Map<String, Object> existingField = existingFields.get(rdfField.getName());

                                //note: using difference to be able to log what changed
                                MapDifference<String, Object> difference = Maps.difference(existingField, rdfFieldMap);
                                if (!difference.areEqual()) {
                                    Logger.info("Replacing field in Solr schema because it seemed to have changed; " + rdfField.getName() + " (" + rdfField.getType() + ") " +
                                                difference);
                                    new SchemaRequest.ReplaceField(rdfFieldMap).process(this.solrClient);
                                }
                            }
                            //if the field is unknown, add it
                            else {
                                Logger.info("Adding field to Solr schema because it doesn't exist yet; " + rdfField.getName() + " (" + rdfField.getType() + ")");
                                new SchemaRequest.AddField(rdfFieldMap).process(this.solrClient);
                            }
                        }
                        else {
                            throw new IOException("Unable to translate an RDF property to a valid Solr field, this should probably be fixed; " + rdfField.getName() + " (" +
                                                  rdfField.getType() + ")");
                        }
                    }
                }
            }
        }

        for (IndexEntryField field : new SolrPageIndexEntry().getInternalFields()) {
            // This is just a failsafe test to see if all internal fields are present in the Solr index.
            // We're not creating them if they are missing (this is static enough to be implemented in SolrConfigs),
            // we only throw an exception to tell the user something's wrong.
            if (!existingFields.containsKey(field.getName())) {
                throw new IOException("Encountered internal field that's not part of the Solr schema, please fix this; " + field);
            }
            //since it exists, wipe it from the list so it doesn't get deleted below
            else {
                existingFieldsTracker.remove(field.getName());
            }
        }

        //do the same for the reserved Solr fields like _version_ and all
        for (SolrField field : SolrConfigs.RESERVED_FIELDS) {
            if (!existingFields.containsKey(field.getName())) {
                throw new IOException("Encountered reserved Solr field that's not part of the Solr schema, please fix this; " + field);
            }
            //since it exists, wipe it from the list so it doesn't get deleted below
            else {
                existingFieldsTracker.remove(field.getName());
            }
        }

        for (String solrFieldName : existingFieldsTracker) {
            Logger.info("Deleting field from Solr schema because it seemed to have vanished; " + solrFieldName);
            new SchemaRequest.DeleteField(solrFieldName).process(this.solrClient);
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
            Files.write(solrXml, SolrConfigs.SOLR_CONFIG.getBytes(Charsets.UTF_8));
        }

        NodeConfig solrConfig = SolrXmlConfig.fromSolrHome(solrHomeDir);

        //Solr can group configs of different cores (eg. using the same config for multiple cores).
        //Therefore, is has the notion of a "configsets" folder, where each set gets a name.
        //However, we're more or less obliged to work with one config per core, because we'll be altering
        //the schema dynamically, according to the ontologies. So we'll create a "conf" folder directly
        //in the instance dir, using that as the config folder for the core.
        //Also (see commented resolve part below), we decided to not use a separate sub-folder for the core
        //because we're already in the "pages/index/" folder. Adding another "pages" subfolder there
        //is kind of silly. So for us (at least in embedded mode), the instanceDir is the same as
        //the homeDir.
        Path instanceDir = solrHomeDir/*.resolve(coreName)*/;
        Path coreConfig = instanceDir.resolve(CORE_CONF_DIR);
        if (!Files.exists(coreConfig)) {
            Files.createDirectories(coreConfig);
        }
        if (!Files.isWritable(coreConfig)) {
            throw new IOException("Solr core home directory is not writable, please check the permissions; " + coreConfig);
        }

        Path coreXml = coreConfig.resolve("solrconfig.xml");
        if (!Files.exists(coreXml)) {
            Files.write(coreXml, SolrConfigs.CORE_CONFIG.getBytes(Charsets.UTF_8));
        }

        Path coreSchema = coreConfig.resolve("managed-schema");
        if (!Files.exists(coreSchema)) {
            if (this.useSchemaless) {
                Files.write(coreSchema, SolrConfigs.DEFAULT_SCHEMA.getBytes(Charsets.UTF_8));
            }
            else {
                Files.write(coreSchema, SolrConfigs.CORE_SCHEMA.getBytes(Charsets.UTF_8));
            }
        }

        return new EmbeddedSolrServer(solrConfig, coreName);
    }
    private SolrClient initRemote(String coreName) throws IOException, SolrServerException
    {
        //TODO: setup the directory structure if needed: see initEmbedded()

        //note that this connects to the general solr server, not the specific requested core!
        return new HttpSolrClient.Builder("http://localhost:8983/solr/").build();
    }
}
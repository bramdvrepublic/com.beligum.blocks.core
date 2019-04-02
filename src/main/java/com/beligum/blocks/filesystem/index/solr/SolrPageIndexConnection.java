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

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.AbstractIndexConnection;
import com.beligum.blocks.filesystem.index.entries.JsonPageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.*;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.handler.UpdateRequestHandler;
import org.apache.solr.servlet.SolrRequestParsers;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 2/22/16.
 */
public class SolrPageIndexConnection extends AbstractIndexConnection implements PageIndexConnection
{
    //-----CONSTANTS-----
    public enum QueryFormat implements IndexConnection.QueryFormat
    {
        /**
         * The query string will be an URI with query parameters
         * as in a default standalone solr server
         */
        URI_PARAMS
    }

    private static final String TX_RESOURCE_NAME = SolrPageIndexConnection.class.getSimpleName();

    //-----VARIABLES-----
    private SolrPageIndexer pageIndexer;
    private SolrClient solrClient;
    private TX transaction;
    private boolean active;

    private RollbackBackup rollbackBackup = null;
    private Object rollbackBackupLock = new Object();

    //-----CONSTRUCTORS-----
    public SolrPageIndexConnection(SolrPageIndexer pageIndexer, TX transaction) throws IOException
    {
        this.pageIndexer = pageIndexer;
        this.solrClient = pageIndexer.getSolrClient();
        this.transaction = transaction;
        this.active = true;
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexEntry get(URI key) throws IOException
    {
        this.assertActive();

        try {
            //            SolrQuery query = new SolrQuery();
            //            query.setParam("q", "{!child of=" + PageIndexEntry.parentId.getName() + ":null}");
            //            QueryResponse response = this.solrClient.query(query);
            //            Logger.info("Got " + response.getResults().getNumFound() + " docs: " + json);
            //            for (int i = 0; i < response.getResults().getNumFound(); i++) {
            //                Logger.info(response.getResults().get(i).jsonStr());
            //            }
            //
            //            //{"id":"/en/blah","rdf:type":"http://www.reinvention.be/ontology/Page","rdfs:label":"rdfs label test 5"}
            //            //            key = URI.create("http://localhost:8080/en/blah");
            //            query = new SolrQuery();
            //            //query.setQuery(QueryParser.escape(SolrConfigs.CORE_SCHEMA_FIELD_ID) + ":" + SimplePageIndexEntry.generateId(key));
            //            query.setQuery("*:*");
            //            response = this.solrClient.query(query);
            //            SolrDocumentList docList = response.getResults();
            //            Logger.info("Got " + docList.getNumFound() + " docs: " + json);
            //            for (int i = 0; i < docList.getNumFound(); i++) {
            //                Logger.info(docList.get(i).jsonStr());
            //            }

            SolrDocument doc = this.solrClient.getById(PageIndexEntry.generateId(key));
            return doc == null ? null : new SolrPageIndexEntry(doc.jsonStr());
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }
    @Override
    public synchronized void update(Resource resource) throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        try {
            Page page = resource.unwrap(Page.class);

            SolrPageIndexEntry indexEntry = new SolrPageIndexEntry(page);

            this.saveRollbackBackup(page);

            this.update(indexEntry);

            // See https://lucidworks.com/2013/08/23/understanding-transaction-logs-softcommit-and-commit-in-sorlcloud/
            //
            // During a softcommit, the transaction log is not truncated, it continues to grow.
            // However, a new searcher is opened, which makes the documents since last softcommit visible for searching.
            // Also, some of the top-level caches in Solr are invalidated, so it’s not a completely free operation.
            this.solrClient.commit(false, false, true);
        }
        catch (Exception e) {
            throw new IOException("Error while updating a Solr resource; " + resource, e);
        }
    }
    private void update(SolrPageIndexEntry indexEntry) throws IOException, SolrServerException
    {
        // now all sub-objects are attached to each other, recursively iterate them to find all the paths to
        // the sub-objects, so we can report to Solr where to split its children
        StringBuilder solrSplit = new StringBuilder();
        indexEntry.iterateObjectNodes(new JsonPageIndexEntry.JsonNodeVisitor()
        {
            @Override
            public String getPathDelimiter()
            {
                return "/";
            }
            @Override
            public void visit(String fieldName, JsonNode fieldValue, String path)
            {
                if (solrSplit.length() > 0) {
                    solrSplit.append("|");
                }
                solrSplit.append(path);
            }
        });

        // see https://lucene.apache.org/solr/guide/7_7/uploading-data-with-index-handlers.html#json-formatted-index-updates
        // and https://lucene.apache.org/solr/guide/7_7/transforming-and-indexing-custom-json.html#setting-json-defaults
        // Note: the "/update/json/docs" path is a shortcut for "/update" with the contentType and the command=false already set right
        ContentStreamUpdateRequest request = new ContentStreamUpdateRequest(UpdateRequestHandler.DOC_PATH);
        request.addContentStream(new ContentStreamBase.StringStream(indexEntry.toString(), CommonParams.JSON_MIME));

        // Defines the path at which to split the input JSON into multiple Solr documents and is required if you have multiple documents in a single JSON file.
        // If the entire JSON makes a single Solr document, the path must be “/”.
        // It is possible to pass multiple split paths by separating them with a pipe (|), for example: split=/|/foo|/foo/bar.
        // If one path is a child of another, they automatically become a child document.
        request.setParam("split", solrSplit.toString());

        // See https://lucene.apache.org/solr/guide/7_7/transforming-and-indexing-custom-json.html
        // Provides multivalued mapping to map document field names to Solr field names.
        // The format of the parameter is target-field-name:json-path, as in f=first:/first.
        // The json-path is required. The target-field-name is the Solr document field name, and is optional.
        // If not specified, it is automatically derived from the input JSON.
        // The default target field name is the fully qualified name of the field.
        // Instead of specifying all the field names explicitly, it is possible to specify wildcards to map fields automatically.
        // There are two restrictions: wildcards can only be used at the end of the json-path, and the split path cannot use wildcards.
        // A single asterisk * maps only to direct children, and a double asterisk ** maps recursively to all descendants.
        // The following are example wildcard path mappings:
        // f=$FQN:/**: maps all fields to the fully qualified name ($FQN) of the JSON field. The fully qualified name is obtained by concatenating all the keys in the hierarchy with a period (.) as a delimiter. This is the default behavior if no f path mappings are specified.
        // f=/docs/*: maps all the fields under docs and in the name as given in JSON
        // f=/docs/**: maps all the fields under docs and its children in the name as given in JSON
        // f=searchField:/docs/*: maps all fields under /docs to a single field called ‘searchField’
        // f=searchField:/docs/**: maps all fields under /docs and its children to searchField
        request.setParam("f", "/**");

        // this effectively executes the update command
        this.solrClient.request(request);
    }
    @Override
    public synchronized void delete(Resource resource) throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        Page page = resource.unwrap(Page.class);

        try {
            this.solrClient.deleteById(PageIndexEntry.generateId(page));

            // see comments in update()
            this.solrClient.commit(false, false, true);
        }
        catch (Exception e) {
            throw new IOException("Error while deleting a Solr resource; " + resource, e);
        }
    }
    @Override
    public synchronized void deleteAll() throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        try {
            this.solrClient.deleteByQuery("*:*");

            // see comments in update()
            this.solrClient.commit(false, false, true);
        }
        catch (Exception e) {
            throw new IOException("Error while deleting an entire Solr database", e);
        }
    }
    @Override
    public IndexSearchResult search(IndexSearchRequest indexSearchRequest) throws IOException
    {
        this.assertActive();

        if (!(indexSearchRequest instanceof SolrIndexSearchRequest)) {
            throw new IOException("Encountered unsupported index search request object; this shouldn't happen" + indexSearchRequest);
        }
        else {
            try {
                return new SolrIndexSearchResult(indexSearchRequest, this.solrClient.query(((SolrIndexSearchRequest) indexSearchRequest).buildSolrQuery()));
            }
            catch (Exception e) {
                throw new IOException("Error while executing a Solr search; " + indexSearchRequest, e);
            }
        }
    }
    @Override
    public IndexSearchResult search(String query, IndexConnection.QueryFormat format) throws IOException
    {
        this.assertActive();

        IndexSearchResult retVal = null;

        if (format instanceof QueryFormat) {
            switch ((QueryFormat) format) {
                case URI_PARAMS:
                    try {
                        retVal = new SolrIndexSearchResult(this.solrClient.query(SolrRequestParsers.parseQueryString(query)));
                    }
                    catch (SolrServerException e) {
                        throw new IOException("Error while executing a Solr search; " + query, e);
                    }
                    break;
                default:
                    throw new IOException("Encountered unsupported query format; " + format);
            }
        }
        else {
            throw new IOException("Encountered unsupported query format; " + format);
        }

        return retVal;
    }
    @Override
    //Note: this needs to be synchronized for concurrency with the the assertActive() below
    public synchronized void close() throws IOException
    {
        //don't do this anymore: we switched from a new writer per transaction to a single writer
        // which instead flushes at the end of each transactions, so don't close it
        //        if (this.createdWriter) {
        //            this.getLuceneIndexWriter().close();
        //        }

        this.pageIndexer = null;
        this.transaction = null;
        this.active = false;
    }

    //-----PROTECTED METHODS-----
    @Override
    protected void begin() throws IOException
    {
        //note: there's not such thing as a .begin();
        // the begin is just where the last .commit() left off
    }
    @Override
    protected void prepareCommit() throws IOException
    {
        if (this.isRegistered()) {

        }
    }
    @Override
    protected void commit() throws IOException
    {
        if (this.isRegistered()) {
            this.flushRollbackBackup();
        }
    }
    @Override
    protected void rollback() throws IOException
    {
        if (this.isRegistered()) {
            try {
                this.restoreRollbackBackup();
            }
            catch (SolrServerException e) {
                throw new IOException(e);
            }
        }
    }
    @Override
    protected Indexer getResourceManager()
    {
        return this.pageIndexer;
    }

    //-----PRIVATE METHODS-----
    private synchronized void assertActive() throws IOException
    {
        if (!this.active) {
            throw new IOException("Can't proceed, an active Lucene index connection was asserted");
        }
    }
    private synchronized void assertTransaction() throws IOException
    {
        if (this.transaction == null) {
            throw new IOException("Transaction asserted, but none was initialized, can't continue");
        }
        else {
            //only need to do it once (at the beginning of a method using a tx)
            if (!this.isRegistered()) {
                //attach this connection to the transaction manager
                this.transaction.registerResource(TX_RESOURCE_NAME, this);
            }
        }
    }
    private boolean isRegistered()
    {
        return this.transaction != null && this.transaction.getRegisteredResource(TX_RESOURCE_NAME) != null;
    }
    private void saveRollbackBackup(Page page) throws IOException, SolrServerException
    {
        synchronized (rollbackBackupLock) {
            if (this.rollbackBackup == null) {
                this.rollbackBackup = new RollbackBackup(page, this.solrClient);
            }
            else {
                throw new IOException("Encountered a situation where the rollback backup was already set, this shouldn't happen; " + this.rollbackBackup);
            }
        }
    }
    private void restoreRollbackBackup() throws IOException, SolrServerException
    {
        synchronized (rollbackBackupLock) {
            if (this.rollbackBackup != null) {

                this.solrClient.deleteById(this.rollbackBackup.id);

                if (this.rollbackBackup.document != null) {
                    //TODO validate this; don't know if it works...
                    this.solrClient.add(this.solrClient.getBinder().toSolrInputDocument(this.rollbackBackup.document));
                }

                this.rollbackBackup = null;
            }
            else {
                throw new IOException("Encountered a situation where the rollback backup to be restored was not set, this shouldn't happen");
            }
        }
    }
    private void flushRollbackBackup() throws IOException
    {
        synchronized (rollbackBackupLock) {
            if (this.rollbackBackup != null) {
                this.rollbackBackup = null;
            }
            else {
                throw new IOException("Encountered a situation where the rollback backup to be flushed was not set, this shouldn't happen");
            }
        }
    }

    //    private boolean querySolr(JsonNode json) throws IOException
    //    {
    //        try {
    //
    //            SolrQuery query = new SolrQuery();
    //            //query.setQuery("*:*");
    //            query.set("q", QueryParser.escape(toSolrField(RDFS.label)) + ":*");
    //            QueryResponse response = this.solrClient.query(query);
    //
    //            SolrDocumentList docList = response.getResults();
    //
    //            Logger.info("Found " + docList.getNumFound() + " docs");
    //
    //            for (SolrDocument doc : docList) {
    //                for (String fieldName : doc.getFieldNames()) {
    //                    Object fieldValue = doc.getFieldValue(fieldName);
    //                    Logger.info(fieldName + " - " + fromSolrField(fieldName) + " - " + fieldValue);
    //                }
    //                //Logger.info((String) doc.getFieldValue("id"), "123456");
    //            }
    //
    //            //return !docList.isEmpty();
    //            return false;
    //        }
    //        catch (Exception e) {
    //            throw new IOException(e);
    //        }
    //    }
    //    private void saveToSolr(JsonNode json) throws IOException
    //    {
    //        try {
    //            byte[] jsonBytes = this.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(json);
    //            JSONUpdateRequest request = new JSONUpdateRequest(new ByteArrayInputStream(jsonBytes));
    //            UpdateResponse response = request.process(this.solrClient);
    //
    //            //v1
    //            //this.solrClient.add(this.makeSolrDoc(json));
    //
    //            this.solrClient.commit();
    //        }
    //        catch (Exception e) {
    //            throw new IOException(e);
    //        }
    //    }
    //    private static SolrInputDocument makeSolrDoc(JsonNode jsonNode)
    //    {
    //        try {
    //            SolrInputDocument doc = new SolrInputDocument();
    //
    //            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
    //            while (fields.hasNext()) {
    //                Map.Entry<String, JsonNode> field = fields.next();
    //
    //                JsonNode value = field.getValue();
    //                if (value.isContainerNode()) {
    //                    //                    ArrayNode array = (ArrayNode) value;
    //                    //
    //                    //                    for (int i = 0; i < array.length(); i++) {
    //                    //                        doc.addField(key, array.get(i));
    //                    //                    }
    //                    //TODO
    //                }
    //                else {
    //                    //see https://lucene.apache.org/solr/guide/7_4/defining-fields.html
    //                    doc.addField(toSolrField(field.getKey()), value.textValue());
    //                }
    //            }
    //
    //            doc.addField("name", "Kenmore Dishwasher");
    //
    //            return doc;
    //        }
    //        catch (Exception e) {
    //            throw new RuntimeException(e);
    //        }
    //    }
    //String jsonStr = this.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(retVal);

    private static class RollbackBackup
    {
        private final String id;
        private final SolrDocument document;

        public RollbackBackup(Page page, SolrClient solrClient) throws IOException, SolrServerException
        {
            this.id = PageIndexEntry.generateId(page);
            this.document = solrClient.getById(this.id);
        }
    }
}

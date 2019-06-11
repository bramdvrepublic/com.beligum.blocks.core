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

package com.beligum.blocks.index.solr;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.tx.TX;
import com.beligum.blocks.index.AbstractIndexConnection;
import com.beligum.blocks.index.entries.JsonPageIndexEntry;
import com.beligum.blocks.index.ifaces.*;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.handler.UpdateRequestHandler;
import org.apache.solr.servlet.SolrRequestParsers;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by bram on 2/22/16.
 */
public class SolrPageIndexConnection extends AbstractIndexConnection implements IndexConnection
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

    /**
     * This defines which kind of transaction isolation type is used
     */
    public enum TxType
    {
        /**
         * This will effectively serialize all transactions in the parent indexer
         * and use the native commit/rollback of the Solr server. Note that this
         * is only effective if the server is used in embedded mode and won't be modified
         * from other clients because otherwise this doesn't make any sense.
         */
        EMBEDDED_FULL_SYNC_MODE,

        /**
         * This will assume everything goes well and try to revert the changes made if it doesn't
         * by loading in the existing data before making changes and re-entering the saved data
         * if things go wrong. This mode is not safe at all, but offers a lot more throughput
         * <p>
         * WARNING: this mode doesn't support full index deletion rollback using deleteAll() !!!
         */
        OPPORTUNISTIC_MODE
    }

    //-----VARIABLES-----
    private SolrPageIndexer pageIndexer;
    private SolrClient solrClient;
    private TxType txType;

    // Note that we can have multiple backups because this connection is part of a transaction
    // that can be re-used multiple times
    private List<RollbackBackup> rollbackBackups = new ArrayList<>();
    private final Object rollbackBackupLock = new Object();

    //-----CONSTRUCTORS-----
    SolrPageIndexConnection(SolrPageIndexer pageIndexer, TX transaction, String txResourceName, TxType txType) throws IOException
    {
        super(transaction, txResourceName);

        this.pageIndexer = pageIndexer;
        this.solrClient = pageIndexer.getSolrClient();
        this.txType = txType;
        this.active = true;

        // make sure this connection is released, eventually
        this.registerConnection();
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexEntry get(URI key) throws IOException
    {
        this.assertActive();

        PageIndexEntry retVal = null;

        try {
            //            SolrQuery query = new SolrQuery();
            //            query.setParam("q", "{!child of=" + PageIndexEntry.parentId.getName() + ":null}");
            //            QueryResponse response = this.solrClient.query(query);
            //            Logger.info("Got " + response.getResults().getNumFound() + " docs: " + json);
            //            for (SolrDocument doc : response.getResults()) {
            //                Logger.info(doc.jsonStr());
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
            //            for (SolrDocument doc : docList) {
            //                Logger.info(doc.jsonStr());
            //            }

            String pageId = PageIndexEntry.uriField.serialize(key);

            //for future use, needs testing first
            final boolean USE_REALTIME_API = false;

            if (USE_REALTIME_API) {
                //see https://github.com/apache/lucene-solr/blob/master/solr/solrj/src/test/org/apache/solr/client/solrj/SolrExampleTests.java#L1635
                SolrQuery query = new SolrQuery();
                query.setRequestHandler("/get");
                query.set("id", pageId);
                QueryResponse response = this.solrClient.query(query);
                SolrDocumentList results = response.getResults();
                if (results.size() == 1) {
                    retVal = new SolrPageIndexEntry(results.get(0).jsonStr());
                }
                else if (results.size() > 1) {
                    throw new IOException("Encountered multiple hits in Solr index for this ID, this shouldn't happen; " + pageId);
                }
            }
            else {
                SolrDocument doc = this.solrClient.getById(pageId);
                if (doc != null) {
                    retVal = new SolrPageIndexEntry(doc.jsonStr());
                }
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }

        return retVal;
    }
    @Override
    public synchronized void update(Resource resource) throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        try {
            Page page = resource.unwrap(Page.class);

            if (this.txType.equals(TxType.OPPORTUNISTIC_MODE)) {
                this.saveRollbackBackup(PageIndexEntry.uriField.serialize(page));
            }

            this.updateJsonDoc(new SolrPageIndexEntry(page));
        }
        catch (Exception e) {
            throw new IOException("Error while updating a Solr resource; " + resource, e);
        }
    }
    @Override
    public synchronized void delete(Resource resource) throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        try {
            Page page = resource.unwrap(Page.class);

            String pageId = PageIndexEntry.uriField.serialize(page);

            if (this.txType.equals(TxType.OPPORTUNISTIC_MODE)) {
                this.saveRollbackBackup(pageId);
            }

            this.solrClient.deleteById(pageId);
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
            // WARNING: this doesn't have support for opportunistic rollback
            this.solrClient.deleteByQuery("*:*");
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
        this.pageIndexer = null;
        this.transaction = null;
        this.active = false;
    }

    //-----PROTECTED METHODS-----
    @Override
    protected void begin() throws IOException
    {
        switch (this.txType) {
            case EMBEDDED_FULL_SYNC_MODE:

                //this will block if there's currently another TX active,
                // effectively serializing transactions so we can use the native commit/rollback mechanism
                this.pageIndexer.acquireCentralTxLock(this);

                // Note: there is no such thing as a Solr "begin" method;
                // changes are just queued and made visible when we call commit (either hard or soft)

                break;
            case OPPORTUNISTIC_MODE:

                // we don't really do anything special when the transaction starts

                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this.txType);
        }
    }
    @Override
    protected void prepareCommit() throws IOException
    {
        if (this.isRegistered()) {
            switch (this.txType) {
                case EMBEDDED_FULL_SYNC_MODE:
                    //NOOP
                    break;
                case OPPORTUNISTIC_MODE:
                    //NOOP
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + this.txType);
            }
        }
    }
    /**
     * Note: if this method throws an exception, rollback() will always be called before ending the transaction
     */
    @Override
    protected void commit() throws IOException
    {
        if (this.isRegistered()) {

            switch (this.txType) {
                case EMBEDDED_FULL_SYNC_MODE:

                    try {
                        // This will perform a 'soft commit'; eg. the changes will be appended to the transaction log,
                        // but won't be flushed to the index on disk. So on power failure, the actions in the log
                        // will need to be replayed. Note that Solr is configured to do a periodic hard commit (every 15 sec),
                        // so a soft commit basically only makes the changes visible to future search queries.
                        //
                        // From the docs:
                        // During a softcommit, the transaction log is not truncated, it continues to grow.
                        // However, a new searcher is opened, which makes the documents since last softcommit visible for searching.
                        // Also, some of the top-level caches in Solr are invalidated, so it’s not a completely free operation.
                        //
                        // See https://lucidworks.com/2013/08/23/understanding-transaction-logs-softcommit-and-commit-in-sorlcloud/
                        // In this post, the authors clearly state we shouldn't be calling commit manually because this could potentially
                        // softcommit the index very frequently. However, since page saving won't happen every millisecond, this should be okay,
                        // and allows us to wrap our TX methods around use the native lucene methods.
                        //
                        // WARNING: beware of re-indexing (when a lot of commits will happen per second), we should create a new mode for this.
                        this.solrClient.commit(false, false, true);

                        // Make sure the data gets saved eventually
                        // we'll manually schedule a hard commit ourself so the periodic auto-commit if Solr (which we disabled)
                        // can't slip in between our commits
                        this.pageIndexer.scheduleHardCommit(this);

                        // If all went well, release the lock. Note that rollback() will be called
                        // when this method throws an exception, so the lock should always be released
                        this.pageIndexer.releaseCentralTxLock(this);
                    }
                    catch (Throwable e) {
                        throw new IOException(e);
                    }

                    break;
                case OPPORTUNISTIC_MODE:

                    try {
                        //see above
                        this.solrClient.commit(false, false, true);

                        // make sure the data gets saved eventually
                        this.pageIndexer.scheduleHardCommit(this);

                        // All went well; wipe the temp-stored rollback backup entry
                        this.flushRollbackBackup();
                    }
                    catch (Throwable e) {
                        throw new IOException(e);
                    }

                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + this.txType);
            }
        }

        final boolean DEBUG = false;
        if (DEBUG) {

            try {
                SolrQuery query = new SolrQuery();
                query.setQuery("*:*");
                QueryResponse response = this.solrClient.query(query);
                SolrDocumentList docList = response.getResults();
                Logger.info("-------------------\nAll " + docList.getNumFound() + " docs");
                for (SolrDocument doc : docList) {
                    Logger.info(doc.jsonStr());
                }
                Logger.info("-------------------");
            }
            catch (Throwable e) {
                Logger.error(e);
            }

            try {
                Logger.info("##### DEBUG CODE");
                SolrQuery query = new SolrQuery();
                //query.setQuery("*:*");
                //query.setParam("q", "{!child of=" + PageIndexEntry.parentUriField.getName() + ":null}");
                query.setParam("q", "{!parent which=" + PageIndexEntry.parentUriField.getName() + ":null}");
                //query.setParam("q", "{!join from=" + PageIndexEntry.parentUriField.getName() + " to=" + PageIndexEntry.uriField.getName() + "}*");
                //            query.setParam("fl", "*,[child]");
                //            query.setParam("q", "{!graph from=uri to=parentUri maxDepth=1}typeOf:ror\\:BlogPost");
                QueryResponse response = this.solrClient.query(query);
                Logger.info("------ Response: \n" + response.jsonStr());
                SolrDocumentList docList = response.getResults();
                Logger.info("------------------- Selected " + docList.getNumFound() + " docs: \n");
                for (SolrDocument doc : docList) {
                    Logger.info(doc.jsonStr());
                }
                Logger.info("-------------------");
            }
            catch (Throwable e) {
                Logger.error(e);
            }
        }
    }
    @Override
    protected void rollback() throws IOException
    {
        if (this.isRegistered()) {

            switch (this.txType) {
                case EMBEDDED_FULL_SYNC_MODE:

                    try {
                        // From the docs:
                        // Performs a rollback of all non-committed documents pending. Note that this is not a true rollback as in databases.
                        // Content you have previously added may have been committed due to autoCommit, buffer full, other client performing a commit etc.
                        // Also note that rollbacks reset changes made by all clients. Use this method carefully when multiple clients, or multithreaded clients are in use.
                        //
                        // Note: we explicitly disabled autoCommit (both hard and soft), see SolrPageIndexer
                        this.solrClient.rollback();

                        //Note: don't cancel the scheduled hard commit because other threads/requests might have
                        // started it and have nothing to do with us.
                    }
                    catch (SolrServerException e) {
                        throw new IOException(e);
                    }
                    finally {
                        // Always release the lock.
                        // Note that rollback() will be called when commit throws an exception,
                        // so the lock should always be released
                        this.pageIndexer.releaseCentralTxLock(this);
                    }

                    break;
                case OPPORTUNISTIC_MODE:

                    try {
                        this.restoreRollbackBackup();
                    }
                    catch (SolrServerException e) {
                        throw new IOException(e);
                    }

                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + this.txType);
            }
        }
    }
    @Override
    protected Indexer getResourceManager()
    {
        return this.pageIndexer;
    }

    //-----PRIVATE METHODS-----
    private void updateJsonDoc(SolrPageIndexEntry indexEntry) throws IOException, SolrServerException
    {
        // now all sub-objects are attached to each other, recursively iterate them to find all the paths to
        // the sub-objects, so we can report to Solr where to split its children
        StringBuilder solrSplit = new StringBuilder();
        Set<String> splitRefs = new HashSet<>();
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
                // don't add it again if it was added already, it causes errors saying "... has a parent node at my level or lower"
                if (!splitRefs.contains(path)) {
                    if (solrSplit.length() > 0) {
                        solrSplit.append("|");
                    }
                    solrSplit.append(path);
                    splitRefs.add(path);
                }
            }
        });

        // see https://lucene.apache.org/solr/guide/7_7/uploading-data-with-index-handlers.html#json-formatted-index-updates
        // and https://lucene.apache.org/solr/guide/7_7/transforming-and-indexing-custom-json.html#setting-json-defaults
        // Note: the "/update/json/docs" path is a shortcut for "/update" with the contentType and the command=false already set right
        ContentStreamUpdateRequest request = new ContentStreamUpdateRequest(UpdateRequestHandler.DOC_PATH);

        // From the docs: if you have a unique key field, but you feel confident that you can safely bypass the uniqueness check
        // (eg: you build your indexes in batch, and your indexing code guarantees it never adds the same document more than once)
        // you can specify the overwrite="false" option when adding your documents.
        //true is the default, but this way, we clearly indicate what we're doing
        request.setParam(UpdateRequestHandler.OVERWRITE, "true");

        //the mime type is not strictly necessary (see comment above) but let's add it anyway
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

        final boolean DEBUG = false;
        if (DEBUG) {
            Logger.info("##### Indexed doc: \n\n " + indexEntry.toString() + "\n\n");
        }
    }
    private void saveRollbackBackup(String id) throws IOException, SolrServerException
    {
        synchronized (rollbackBackupLock) {

            //note: this returns null if nothing was found
            SolrDocument existingIndexEntry = this.solrClient.getById(id);

            this.rollbackBackups.add(new RollbackBackup(id, existingIndexEntry == null ? null : new SolrPageIndexEntry(existingIndexEntry.jsonStr())));
        }
    }
    private void restoreRollbackBackup() throws IOException, SolrServerException
    {
        synchronized (rollbackBackupLock) {

            for (RollbackBackup rollbackBackup : this.rollbackBackups) {

                //if the indexEntry is null, it means the index didn't have a prior entry
                if (rollbackBackup.indexEntry != null) {
                    this.updateJsonDoc(rollbackBackup.indexEntry);
                }
                else {
                    this.solrClient.deleteById(rollbackBackup.id);
                }
            }

            this.rollbackBackups.clear();
        }
    }
    private void flushRollbackBackup() throws IOException
    {
        synchronized (rollbackBackupLock) {

            this.rollbackBackups.clear();

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
        private final SolrPageIndexEntry indexEntry;

        public RollbackBackup(String id, SolrPageIndexEntry indexEntry)
        {
            this.id = id;
            this.indexEntry = indexEntry;
        }
    }
}

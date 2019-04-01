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

import com.beligum.base.database.models.ifaces.JsonObject;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.AbstractIndexConnection;
import com.beligum.blocks.filesystem.index.entries.pages.AbstractPageIndexEntry;
import com.beligum.blocks.filesystem.index.entries.pages.JsonPageIndexEntry;
import com.beligum.blocks.filesystem.index.entries.pages.SimplePageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.*;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ontologies.Meta;
import com.beligum.blocks.rdf.ontologies.RDFS;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.handler.UpdateRequestHandler;
import org.apache.solr.parser.QueryParser;
import org.apache.solr.schema.NestPathField;
import org.apache.solr.servlet.DirectSolrConnection;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import static com.beligum.base.templating.velocity.VelocityTemplateToolkit.json;

/**
 * Created by bram on 2/22/16.
 */
public class SolrPageIndexConnection extends AbstractIndexConnection implements PageIndexConnection
{
    //-----CONSTANTS-----
    private static final String TX_RESOURCE_NAME = SolrPageIndexConnection.class.getSimpleName();

    //-----VARIABLES-----
    private SolrPageIndexer pageIndexer;
    private SolrClient solrClient;
    private TX transaction;
    private boolean active;

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

            SolrDocument doc = this.solrClient.getById(AbstractPageIndexEntry.generateId(key).toString());
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

            //            this.solrClient.deleteById("1");
            //            this.solrClient.deleteById("2");
            //
            //            SolrInputDocument parent = new SolrInputDocument();
            //            parent.addField("id", "1");
            //            parent.addField("isParent", true);
            //            parent.addField("hehe", "parentValue");
            //            SolrInputDocument child = new SolrInputDocument();
            //            child.addField("id", "2");
            //            child.addField("hehe", "childValue");
            //            parent.addChildDocument(child);
            //            String res = parent.jsonStr();
            //            this.solrClient.add(parent);
            //
            //            Logger.info("Writing index entry: " + indexEntry.toString());

            //TODO check if we need this
            //this.solrClient.deleteById(indexEntry.getId());

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

            this.solrClient.request(request);

            //TODO comment
            this.solrClient.commit(false, false, true);

            SolrDocument doc = this.solrClient.getById(AbstractPageIndexEntry.generateId(resource.getUri()).toString());

            SolrQuery testQuery = new SolrQuery();
            //fq=(id:/en/blah)&rows=50&start=0
            //testQuery.setQuery("id:/en/blah");
            testQuery.setQuery("*:*");
            testQuery.addFilterQuery("id:/en/blah");
            QueryResponse testRes = this.solrClient.query(testQuery);

            IndexSearchRequest searchRequestBuilder = IndexSearchRequest.createFor(this)
                                                                        .filter(PageIndexEntry.id, resource.getUri().getPath(),
                                                                                IndexSearchRequest.FilterBoolean.OR);
            IndexSearchResult testres = this.search(searchRequestBuilder);

            Logger.info("");
        }
        catch (Exception e) {
            throw new IOException("Error while updating a Solr resource; " + resource, e);
        }

        //if (true) throw new IOException("DEBUG");

        //TODO
    }
    @Override
    public synchronized void delete(Resource resource) throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        Page page = resource.unwrap(Page.class);

        //TODO
    }
    @Override
    public synchronized void deleteAll() throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        //TODO
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
    public IndexSearchResult search(String query) throws IOException
    {
        this.assertActive();

        //TODO

        return null;
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
            //            this.pageIndexer.getIndexWriter().prepareCommit();
        }
    }
    @Override
    protected void commit() throws IOException
    {
        if (this.isRegistered()) {
            //            this.pageIndexer.getIndexWriter().commit();
            //
            //            //mark all readers and searchers to reload
            //            this.pageIndexer.indexChanged();
        }
    }
    @Override
    protected void rollback() throws IOException
    {
        if (this.isRegistered()) {
            //            this.pageIndexer.getIndexWriter().rollback();
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
}

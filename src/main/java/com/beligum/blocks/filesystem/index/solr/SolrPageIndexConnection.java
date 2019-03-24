///*
// * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.beligum.blocks.filesystem.index;
//
//import com.beligum.base.resources.ifaces.Resource;
//import com.beligum.blocks.filesystem.hdfs.TX;
//import com.beligum.blocks.filesystem.index.entries.pages.LuceneDocFactory;
//import com.beligum.blocks.filesystem.index.entries.pages.LuceneIndexSearchResult;
//import com.beligum.blocks.filesystem.index.entries.pages.SimplePageIndexEntry;
//import com.beligum.blocks.filesystem.index.ifaces.*;
//import com.beligum.blocks.filesystem.pages.PageModel;
//import com.beligum.blocks.filesystem.pages.ifaces.Page;
//import com.beligum.blocks.rdf.ifaces.RdfProperty;
//import com.beligum.blocks.rdf.ontologies.endpoints.LocalQueryEndpoint;
//import com.beligum.blocks.utils.RdfTools;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.lang3.math.NumberUtils;
//import org.apache.lucene.analysis.TokenStream;
//import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.index.IndexWriter;
//import org.apache.lucene.index.Term;
//import org.apache.lucene.queryparser.classic.QueryParser;
//import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
//import org.apache.lucene.search.*;
//
//import java.io.IOException;
//import java.io.StringReader;
//import java.net.URI;
//import java.util.Map;
//
///**
// * Created by bram on 2/22/16.
// */
//public class SolrPageIndexConnection extends AbstractIndexConnection implements PageIndexConnection
//{
//    //-----CONSTANTS-----
//
//    //-----VARIABLES-----
//    private SolrPageIndexer pageIndexer;
//    private TX transaction;
//    private boolean active;
//
//    //-----CONSTRUCTORS-----
//    public SolrPageIndexConnection(SolrPageIndexer pageIndexer, TX transaction)
//    {
//        this.pageIndexer = pageIndexer;
//        this.transaction = transaction;
//        this.active = true;
//    }
//
//    //-----PUBLIC METHODS-----
//    @Override
//    public PageIndexEntry get(URI key) throws IOException
//    {
//        this.assertActive();
//
//        //TODO
//    }
//    @Override
//    public synchronized void delete(Resource resource) throws IOException
//    {
//        this.assertActive();
//        this.assertTransaction();
//
//        Page page = resource.unwrap(Page.class);
//
//        //TODO
//    }
//    @Override
//    public synchronized void update(Resource resource) throws IOException
//    {
//        this.assertActive();
//        this.assertTransaction();
//
//        Page page = resource.unwrap(Page.class);
//
//        //TODO
//    }
//    @Override
//    public synchronized void deleteAll() throws IOException
//    {
//        this.assertActive();
//        this.assertTransaction();
//
//        //TODO
//    }
//    @Override
//    //Note: this needs to be synchronized for concurrency with the the assertActive() below
//    public synchronized void close() throws IOException
//    {
//        //don't do this anymore: we switched from a new writer per transaction to a single writer
//        // which instead flushes at the end of each transactions, so don't close it
//        //        if (this.createdWriter) {
//        //            this.getLuceneIndexWriter().close();
//        //        }
//
//        this.pageIndexer = null;
//        this.transaction = null;
//        this.active = false;
//    }
//    @Override
//    public IndexSearchResult search(Query luceneQuery, RdfProperty sortField, boolean sortReversed, int pageSize, int pageOffset) throws IOException
//    {
//        this.assertActive();
//
//        long searchStart = System.currentTimeMillis();
//
//        //TODO
//    }
//
//    //-----PROTECTED METHODS-----
//    @Override
//    protected void begin() throws IOException
//    {
//        //note: there's not such thing as a .begin();
//        // the begin is just where the last .commit() left off
//    }
//    @Override
//    protected void prepareCommit() throws IOException
//    {
//        if (this.isRegistered()) {
//            this.pageIndexer.getIndexWriter().prepareCommit();
//        }
//    }
//    @Override
//    protected void commit() throws IOException
//    {
//        if (this.isRegistered()) {
//            this.pageIndexer.getIndexWriter().commit();
//
//            //mark all readers and searchers to reload
//            this.pageIndexer.indexChanged();
//        }
//    }
//    @Override
//    protected void rollback() throws IOException
//    {
//        if (this.isRegistered()) {
//            this.pageIndexer.getIndexWriter().rollback();
//        }
//    }
//    @Override
//    protected Indexer getResourceManager()
//    {
//        return this.pageIndexer;
//    }
//
//    //-----PRIVATE METHODS-----
//
//}

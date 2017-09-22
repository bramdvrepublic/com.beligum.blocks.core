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

package com.beligum.blocks.filesystem.index;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.entries.pages.*;
import com.beligum.blocks.filesystem.index.ifaces.Indexer;
import com.beligum.blocks.filesystem.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexConnection;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

/**
 * Created by bram on 2/22/16.
 */
public class LucenePageIndexConnection extends AbstractIndexConnection implements PageIndexConnection, LuceneQueryConnection
{
    //-----CONSTANTS-----
    //this is the default number of maximum search results that will be returned when no specific value is passed
    public static final int DEFAULT_MAX_SEARCH_RESULTS = 1000;

    //-----VARIABLES-----
    private LucenePageIndexer pageIndexer;
    private TX transaction;
    private boolean registeredTransaction;
    private boolean active;

    //-----CONSTRUCTORS-----
    public LucenePageIndexConnection(LucenePageIndexer pageIndexer, TX transaction) throws IOException
    {
        this.pageIndexer = pageIndexer;
        this.transaction = transaction;
        this.registeredTransaction = false;
        this.active = true;
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexEntry get(URI key) throws IOException
    {
        this.assertActive();

        //since we treat all URIs as relative, we only take the path into account
        TermQuery query = new TermQuery(AbstractPageIndexEntry.toLuceneId(StringFunctions.getRightOfDomain(key)));
        TopDocs topdocs = this.pageIndexer.getIndexSearcher().search(query, 1);

        if (topdocs.scoreDocs.length == 0) {
            return null;
        }
        else {
            return SimplePageIndexEntry.fromLuceneDoc(this.pageIndexer.getIndexSearcher().doc(topdocs.scoreDocs[0].doc));
        }
    }
    @Override
    public synchronized void delete(Resource resource) throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        Page page = resource.unwrap(Page.class);

        //don't use the canonical address as the id of the entry: it's not unique (will be the same for different languages)
        this.pageIndexer.getIndexWriter().deleteDocuments(AbstractPageIndexEntry.toLuceneId(page.getPublicRelativeAddress()));

        //for debug
        //this.printLuceneIndex();
    }
    @Override
    public synchronized void update(Resource resource) throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        Page page = resource.unwrap(Page.class);

        DeepPageIndexEntry indexExtry = new DeepPageIndexEntry(page);

        //let's not mix-and-mingle writes (even though the IndexWriter is thread-safe),
        // so we can do a clean commit/rollback on our own
        this.pageIndexer.getIndexWriter().updateDocument(AbstractPageIndexEntry.toLuceneId(indexExtry), indexExtry.createLuceneDoc());

        //for debug
        //this.printLuceneIndex();
    }
    @Override
    public synchronized void deleteAll() throws IOException
    {
        this.assertActive();
        this.assertTransaction();

        this.pageIndexer.getIndexWriter().deleteAll();
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
        this.registeredTransaction = false;
        this.active = false;
    }
    @Override
    public IndexSearchResult search(Query luceneQuery, RdfProperty sortField, boolean sortReversed, int pageSize, int pageOffset) throws IOException
    {
        this.assertActive();

        long searchStart = System.currentTimeMillis();
        IndexSearcher indexSearcher = this.pageIndexer.getIndexSearcher();

        //keep supplied values within reasonable bounds
        //Note that Lucene always expects numHits to be > 0!
        int validPageSize = pageSize <= 0 ? DEFAULT_MAX_SEARCH_RESULTS : pageSize;
        int validPageOffset = pageOffset < 0 ? 0 : pageOffset;

        //see http://stackoverflow.com/questions/29695307/sortiing-string-field-alphabetically-in-lucene-5-0
        //see http://www.gossamer-threads.com/lists/lucene/java-user/203857
        Sort sort = null;
        if (sortField != null) {
            sort = new Sort(/*SortField.FIELD_SCORE,*/new SortField(sortField.getCurieName().toString(), SortField.Type.STRING, sortReversed));

            //found this here, I suppose we need to call this to allow very specific sort fields?
            //  https://svn.alfresco.com/repos/alfresco-open-mirror/alfresco/HEAD/root/projects/solr4/source/java/org/alfresco/solr/query/AlfrescoReRankQParserPlugin.java
            sort = sort.rewrite(indexSearcher);
        }

        TopDocs results;
        if (validPageOffset == 0) {
            results = sort == null ? indexSearcher.search(luceneQuery, validPageSize) : indexSearcher.search(luceneQuery, validPageSize, sort);
        }
        else {
            int maxResultSize = validPageSize;
            TopDocs tempResults = sort == null ? indexSearcher.search(luceneQuery, maxResultSize) : indexSearcher.search(luceneQuery, maxResultSize, sort);
            if (tempResults.scoreDocs.length > 0) {
                ScoreDoc last = tempResults.scoreDocs[tempResults.scoreDocs.length - 1];
                int currPageIdx = 0;
                boolean keepSearching = true;
                while (keepSearching && ++currPageIdx <= validPageOffset) {
                    //if we're in the last page, we shouldn't return the size of an entire page
                    // because it will be padded with other results (seems like lucene wraps around?)
                    if ((currPageIdx + 1) * validPageSize > tempResults.totalHits) {
                        maxResultSize = tempResults.totalHits % validPageSize;
                        //this will make sure oversized index values won't lead to out of bounds exceptions
                        keepSearching = false;
                    }

                    tempResults = sort == null ? indexSearcher.searchAfter(last, luceneQuery, maxResultSize) : indexSearcher.searchAfter(last, luceneQuery, maxResultSize, sort);
                    last = tempResults.scoreDocs[tempResults.scoreDocs.length - 1];
                }
            }

            results = tempResults;
        }

        return new LuceneIndexSearchResult(indexSearcher, results, validPageOffset, validPageSize, System.currentTimeMillis() - searchStart);
    }
    @Override
    public IndexSearchResult search(Query luceneQuery, int maxResults) throws IOException
    {
        this.assertActive();

        return this.search(luceneQuery, null, false, maxResults, 0);
    }
    @Override
    public Query buildWildcardQuery(String fieldName, String phrase) throws IOException
    {
        this.assertActive();

        Query retVal = null;

        if (StringUtils.isEmpty(fieldName)) {
            fieldName = LucenePageIndexer.CUSTOM_FIELD_ALL;
        }

        boolean isNumber = NumberUtils.isNumber(phrase);

        try {
            //we'll use a complex phrase query parser because a regular phrase parser doesn't support wildcards
            ComplexPhraseQueryParser complexPhraseParser = new ComplexPhraseQueryParser(fieldName, LucenePageIndexer.ACTIVE_ANALYZER);

            //Note: the escaping of the special characters is not really necessary, because they'll be removed in the analyzer below anyway
            String safePhrase = QueryParser.escape(phrase.trim());

            //Instead of relying on the analyzer in the parser, we'll pass the search phrase through the analyzer ourself,
            // this allows us to interact with the terms more closely
            //Note: the standard analyzer doesn't index special characters (eg. "blah (en)" gets indexed as "blah" and "en").
            StringBuilder sb = new StringBuilder();
            boolean multiword = false;
            try (TokenStream stream = LucenePageIndexer.ACTIVE_ANALYZER.tokenStream(fieldName, new StringReader(safePhrase))) {
                CharTermAttribute termAttr = stream.getAttribute(CharTermAttribute.class);
                stream.reset();
                boolean hasNext = stream.incrementToken();
                while (hasNext) {
                    String term = termAttr.toString();
                    if (sb.length() > 0) {
                        sb.append(" ");
                        multiword = true;
                    }
                    sb.append(term);

                    hasNext = stream.incrementToken();

                    //makes sense to _not_ add the wildcard * expansion to numbers, no? Otherwise, 15 would also match 15000, which is somewhat weird/wrong.
                    if (!isNumber && !hasNext) {
                        sb.append("*");
                    }
                }
            }

            String parsedQuery = sb.toString();

            //this check is necessary because in Lucene, '"bram"' doesn't seem to match 'bram'
            if (multiword) {
                //Update: instead of searching for the exact phrase, we switched to an ordered search where
                //        all terms need to be present
                complexPhraseParser.setInOrder(true);
                complexPhraseParser.setDefaultOperator(QueryParser.Operator.AND);
                //parsedQuery = "\"" + parsedQuery + "\"";
            }

            retVal = complexPhraseParser.parse(parsedQuery);
        }
        catch (Exception e) {
            throw new IOException("Error while building Lucene wildcard query for '" + phrase + "' on field '" + fieldName + "'", e);
        }

        return retVal;
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
        if (this.registeredTransaction) {
            this.pageIndexer.getIndexWriter().prepareCommit();
        }
    }
    @Override
    protected void commit() throws IOException
    {
        if (this.registeredTransaction) {
            this.pageIndexer.getIndexWriter().commit();

            //mark all readers and searchers to reload
            this.pageIndexer.indexChanged();
        }
    }
    @Override
    protected void rollback() throws IOException
    {
        if (this.registeredTransaction) {
            this.pageIndexer.getIndexWriter().rollback();
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
            if (!this.registeredTransaction) {
                //attach this connection to the transaction manager
                this.transaction.registerResource(this);
                this.registeredTransaction = true;
            }
        }
    }
}

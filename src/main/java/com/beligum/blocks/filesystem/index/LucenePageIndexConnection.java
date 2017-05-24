package com.beligum.blocks.filesystem.index;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.entries.IndexEntry;
import com.beligum.blocks.filesystem.index.entries.pages.*;
import com.beligum.blocks.filesystem.index.ifaces.Indexer;
import com.beligum.blocks.filesystem.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexConnection;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import jersey.repackaged.com.google.common.collect.Sets;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by bram on 2/22/16.
 */
public class LucenePageIndexConnection extends AbstractIndexConnection implements PageIndexConnection, LuceneQueryConnection
{
    //-----CONSTANTS-----
    private static final Set<String> INDEX_FIELDS_TO_LOAD = Sets.newHashSet(PageIndexEntry.Field.object.name());

    //this is a cap value for the most results we'll ever return
    // (eg. the size of the array that will hold the values to be sorted before pagination)
    public static final int MAX_SEARCH_RESULTS = 1000;

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

        List<IndexEntry> retVal = new ArrayList<>();

        long searchStart = System.currentTimeMillis();
        IndexSearcher indexSearcher = this.pageIndexer.getIndexSearcher();
        TopDocsCollector mainCollector = null;

        //keep supplied values within reasonable bounds
        //note that MAX_SEARCH_RESULTS should always be larger than pageSize -> verify it here and adjust if needed
        //also note we can use a negative pageSize to 'disable' fixed sizing
        int validPageSize = pageSize < 0 ? MAX_SEARCH_RESULTS : Math.min(pageSize, MAX_SEARCH_RESULTS);
        int validMaxResults = Math.max(pageSize, MAX_SEARCH_RESULTS);
        int validPageOffset = pageOffset < 0 ? 0 : (pageOffset * validPageSize > validMaxResults ? (int) Math.floor(validMaxResults / validPageSize) : pageOffset);

        //see http://stackoverflow.com/questions/29695307/sortiing-string-field-alphabetically-in-lucene-5-0
        //see http://www.gossamer-threads.com/lists/lucene/java-user/203857
        if (sortField != null) {
            Sort sort = new Sort(/*SortField.FIELD_SCORE,*/new SortField(sortField.getCurieName().toString(), SortField.Type.STRING, sortReversed));

            //found this here, I suppose we need to call this to allow very specific sort fields?
            //  https://svn.alfresco.com/repos/alfresco-open-mirror/alfresco/HEAD/root/projects/solr4/source/java/org/alfresco/solr/query/AlfrescoReRankQParserPlugin.java
            sort = sort.rewrite(indexSearcher);

            mainCollector = TopFieldCollector.create(sort, validMaxResults, null, true, false, false);
        }
        else {
            //Actually, I suppose we could write this as a TopFieldCollector, sorting on SortField.FIELD_SCORE, no?
            mainCollector = TopScoreDocCollector.create(validMaxResults);
        }

        //TODO: if using deep paging, we should refactor and start using searchAfter() instead
        indexSearcher.search(luceneQuery, mainCollector);

        TopDocs hits = mainCollector.topDocs(validPageOffset * validPageSize, validPageSize);
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            retVal.add(SimplePageIndexEntry.fromLuceneDoc(indexSearcher.doc(scoreDoc.doc, INDEX_FIELDS_TO_LOAD)));
        }

        return new IndexSearchResult(retVal, mainCollector.getTotalHits(), validPageOffset, validPageSize, System.currentTimeMillis() - searchStart);
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
            fieldName = LucenePageIndexer.CUSTOM_FIELD_ALL_ANALYZED;
        }

        boolean isNumber = NumberUtils.isNumber(phrase);

        try {
            //we'll use a complex phrase query parser because a regular phrase parser doesn't support wildcards
            ComplexPhraseQueryParser complexPhraseParser = new ComplexPhraseQueryParser(fieldName, LucenePageIndexer.DEFAULT_ANALYZER);
            complexPhraseParser.setInOrder(true);

            //Note: the escaping of the special characters is not really necessary, because they'll be removed in the analyzer below anyway
            String safePhrase = QueryParser.escape(phrase.trim());

            //Instead of relying on the analyzer in the parser, we'll pass the search phrase through the analyzer ourself,
            // this allows us to interact with the terms more closely
            //Note: the standard analyzer doesn't index special characters (eg. "blah (en)" gets indexed as "blah" and "en").
            StringBuilder sb = new StringBuilder();
            boolean multiword = false;
            try (TokenStream stream = LucenePageIndexer.DEFAULT_ANALYZER.tokenStream(fieldName, new StringReader(safePhrase))) {
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
                parsedQuery = "\"" + parsedQuery + "\"";
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

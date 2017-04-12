package com.beligum.blocks.filesystem.index;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
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
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FSLockFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static FSLockFactory luceneLockFactory = FSLockFactory.getDefault();

    private LucenePageIndexer pageIndexer;
    private TX transaction;
    private boolean createdWriter;
    private boolean active;

    //-----CONSTRUCTORS-----
    public LucenePageIndexConnection(LucenePageIndexer pageIndexer, TX transaction) throws IOException
    {
        this.pageIndexer = pageIndexer;
        this.transaction = transaction;
        this.createdWriter = false;
        this.active = true;
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexEntry get(URI key) throws IOException
    {
        this.assertActive();

        //since we treat all URIs as relative, we only take the path into account
        TermQuery query = new TermQuery(AbstractPageIndexEntry.toLuceneId(StringFunctions.getRightOfDomain(key)));
        TopDocs topdocs = getLuceneIndexSearcher().search(query, 1);

        if (topdocs.scoreDocs.length == 0) {
            return null;
        }
        else {
            return SimplePageIndexEntry.fromLuceneDoc(getLuceneIndexSearcher().doc(topdocs.scoreDocs[0].doc));
        }
    }
    @Override
    public void delete(Resource resource) throws IOException
    {
        this.assertActive();

        Page page = resource.unwrap(Page.class);

        //don't use the canonical address as the id of the entry: it's not unique (will be the same for different languages)
        this.getLuceneIndexWriter().deleteDocuments(AbstractPageIndexEntry.toLuceneId(page.getPublicRelativeAddress()));

        //for debug
        //this.printLuceneIndex();
    }
    @Override
    public void update(Resource resource) throws IOException
    {
        this.assertActive();

        Page page = resource.unwrap(Page.class);

        DeepPageIndexEntry indexExtry = new DeepPageIndexEntry(page);

        //let's not mix-and-mingle writes (even though the IndexWriter is thread-safe),
        // so we can do a clean commit/rollback on our own
        this.getLuceneIndexWriter().updateDocument(AbstractPageIndexEntry.toLuceneId(indexExtry), indexExtry.createLuceneDoc());

        //for debug
        //this.printLuceneIndex();
    }
    @Override
    public void deleteAll() throws IOException
    {
        this.assertActive();

        this.getLuceneIndexWriter().deleteAll();
    }
    @Override
    public IndexSearchResult search(Query luceneQuery, RdfProperty sortField, boolean sortReversed, int pageSize, int pageOffset) throws IOException
    {
        this.assertActive();

        List<IndexEntry> retVal = new ArrayList<>();

        long searchStart = System.currentTimeMillis();
        IndexSearcher indexSearcher = getLuceneIndexSearcher();
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
            retVal.add(SimplePageIndexEntry.fromLuceneDoc(getLuceneIndexSearcher().doc(scoreDoc.doc, INDEX_FIELDS_TO_LOAD)));
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
    public Query buildWildcardQuery(String fieldName, String phrase, boolean complex) throws IOException
    {
        this.assertActive();

        Query retVal = null;

        if (StringUtils.isEmpty(fieldName)) {
            fieldName = LucenePageIndexer.CUSTOM_FIELD_ALL;
        }

        //makes sense to _not_ add the wildcard * expansion to numbers, no?
        String wildcardSuffix = NumberUtils.isNumber(phrase) ? "" : "*";

        if (!complex) {
            QueryParser queryParser = new QueryParser(fieldName, LucenePageIndexer.DEFAULT_ANALYZER);
            //we need to escape the wildcard query, and append the asterisk afterwards (or it will be escaped)
            try {
                retVal = queryParser.parse(QueryParser.escape(phrase) + wildcardSuffix);
            }
            catch (ParseException e) {
                throw new IOException("Error while building simple Lucene wildcard query for '" + phrase + "' on field '" + fieldName + "'", e);
            }
        }
        else {
            //we need to escape the wildcard query, and append the asterisk afterwards (or it will be escaped)
            ComplexPhraseQueryParser complexPhraseParser = new ComplexPhraseQueryParser(fieldName, LucenePageIndexer.DEFAULT_ANALYZER);
            complexPhraseParser.setInOrder(true);
            //this is tricky: using an asterisk after a special character seems to throw lucene off
            // since the standard analyzer doesn't index those characters anyway (eg. "blah (en)" gets indexed as "blah" and "en"),
            // it's safe to delete those special characters and just add the asterisk
            //Update: no, had some weird issues when looking for eg. "tongs/scissors-shaped" -> worked better when replacing with a space instead of deleting them
            String parsedQuery = removeEscapedChars(phrase, " ").trim();
            String queryStr = null;
            //this check is needed because "\"bram*\"" doesn't seem to match the "bram" token
            if (parsedQuery.contains(" ")) {
                queryStr = "\"" + parsedQuery + wildcardSuffix + "\"";
            }
            else {
                queryStr = parsedQuery + wildcardSuffix;
            }

            try {
                retVal = complexPhraseParser.parse(queryStr);
            }
            catch (ParseException e) {
                throw new IOException("Error while building complex Lucene wildcard query for '" + phrase + "' on field '" + fieldName + "'", e);
            }
        }

        return retVal;
    }
    @Override
    public void close() throws IOException
    {
        //don't do this anymore: we switched from a new writer per transaction to a single writer, so don't close it
        //        if (this.createdWriter) {
        //            this.getLuceneIndexWriter().close();
        //        }

        this.pageIndexer = null;
        this.transaction = null;
        this.createdWriter = false;
        this.active = false;
    }

    //-----PUBLIC STATIC METHODS-----
    //exactly the same code as QueryParserBase.escape(), but with the sb.append('\\'); line commented and added an else-part
    public static String removeEscapedChars(String s, String replacement)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
                || c == '*' || c == '?' || c == '|' || c == '&' || c == '/') {
                //sb.append('\\');
                sb.append(replacement);
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    //-----PROTECTED METHODS-----
    @Override
    protected void begin() throws IOException
    {
        //note: there's not such thing as a .begin(); the begin is just where the last .commit() left off
    }
    @Override
    protected void prepareCommit() throws IOException
    {
        if (this.createdWriter) {
            this.getLuceneIndexWriter().prepareCommit();
        }
    }
    @Override
    protected void commit() throws IOException
    {
        if (this.createdWriter) {
            this.getLuceneIndexWriter().commit();

            //mark all readers and searchers to reload
            this.indexChanged();
        }
    }
    @Override
    protected void rollback() throws IOException
    {
        if (this.createdWriter) {
            this.getLuceneIndexWriter().rollback();
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
    private void printLuceneIndex() throws IOException
    {
        Directory dir = FSDirectory.open(getDocDir());

        try (IndexReader reader = DirectoryReader.open(dir)) {
            int numDocs = reader.numDocs();
            for (int i = 0; i < numDocs; i++) {
                Document d = reader.document(i);
                System.out.println(i + ") " + d);
            }
        }
    }
    private void indexChanged()
    {
        //will be re-initialized on next read/search
        R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_SEARCHER);
    }
    private synchronized IndexSearcher getLuceneIndexSearcher() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_SEARCHER)) {

            IndexReader indexReader = DirectoryReader.open(FSDirectory.open(getDocDir()));
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);

            R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_SEARCHER, indexSearcher);
        }

        return (IndexSearcher) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_SEARCHER);
    }
    private synchronized IndexWriter getLuceneIndexWriter() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_WRITER)) {
            R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_WRITER, buildNewLuceneIndexWriter(getDocDir()));
        }

        //Note that a Lucene rollback closes the index for concurrency reasons, so double-check
        IndexWriter retVal = (IndexWriter) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_WRITER);
        if (retVal == null || !retVal.isOpen()) {
            R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_WRITER, retVal = buildNewLuceneIndexWriter(getDocDir()));
        }

        //register the writer on the first time we need it in this connection
        if (!this.createdWriter) {
            //attach this connection to the transaction manager
            this.transaction.registerResource(this, new TX.Listener()
            {
                @Override
                public void transactionEnded(int status)
                {
                    try {
                        close();
                    }
                    catch (IOException e) {
                        Logger.error("Error while closing a Lucene indexer connection after TX end", e);
                    }
                }
            });
            this.createdWriter = true;
        }

        return retVal;
    }
    private synchronized static Path getDocDir() throws IOException
    {
        final java.nio.file.Path retVal = Paths.get(Settings.instance().getPageMainIndexFolder());

        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_BOOTED)) {
            if (!Files.exists(retVal)) {
                Files.createDirectories(retVal);
            }
            if (!Files.isWritable(retVal)) {
                throw new IOException("Lucene index directory is not writable, please check the permissions of: " + retVal);
            }

            try (IndexWriter indexWriter = buildNewLuceneIndexWriter(retVal)) {
                //just open and close the writer once to create the necessary files,
                // else we'll get a "no segments* file found" exception on first search
            }

            //we built it at least once, save that for later checking
            R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_BOOTED, true);
        }

        return retVal;
    }
    /**
     * From the Lucene JavaDoc:
     * "IndexWriter instances are completely thread safe, meaning multiple threads can call any of its methods, concurrently."
     * so I hope it's ok to keep this open.
     * Note: switched to instance-generation because an open writer seemed to block access to the directory with a .lock file?
     * <p/>
     * Reading here, it seems to be an OK usecase:
     * http://stackoverflow.com/questions/8878448/lucene-good-practice-and-thread-safety
     *
     * @return
     * @throws IOException
     */
    private static IndexWriter buildNewLuceneIndexWriter(Path docDir) throws IOException
    {
        IndexWriterConfig iwc = new IndexWriterConfig(LucenePageIndexer.DEFAULT_ANALYZER);

        // Add new documents to an existing index:
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        return new IndexWriter(FSDirectory.open(docDir, luceneLockFactory), iwc);
    }
}

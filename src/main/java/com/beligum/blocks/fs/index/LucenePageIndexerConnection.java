package com.beligum.blocks.fs.index;

import com.beligum.base.server.R;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.entries.pages.AbstractPageIndexEntry;
import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.fs.index.entries.pages.SimplePageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.pages.ReadOnlyPage;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.hadoop.fs.FileContext;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bram on 2/22/16.
 */
public class LucenePageIndexerConnection extends AbstractIndexConnection implements PageIndexConnection, LuceneQueryConnection
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private IndexWriter indexWriter;

    //-----CONSTRUCTORS-----
    public LucenePageIndexerConnection() throws IOException
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexEntry get(URI key) throws IOException
    {
        //since we treat all URIs as relative, we only take the path into account
        TermQuery query = new TermQuery(AbstractPageIndexEntry.toLuceneId(StringFunctions.getRightOfDomain(key)));
        TopDocs topdocs = getLuceneIndexSearcher().search(query, 1);

        if (topdocs.scoreDocs.length == 0) {
            return null;
        }
        else {
            return SimplePageIndexEntry.fromLuceneDoc(getLuceneIndexReader().document(topdocs.scoreDocs[0].doc));
        }
    }
    @Override
    public void delete(Page page) throws IOException
    {
        this.assertWriterTransaction();

        //don't use the canonical address as the id of the entry: it's not unique (will be the same for different languages)
        this.indexWriter.deleteDocuments(AbstractPageIndexEntry.toLuceneId(page.getPublicRelativeAddress()));

        //for debug
        //this.printLuceneIndex();
    }
    @Override
    public void update(Page page) throws IOException
    {
        this.assertWriterTransaction();

        SimplePageIndexEntry indexExtry = new SimplePageIndexEntry(page);

        //let's not mix-and-mingle writes (even though the IndexWriter is thread-safe),
        // so we can do a clean commit/rollback on our own
        this.indexWriter.updateDocument(AbstractPageIndexEntry.toLuceneId(indexExtry), SimplePageIndexEntry.toLuceneDoc(indexExtry));

        //for debug
        //this.printLuceneIndex();
    }
    @Override
    public List<PageIndexEntry> search(FieldQuery[] fieldQueries, int maxResults) throws IOException
    {
        List<PageIndexEntry> retVal = new ArrayList<>();

        try {
            retVal = this.search(this.buildLuceneQuery(fieldQueries), maxResults);
        }
        catch (ParseException e) {
            throw new IOException("Error while parsing multi-field lucene query; " + fieldQueries, e);
        }

        return retVal;
    }
    @Override
    public List<PageIndexEntry> search(Query luceneQuery, int maxResults) throws IOException
    {
        List<PageIndexEntry> retVal = new ArrayList<>();

        //old code when experimenting with lucene grouping to select the best double resource URI result (eg. for language selection)
        //        Sort groupSort = new Sort();
        //        groupSort.setSort(new SortField(PageIndexEntry.Field.resource.name(), SortField.Type.STRING, true)/*, new SortField("progress", SortField.FLOAT, true)*/);
        //        TermFirstPassGroupingCollector c1 = new TermFirstPassGroupingCollector(PageIndexEntry.Field.resource.name(), Sort.RELEVANCE, maxResults);
        //
        //        getLuceneIndexSearcher().search(luceneQuery, c1);
        //        boolean fillFields = true;
        //        Collection<SearchGroup<BytesRef>> topGroups = c1.getTopGroups(0, fillFields);
        //
        //        if (topGroups == null) {
        //            // No groups matched
        //            return retVal;
        //        }
        //        boolean getScores = true;
        //        boolean getMaxScores = true;
        //        TermSecondPassGroupingCollector c2 = new TermSecondPassGroupingCollector("author", topGroups, groupSort, docSort, docOffset + docsPerGroup, getScores, getMaxScores, fillFields);

        TopDocs topdocs = getLuceneIndexSearcher().search(luceneQuery, maxResults);
        //TODO this is probably not so efficient?
        for (ScoreDoc scoreDoc : topdocs.scoreDocs) {
            retVal.add(SimplePageIndexEntry.fromLuceneDoc(getLuceneIndexReader().document(scoreDoc.doc)));
        }

        return retVal;
    }
    @Override
    protected void begin() throws IOException
    {
        //note: there's not such thing as a .begin(); the begin is just where the last .commit() left off
    }
    @Override
    protected void prepareCommit() throws IOException
    {
        if (this.indexWriter != null) {
            this.indexWriter.prepareCommit();
        }
    }
    @Override
    protected void commit() throws IOException
    {
        if (this.indexWriter != null) {
            this.indexWriter.commit();
        }
    }
    @Override
    protected void rollback() throws IOException
    {
        if (this.indexWriter != null) {
            this.indexWriter.rollback();
        }
    }
    @Override
    public void close() throws IOException
    {
        if (this.indexWriter != null) {
            this.indexWriter.close();
            this.indexWriter = null;
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private BooleanQuery buildLuceneQuery(FieldQuery[] fieldQueries) throws ParseException
    {
        BooleanQuery query = new BooleanQuery();
        Map<Integer, BooleanQuery> groups = new HashMap<>();
        for (FieldQuery q : fieldQueries) {

            BooleanQuery activeQuery = query;
            if (q.getGroup() != null) {
                if (groups.containsKey(q.getGroup())) {
                    activeQuery = groups.get(q.getGroup());
                }
                else {
                    activeQuery = new BooleanQuery();
                    groups.put(q.getGroup(), activeQuery);
                    //TODO hmm, this (FILTER = and) won't always be true...
                    query.add(activeQuery, BooleanClause.Occur.FILTER);
                }
            }

            Query subQuery = null;
            switch (q.getType()) {
                case EXACT:
                    //note that we must not escape an exact value (eg. it doesn't work if you do)
                    subQuery = new TermQuery(new Term(q.getField().name(), q.getQuery()));
                    break;
                case WILDCARD:
                    QueryParser queryParser = new QueryParser(q.getField().name(), DEFAULT_ANALYZER);
                    //we need to escape the wildcard query, and append the asterisk afterwards (or it will be escaped)
                    subQuery = queryParser.parse(QueryParser.escape(q.getQuery()) + "*");
                    break;
                case WILDCARD_COMPLEX:
                    //we need to escape the wildcard query, and append the asterisk afterwards (or it will be escaped)
                    ComplexPhraseQueryParser complexPhraseParser = new ComplexPhraseQueryParser(q.getField().name(), DEFAULT_ANALYZER);
                    complexPhraseParser.setInOrder(true);
                    //this is tricky: using an asterisk after a special character seems to throw lucene off
                    // since the standard analyzer doesn't index those characters anyway (eg. "blah (en)" gets indexed as "blah" and "en"),
                    // it's safe to delete those special characters and just add the asterisk
                    String parsedQuery = this.removeEscapedChars(q.getQuery()).trim();
                    String queryStr = null;
                    //this check is needed because "\"bram*\"" doesn't seem to match the "bram" token
                    if (parsedQuery.contains(" ")) {
                        queryStr = "\"" + parsedQuery + "*\"";
                    }
                    else {
                        queryStr = parsedQuery + "*";
                    }

                    subQuery = complexPhraseParser.parse(queryStr);
                    break;
                default:
                    throw new ParseException("Encountered unsupported query type; this shouldn't happen; " + q.getType());
            }

            activeQuery.add(subQuery, q.getBool());
        }

        return query;
    }
    //exactly the same code as QueryParserBase.escape(), but with the sb.append('\\'); line commented and added an else-part
    private String removeEscapedChars(String s)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
                || c == '*' || c == '?' || c == '|' || c == '&' || c == '/') {
                //sb.append('\\');
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    private void printLuceneIndex() throws IOException
    {
        Directory dir = FSDirectory.open(Settings.instance().getPageMainIndexFolder());

        try (IndexReader reader = DirectoryReader.open(dir)) {
            int numDocs = reader.numDocs();
            for (int i = 0; i < numDocs; i++) {
                Document d = reader.document(i);
                System.out.println(i + ") " + d);
            }
        }
    }
    /**
     * Look in our file system and search for the parent of this document (with the same language).
     */
    private URI getParentUri(URI pageUri, FileContext fc) throws IOException
    {
        URI retVal = null;

        URI parentUri = StringFunctions.getParent(pageUri);
        while (retVal == null) {
            if (parentUri == null) {
                break;
            }
            else {
                //note: this is null proof
                Page parentPage = new ReadOnlyPage(pageUri);
                if (fc.util().exists(parentPage.getResourcePath().getLocalPath())) {
                    retVal = parentUri;
                }
                else {
                    parentUri = StringFunctions.getParent(parentUri);
                }

            }
        }

        return retVal;
    }
    private IndexReader getLuceneIndexReader() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_READER)) {
            //make sure the basic structure to read stuff exists
            this.assertBasicStructure();
            //TODO we just set the value to a dummy true value because it didn't work so well...
            R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_READER, true);
        }

        return DirectoryReader.open(FSDirectory.open(Settings.instance().getPageMainIndexFolder()));
        //return (IndexReader) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_READER);
    }
    private IndexSearcher getLuceneIndexSearcher() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_SEARCHER)) {
            //make sure the basic structure to read stuff exists
            this.assertBasicStructure();
            //TODO we just set the value to a dummy true value because it didn't work so well...
            R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_SEARCHER, true);
        }

        return new IndexSearcher(getLuceneIndexReader());
        //return (IndexSearcher) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_SEARCHER);
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
    private IndexWriter getNewLuceneIndexWriter() throws IOException
    {
        final java.nio.file.Path docDir = Settings.instance().getPageMainIndexFolder();
        if (!Files.exists(docDir)) {
            Files.createDirectories(docDir);
        }
        if (!Files.isWritable(docDir)) {
            throw new IOException("Lucene index directory is not writable, please check the path; " + docDir);
        }

        IndexWriterConfig iwc = new IndexWriterConfig(DEFAULT_ANALYZER);

        // Add new documents to an existing index:
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        return new IndexWriter(FSDirectory.open(Settings.instance().getPageMainIndexFolder()), iwc);
    }
    private void assertWriterTransaction() throws IOException
    {
        this.assertWriter();

        //attach this connection to the transaction manager
        StorageFactory.getCurrentRequestTx().registerResource(this);
    }
    private void assertWriter() throws IOException
    {
        if (this.indexWriter == null) {
            this.indexWriter = this.getNewLuceneIndexWriter();
        }
    }
    /**
     * Watch out: don't call this method often, it's hideously slow!
     */
    private void assertBasicStructure() throws IOException
    {
        try (IndexWriter indexWriter = this.getNewLuceneIndexWriter()) {
            //just open and close the writer once, else we'll get a "no segments* file found" exception
        }
    }
}

package com.beligum.blocks.fs.index;

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.entries.AbstractIndexEntry;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.entries.SimplePageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.fs.pages.DefaultPageImpl;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import org.apache.hadoop.fs.FileContext;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
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
public class LucenePageIndexerConnection extends AbstractIndexConnection implements PageIndexConnection
{
    //-----CONSTANTS-----
    private static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer();

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
        TermQuery query = new TermQuery(AbstractIndexEntry.toLuceneId(key));
        TopDocs topdocs = getLuceneIndexSearcher().search(query, 1);

        if (topdocs.totalHits == 0) {
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

        this.indexWriter.deleteDocuments(AbstractIndexEntry.toLuceneId(page.buildAddress()));

        //for debug
        //this.printLuceneIndex();
    }
    @Override
    public void update(Page page) throws IOException
    {
        this.assertWriterTransaction();

        SimplePageIndexEntry indexExtry = this.createEntry(page);

        //let's not mix-and-mingle writes (even though the IndexWriter is thread-safe),
        // so we can do a clean commit/rollback on our own
        this.indexWriter.updateDocument(AbstractIndexEntry.toLuceneId(indexExtry), SimplePageIndexEntry.toLuceneDoc(indexExtry));

        //for debug
        //this.printLuceneIndex();
    }
    @Override
    public List<PageIndexEntry> search(FieldQuery[] fieldQueries, int maxResults) throws IOException
    {
        List<PageIndexEntry> retVal = new ArrayList<>();

        try {
            BooleanQuery query = new BooleanQuery();
            Map<Integer, BooleanQuery> groups = new HashMap<>();
            for (FieldQuery q : fieldQueries) {

                BooleanQuery activeQuery = query;
                if (q.getGroup()!=null) {
                    if (groups.containsKey(q.getGroup())) {
                        activeQuery = groups.get(q.getGroup());
                    }
                    else {
                        activeQuery = new BooleanQuery();
                        groups.put(q.getGroup(), activeQuery);
                        //TODO hmm, this (FILTER) won't always be true...
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
                        QueryParser parser = new QueryParser(q.getField().name(), DEFAULT_ANALYZER);
                        //we need to escape the wildcard query, and append the asterisk afterwards (or it will be escaped)
                        subQuery = parser.parse(QueryParser.escape(q.getQuery()) + "*");
                        break;
                    default:
                        throw new ParseException("Encountered unsupported query type; this shouldn't happen; "+q.getType());
                }

                activeQuery.add(subQuery, q.getBool());
            }

            TopDocs topdocs = getLuceneIndexSearcher().search(query, maxResults);

            //TODO this is probably not so efficient...
            for (int i = 0; i < topdocs.totalHits; i++) {
                retVal.add(SimplePageIndexEntry.fromLuceneDoc(getLuceneIndexReader().document(topdocs.scoreDocs[i].doc)));
            }
        }
        catch (ParseException e) {
            throw new IOException("Error while parsing multi-field lucene query; " + fieldQueries, e);
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
    private void printLuceneIndex() throws IOException
    {
        final java.nio.file.Path docDir = Settings.instance().getPageMainIndexFolder().toPath();
        Directory dir = FSDirectory.open(docDir);

        try (IndexReader reader = DirectoryReader.open(dir)) {
            int numDocs = reader.numDocs();
            for (int i = 0; i < numDocs; i++) {
                Document d = reader.document(i);
                System.out.println(i + ") " + d);
            }
        }
    }
    private SimplePageIndexEntry createEntry(Page page) throws IOException
    {
        HtmlAnalyzer htmlAnalyzer = page.createAnalyzer();

        FileContext fc = page.getResourcePath().getFileContext();
        URI pageAddress = page.buildAddress();

        SimplePageIndexEntry entry = new SimplePageIndexEntry(pageAddress);
        entry.setResource(htmlAnalyzer.getHtmlAbout() == null ? null : htmlAnalyzer.getHtmlAbout().value);
        entry.setTypeOf(htmlAnalyzer.getHtmlTypeof() == null ? null : htmlAnalyzer.getHtmlTypeof().value);
        entry.setLanguage(htmlAnalyzer.getHtmlLanguage() == null ? null : htmlAnalyzer.getHtmlLanguage().getLanguage());
        URI parent = this.getParentUri(pageAddress, fc);
        entry.setParent(parent == null ? null : parent.toString());
        entry.setTitle(htmlAnalyzer.getTitle());

        return entry;
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
                URI parentResourceUri = DefaultPageImpl.toResourceUri(parentUri, Settings.instance().getPagesStorePath());
                if (parentResourceUri != null && fc.util().exists(new org.apache.hadoop.fs.Path(parentResourceUri))) {
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
        //if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_READER)) {
        //make sure the basic structure to read stuff exists
        try (IndexWriter writer = getNewLuceneIndexWriter()) {
        }

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Settings.instance().getPageMainIndexFolder().toPath()));
        return reader;
        //R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_READER, reader);
        //}

        //return (IndexReader) R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_READER);
    }
    private IndexSearcher getLuceneIndexSearcher() throws IOException
    {
        //if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_SEARCHER)) {
        IndexSearcher searcher = new IndexSearcher(getLuceneIndexReader());
        return searcher;
        //R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_SEARCHER, searcher);
        //}

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
        final java.nio.file.Path docDir = Settings.instance().getPageMainIndexFolder().toPath();
        if (!Files.exists(docDir)) {
            Files.createDirectories(docDir);
        }
        if (!Files.isWritable(docDir)) {
            throw new IOException("Lucene index directory is not writable, please check the path; " + docDir);
        }

        IndexWriterConfig iwc = new IndexWriterConfig(DEFAULT_ANALYZER);

        // Add new documents to an existing index:
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        return new IndexWriter(FSDirectory.open(Settings.instance().getPageMainIndexFolder().toPath()), iwc);
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
}

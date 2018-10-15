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

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexConnection;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FSLockFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Started with this:
 * http://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/IndexFiles.html
 * and
 * http://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/SearchFiles.html
 * <p/>
 * interesting read:
 * http://stackoverflow.com/questions/9377572/is-it-good-practice-to-keep-a-lucene-indexwriter-indexsearcher-open-for-the-li
 * <p/>
 * and for Hibernate:
 * org.hibernate.search.spi.SearchFactoryBuilder.initDocumentBuilders()
 * <p/>
 * Created by bram on 1/26/16.
 */
public class LucenePageIndexer implements PageIndexer
{
    //-----CONSTANTS-----
    public static final String DEFAULT_FIELD_JOINER = " ";

    /**
     * The prefix character we use to insert "meta fields" into the index (sort, all, etc.)
     */
    private static final String CUSTOM_FIELD_PREFIX = "_";

    /**
     * The suffix character we use to turn field names into their verbatim counterpart.
     * Ie. they get indexed as-is, without an analyzer.
     * See notes below for why we need this separated from the analyzed field name.
     * Note that the check below (.contains(...)) assumes this character won't appear anywhere in regular field names.
     */
    private static final String VERBATIM_FIELD_SUFFIX = "~";

    /**
     * The suffix character we use to index human-readable labels of resource URIs.
     * Note that this value is both indexed regularly and verbatim, so it should
     * differ from the verbatim suffix because the two can be (and are) combined.
     * Note that the check below (.contains(...)) assumes this character won't appear anywhere in regular field names.
     */
    private static final String HUMAN_READABLE_FIELD_SUFFIX = "+";

    //mimics the "_all" field of ElasticSearch
    // see https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-all-field.html
    //Note: difference between the two below is that the first one indexes all fields analyzed,
    //      the second field stores the value as is.
    //Note: reason why we need two separate (and can't just index both under '_all') is that if you want to
    //      query an analyzed field in Lucene, and you're using it's analyzed metadata (eg. slop), the constant (un-analyzed)
    //      field will be searched as well, crashing the search session with eg. "field "_all" was indexed without position data"
    public static final String CUSTOM_FIELD_ALL = CUSTOM_FIELD_PREFIX + "all";
    public static final String CUSTOM_FIELD_ALL_VERBATIM = buildVerbatimFieldName(CUSTOM_FIELD_ALL);
    //keeps a list of all fields in this doc, to be able to search for non-existence of a field
    public static final String CUSTOM_FIELD_FIELDS = CUSTOM_FIELD_PREFIX + "fields";

    protected static final Analyzer STANDARD_ANALYZER = new StandardAnalyzer();
    protected static final Analyzer KEYWORD_ANALYZER = new KeywordAnalyzer();
    protected static final Analyzer WHITESPACE_ANALYZER = new WhitespaceAnalyzer();

    /**
     * We switched to using this analyzer instead of the standard one because of better support for french words.
     * Eg. if the user looks for "écope à grain" and the indexed string was "Ecope à grain", the standard analyzer won't find it.
     */
    protected static Analyzer CUSTOM_ANALYZER;
    static {
        try {
            CUSTOM_ANALYZER = CustomAnalyzer.builder()
                                            //difference between these two is that 'standard' also strips punctuation characters like (),' etc.
                                            //.withTokenizer("whitespace")
                                            .withTokenizer("standard")

                                            .addTokenFilter("standard")
                                            //this one is extra, see comment above
                                            .addTokenFilter("asciifolding",
                                                            "preserveOriginal", "false")
                                            .addTokenFilter("lowercase")
                                            .addTokenFilter("stop",
                                                            //note: if no stopwords are set, the default list of english stopwords is used
                                                            "ignoreCase", "false")
                                            .build();
        }
        catch (Exception e) {
            Logger.error("Error while building CUSTOM_ANALYZER, this shouldn't happen; ", e);
        }
    }

    public static final Analyzer ACTIVE_ANALYZER = CUSTOM_ANALYZER;

    //-----VARIABLES-----
    private static final FSLockFactory luceneLockFactory = FSLockFactory.getDefault();
    private java.nio.file.Path indexFolder;
    private Object searcherLock;
    private Object writerLock;

    //-----CONSTRUCTORS-----
    public LucenePageIndexer() throws IOException
    {
        this.searcherLock = new Object();
        this.writerLock = new Object();

        this.reinit();
    }

    //-----PUBLIC METHODS-----
    /**
     * This converts a regular field name into it's "verbatim" counterpart.
     * A verbatim field is a mirror of that field that was indexed un-analyzed
     * and can be searched "exactly as the original"
     */
    public static String buildVerbatimFieldName(String fieldName)
    {
        if (fieldName.contains(VERBATIM_FIELD_SUFFIX)) {
            return fieldName;
        }
        else {
            return fieldName + VERBATIM_FIELD_SUFFIX;
        }
    }
    /**
     * This converts a regular field name into it's "human readable" counterpart.
     * A human readable field is a mirror of a resource-URI field that is (also) indexed as a label.
     * so we can search for "Belgium" instead of "/resource/Country/12345678"
     */
    public static String buildHumanReadableFieldName(String fieldName)
    {
        if (fieldName.contains(HUMAN_READABLE_FIELD_SUFFIX)) {
            return fieldName;
        }
        else {
            return fieldName + HUMAN_READABLE_FIELD_SUFFIX;
        }
    }
    @Override
    public synchronized PageIndexConnection connect(TX tx) throws IOException
    {
        return new LucenePageIndexConnection(this, tx);
    }
    @Override
    public synchronized void reboot() throws IOException
    {
        try {
            this.shutdown();
        }
        finally {
            this.reinit();
        }
    }
    @Override
    public synchronized void shutdown()
    {
        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_SEARCHER)) {
            synchronized (this.searcherLock) {
                if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_SEARCHER)) {
                    IndexSearcher indexSearcher = R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_SEARCHER);
                    try (IndexReader indexReader = indexSearcher.getIndexReader()) {
                        indexReader.close();
                    }
                    catch (Exception e) {
                        Logger.error("Exception caught while closing Lucene reader", e);
                    }
                    finally {
                        R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_SEARCHER);
                    }
                }
            }
        }

        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_WRITER)) {
            synchronized (this.writerLock) {
                if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_WRITER)) {
                    try (IndexWriter indexWriter = R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_WRITER)) {
                        indexWriter.close();
                    }
                    catch (Exception e) {
                        Logger.error("Exception caught while closing Lucene writer", e);
                    }
                    finally {
                        R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_WRITER);
                    }
                }
            }
        }
    }
    public IndexSearcher getIndexSearcher() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_SEARCHER)) {
            synchronized (this.searcherLock) {
                if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_SEARCHER)) {
                    IndexReader indexReader = DirectoryReader.open(FSDirectory.open(this.indexFolder));
                    IndexSearcher indexSearcher = new IndexSearcher(indexReader);

                    R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_SEARCHER, indexSearcher);
                }
            }
        }

        return R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_SEARCHER);
    }
    public IndexWriter getIndexWriter() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_WRITER)) {
            synchronized (this.writerLock) {
                if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.LUCENE_INDEX_WRITER)) {
                    R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_WRITER, buildNewLuceneIndexWriter(this.indexFolder));
                }
            }
        }

        //Note that a Lucene rollback closes the index for concurrency reasons, so double-check
        IndexWriter retVal = R.cacheManager().getApplicationCache().get(CacheKeys.LUCENE_INDEX_WRITER);
        if (retVal == null || !retVal.isOpen()) {
            synchronized (this.writerLock) {
                if (retVal == null || !retVal.isOpen()) {
                    R.cacheManager().getApplicationCache().put(CacheKeys.LUCENE_INDEX_WRITER, retVal = buildNewLuceneIndexWriter(this.indexFolder));
                }
            }
        }

        return retVal;
    }
    public void indexChanged()
    {
        synchronized (this.searcherLock) {
            //will be re-initialized on next read/search
            R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_SEARCHER);
        }
    }

    //-----PUBLIC STATIC METHODS-----
    //exactly the same code as QueryParserBase.escape(), but with the sb.append('\\'); line commented and added an else-part
    public static String replaceEscapedChars(String s, String replacement)
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

    //-----PRIVATE METHODS-----
    private void reinit() throws IOException
    {
        R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_SEARCHER);
        R.cacheManager().getApplicationCache().remove(CacheKeys.LUCENE_INDEX_WRITER);

        this.indexFolder = Paths.get(Settings.instance().getPageMainIndexFolder());

        if (!Files.exists(this.indexFolder)) {
            Files.createDirectories(this.indexFolder);
        }
        if (!Files.isWritable(this.indexFolder)) {
            throw new IOException("Lucene index directory is not writable, please check the permissions of: " + this.indexFolder);
        }

        try (IndexWriter indexWriter = buildNewLuceneIndexWriter(indexFolder)) {
            //just open and close the writer once to instance the necessary files,
            // else we'll get a "no segments* file found" exception on first search
        }
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
    private IndexWriter buildNewLuceneIndexWriter(Path docDir) throws IOException
    {
        IndexWriterConfig iwc = new IndexWriterConfig(LucenePageIndexer.ACTIVE_ANALYZER);

        // Add new documents to an existing index:
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        return new IndexWriter(FSDirectory.open(docDir, luceneLockFactory), iwc);
    }
    private void printLuceneIndex() throws IOException
    {
        Directory dir = FSDirectory.open(this.indexFolder);

        try (IndexReader reader = DirectoryReader.open(dir)) {
            int numDocs = reader.numDocs();
            for (int i = 0; i < numDocs; i++) {
                Document d = reader.document(i);
                System.out.println(i + ") " + d);
            }
        }
    }
}

package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.filesystem.index.LucenePageIndexer;
import com.beligum.blocks.filesystem.index.entries.RdfIndexer;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.utils.RdfTools;
import com.google.common.base.Joiner;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.eclipse.rdf4j.model.Statement;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import static com.beligum.blocks.filesystem.index.LucenePageIndexer.CUSTOM_FIELD_FIELDS;
import static com.beligum.blocks.filesystem.index.LucenePageIndexer.DEFAULT_FIELD_JOINER;
import static org.apache.lucene.util.ByteBlockPool.BYTE_BLOCK_SIZE;

/**
 * Created by bram on 2/13/16.
 */
public class DeepPageIndexEntry extends SimplePageIndexEntry implements RdfIndexer
{
    //-----CONSTANTS-----
    //interesting alternative guide if you ever need it: http://www.citrine.io/blog/2015/2/14/building-a-custom-analyzer-in-lucene
    private static Analyzer SORTFIELD_ANALYZER;

    static {
        try {
            /**
             * This seems to do a pretty good job at normalizing the sort values:
             * - first, split on whitespace (which will later be joined again, see preprocessSortValue() and SORTFIELD_ANALYZER_JOIN_CHAR)
             * - convert all to regular ASCII characters
             * - make all lowercase
             */
            SORTFIELD_ANALYZER = CustomAnalyzer.builder()
                                               //difference between these two is that 'standard' also strips punctuation characters like (),' etc.
                                               //.withTokenizer("whitespace")
                                               .withTokenizer("standard")

                                               .addTokenFilter("asciifolding", "preserveOriginal", "false")
                                               .addTokenFilter("lowercase")
                                               .build();
        }
        catch (Exception e) {
            Logger.error("Error while building sortField analyzer, this shouldn't happen; ", e);
        }
    }

    //-----VARIABLES-----
    private Document luceneDoc;

    //-----CONSTRUCTORS-----
    public DeepPageIndexEntry(Page page) throws IOException
    {
        super(page);

        this.luceneDoc = super.createLuceneDoc();

        //makes sense to eliminate double values for the sort list as well, I think
        Map<RdfProperty, Set<String>> sortFieldMapping = new LinkedHashMap<>();
        //no need to index double values, so let's use a set
        Set<String> allField = new LinkedHashSet<>();

        //Note: we re-use the RDFmodel of the superclass so we don't read it twice
        for (Statement stmt : this.rdfModel) {

            URI predicateCurie = RdfTools.fullToCurie(URI.create(stmt.getPredicate().toString()));
            if (predicateCurie != null) {
                RdfProperty predicate = (RdfProperty) RdfFactory.getClassForResourceType(predicateCurie);
                if (predicate != null) {

                    RdfIndexer.IndexResult value = predicate.indexValue(this, page.getPublicRelativeAddress(), stmt.getObject(), page.getLanguage());

                    String indexValueStr = value.indexValue.toString();
                    //we always index the raw (stringified) value...
                    allField.add(indexValueStr);
                    this.indexConstantField(LucenePageIndexer.CUSTOM_FIELD_ALL, indexValueStr);

                    //also index it so we can search it lowercase, without punctuation, etc...
                    allField.add(value.stringValue);
                    this.indexStringField(LucenePageIndexer.CUSTOM_FIELD_ALL, value.stringValue);

                    Set<String> sortField = sortFieldMapping.get(predicate);
                    if (sortField == null) {
                        sortFieldMapping.put(predicate, sortField = new LinkedHashSet<>());
                    }
                    //makes sense to sort on the human-readable value (eg. 'Belgium' instead of '/resource/Country/1938216')
                    String sortValue = value.stringValue;
                    //this was introduced after experiencing a "java.lang.IllegalArgumentException: DocValuesField "mot:text" is too large, must be <= 32766"
                    //see https://issues.apache.org/jira/browse/LUCENE-4583
                    //Makes sense to crop this to a reasonable value since sorting on thousand-characters-long sort values is't really a valid use case, right?
                    final int MAX_SORT_VALUE_LENGTH = 128;
                    if (sortValue.length() > MAX_SORT_VALUE_LENGTH) {
                        sortValue = sortValue.substring(0, MAX_SORT_VALUE_LENGTH);
                    }
                    sortField.add(sortValue);
                }
            }
        }

        //TODO this is a (shitty) temp workaround to solve Lucene's IllegalArgumentException when trying to index large text chunks.
        //According to http://stackoverflow.com/questions/24019868/utf8-encoding-is-longer-than-the-max-length-32766
        // we should index these fields (especially _all) with a different analyzer...
        Iterator<IndexableField> fieldIter = this.luceneDoc.iterator();
        //found this here: SortedDocValuesWriter.addValue()
        final int MAX_FIELD_SIZE = BYTE_BLOCK_SIZE - 2;
        while (fieldIter.hasNext()) {
            IndexableField f = fieldIter.next();
            String stringVal = f.stringValue();
            //non-string values are probably not too large?
            if (stringVal != null && stringVal.length() > MAX_FIELD_SIZE) {
                Logger.warn("Removing field '" + f.name() + "' from the lucene document we're indexing because it's too large. Value is " + stringVal.length() + " and starts with '" +
                            stringVal.substring(0, 20) + "...' (max " + MAX_FIELD_SIZE + " bytes allowed). Note that this means the content of this value won't be searchable (!)");
                fieldIter.remove();
            }
        }

        //details on DocValues,
        // see https://www.norconex.com/facets-with-lucene/
        //See this discussion for background on multivalues
        // see http://stackoverflow.com/questions/21002042/adding-a-multi-valued-string-field-to-a-lucene-document-do-commas-matter
        for (Map.Entry<RdfProperty, Set<String>> e : sortFieldMapping.entrySet()) {

            RdfProperty predicate = e.getKey();
            String key = predicate.getCurieName().toString();

            //this will allow us to sort on this field
            Set<String> sortField = sortFieldMapping.get(predicate);
            if (sortField != null && !sortField.isEmpty()) {
                String sortValue = preprocessSortValue(Joiner.on(LucenePageIndexer.DEFAULT_FIELD_JOINER).join(sortField));
                this.luceneDoc.add(new SortedDocValuesField(key, new BytesRef(sortValue)));
            }

            //index all field names so we can search for documents that do/don't have a certain field set
            this.luceneDoc.add(new StringField(CUSTOM_FIELD_FIELDS, key, org.apache.lucene.document.Field.Store.NO));
        }
    }

    //-----STATIC METHODS-----
    //see https://mail-archives.apache.org/mod_mbox/lucene-solr-user/201507.mbox/%3CCALvb29w7A6TZVEtiYajDZBPGik8JjQd2tAyo2JTskVrR_JdsuQ@mail.gmail.com%3E
    // and https://examples.javacodegeeks.com/core-java/apache/lucene/lucene-indexing-example-2/
    public static String preprocessSortValue(String value) throws IOException
    {
        String retVal = value;

        if (value != null) {
            StringBuilder sb = new StringBuilder();
            try (TokenStream ts = SORTFIELD_ANALYZER.tokenStream(null, value)) {
                CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
                ts.reset();
                while (ts.incrementToken()) {
                    if (sb.length() > 0) {
                        sb.append(DEFAULT_FIELD_JOINER);
                    }
                    sb.append(term.toString());
                }
                ts.end();
            }

            retVal = sb.toString().trim();
        }

        return retVal;
    }

    //-----PUBLIC METHODS-----
    @Override
    public Document createLuceneDoc() throws IOException
    {
        return this.luceneDoc;
    }
    @Override
    public void indexIntegerField(String fieldName, int value)
    {
        this.luceneDoc.add(new IntField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexLongField(String fieldName, long value)
    {
        this.luceneDoc.add(new LongField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexFloatField(String fieldName, float value)
    {
        this.luceneDoc.add(new FloatField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexDoubleField(String fieldName, double value)
    {
        this.luceneDoc.add(new DoubleField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexStringField(String fieldName, String value)
    {
        this.luceneDoc.add(new TextField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexConstantField(String fieldName, String value)
    {
        this.luceneDoc.add(new StringField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----

    //-----INNER CLASSES-----

}

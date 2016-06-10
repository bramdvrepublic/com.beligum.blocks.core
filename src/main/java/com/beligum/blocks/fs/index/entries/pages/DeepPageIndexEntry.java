package com.beligum.blocks.fs.index.entries.pages;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.fs.index.LucenePageIndexer;
import com.beligum.blocks.fs.index.entries.RdfIndexer;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.utils.RdfTools;
import com.google.common.base.Joiner;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.util.BytesRef;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import static com.beligum.blocks.fs.index.LucenePageIndexer.CUSTOM_FIELD_FIELDS;
import static com.beligum.blocks.fs.index.LucenePageIndexer.DEFAULT_FIELD_JOINER;

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
        Model rdfModel = page.readRdfModel();
        Iterator<Statement> stmtIter = rdfModel.iterator();
        while (stmtIter.hasNext()) {
            Statement stmt = stmtIter.next();

            URI predicateCurie = RdfTools.fullToCurie(URI.create(stmt.getPredicate().toString()));
            if (predicateCurie != null) {
                RdfProperty predicate = (RdfProperty) RdfFactory.getClassForResourceType(predicateCurie);
                if (predicate != null) {

                    RdfIndexer.IndexResult value = predicate.indexValue(this, page.getPublicRelativeAddress(), stmt.getObject(), page.getLanguage());

                    String indexValueStr = value.indexValue.toString();
                    //always index the raw (stringified) value...
                    allField.add(indexValueStr);
                    this.indexConstantField(LucenePageIndexer.CUSTOM_FIELD_ALL, indexValueStr);
                    //...if the human-readable stringValue is different, index that one too
                    if (!indexValueStr.equals(value.stringValue)) {
                        allField.add(value.stringValue);
                        this.indexStringField(LucenePageIndexer.CUSTOM_FIELD_ALL, value.stringValue);
                    }

                    Set<String> sortField = sortFieldMapping.get(predicate);
                    if (sortField == null) {
                        sortFieldMapping.put(predicate, sortField = new LinkedHashSet<>());
                    }
                    //makes sense to sort on the human-readable value (eg. 'Belgium' instead of '/resource/Country/1938216')
                    sortField.add(value.stringValue);
                }
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

        //this will allow us to do a search-all query
//        if (allField != null && !allField.isEmpty()) {
//            String allValue = Joiner.on(LucenePageIndexer.DEFAULT_FIELD_JOINER).join(allField);
//            this.indexStringField(LucenePageIndexer.CUSTOM_FIELD_ALL, allValue);
//        }
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

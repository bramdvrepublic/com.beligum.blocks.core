package com.beligum.blocks.fs.index.entries.pages;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.fs.index.entries.RdfIndexer;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.utils.RdfTools;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bram on 2/13/16.
 */
public class DeepPageIndexEntry extends SimplePageIndexEntry implements RdfIndexer
{
    //-----CONSTANTS-----
    private static final String DEFAULT_FIELD_JOINER = " ";
    private static final String CUSTOM_FIELD_PREFIX = "_";

    //mimics the "_all" field of ElasticSearch
    // see https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-all-field.html
    public static final String CUSTOM_FIELD_ALL = CUSTOM_FIELD_PREFIX+"all";

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

        Map<RdfProperty, String> sortValues = new LinkedHashMap<>();
        Model rdfModel = page.readRdfModel();
        Iterator<Statement> stmtIter = rdfModel.iterator();
        while (stmtIter.hasNext()) {
            Statement stmt = stmtIter.next();

            URI predicateCurie = RdfTools.fullToCurie(URI.create(stmt.getPredicate().toString()));
            if (predicateCurie != null) {
                RdfProperty predicate = (RdfProperty) RdfFactory.getClassForResourceType(predicateCurie);
                if (predicate != null) {
                    Object value = predicate.indexValue(this, page.getPublicRelativeAddress(), stmt.getObject(), page.getLanguage());

                    String valueStr = value.toString();
                    //Lucene: only one value is allowed per field
                    if (sortValues.containsKey(predicate)) {
                        valueStr = sortValues.get(predicate) + DEFAULT_FIELD_JOINER + valueStr;
                    }
                    sortValues.put(predicate, valueStr);
                }
            }
        }

        //details on DocValues,
        // see https://www.norconex.com/facets-with-lucene/
        //See this discussion for background on multivalues
        // see http://stackoverflow.com/questions/21002042/adding-a-multi-valued-string-field-to-a-lucene-document-do-commas-matter
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<RdfProperty, String> e : sortValues.entrySet()) {
            String value = e.getValue();

            //add a docValue for each (possibly joined values of) field so we can sort them
            this.luceneDoc.add(new SortedDocValuesField(e.getKey().getCurieName().toString(), new BytesRef(preprocessSortValue(value))));

            //we'll also concat all (stringified) values together to be able to do an "search all"
            if (sb.length()>0) {
                sb.append(DEFAULT_FIELD_JOINER);
            }
            sb.append(value);
        }

        //index the all field
        this.indexStringField(CUSTOM_FIELD_ALL, sb.toString());
    }

    //-----STATIC METHODS-----
    public static String preprocessSearchValue(String value) throws IOException
    {
        String retVal = value;

        if (value != null) {
            //TODO
        }

        return retVal;
    }
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

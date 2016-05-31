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
import org.openrdf.model.Model;
import org.openrdf.model.Statement;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by bram on 2/13/16.
 */
public class DeepPageIndexEntry extends SimplePageIndexEntry implements RdfIndexer
{
    //-----CONSTANTS-----
    //interesting alternative guide if you ever need it: http://www.citrine.io/blog/2015/2/14/building-a-custom-analyzer-in-lucene
    private static Analyzer SORTFIELD_ANALYZER;
    private static final char SORTFIELD_ANALYZER_JOIN_CHAR = ' ';

    static {
        try {
            /**
             * This seems to do a pretty good job at normalizing the sort values:
             * - first, split on whitespace (which will later be joined again, see preprocessSortValue() and SORTFIELD_ANALYZER_JOIN_CHAR)
             * - convert all to regular ASCII characters
             * - make all lowercase
             */
            SORTFIELD_ANALYZER = CustomAnalyzer.builder()
                                               .withTokenizer("whitespace")
                                               .addTokenFilter("asciifolding", "preserveOriginal", "false")
                                               .addTokenFilter("lowercase")
                                               .build();
        }
        catch (Exception e) {
            Logger.error("Error while building sortField analyzer, this shouldn't happen; ", e);
        }
    }

    //Analogue to org.elasticsearch.index.mapper.core.BooleanFieldMapper.Values
    //also see http://stackoverflow.com/questions/9661489/which-is-the-best-choice-to-indexing-a-boolean-value-in-lucene
    private static final String BOOLEAN_TRUE_STRING = "T";
    private static final String BOOLEAN_FALSE_STRING = "F";

    //-----VARIABLES-----
    private Document luceneDoc;

    //-----CONSTRUCTORS-----
    public DeepPageIndexEntry(Page page) throws IOException
    {
        super(page);

        this.luceneDoc = super.createLuceneDoc();

        Map<RdfProperty, List<org.apache.lucene.document.Field>> valuesToIndex = new LinkedHashMap<>();
        Model rdfModel = page.readRdfModel();
        Iterator<Statement> stmtIter = rdfModel.iterator();
        while (stmtIter.hasNext()) {
            Statement stmt = stmtIter.next();

            URI predicateCurie = RdfTools.fullToCurie(URI.create(stmt.getPredicate().toString()));
            if (predicateCurie != null) {
                RdfProperty predicate = (RdfProperty) RdfFactory.getClassForResourceType(predicateCurie);
                if (predicate != null) {
                    predicate.indexValue(this, page.getPublicRelativeAddress(), stmt.getObject(), page.getLanguage());
                }
            }
        }

//        //details on DocValues,
//        // see https://www.norconex.com/facets-with-lucene/
//        //See this discussion for background on multivalues
//        // see http://stackoverflow.com/questions/21002042/adding-a-multi-valued-string-field-to-a-lucene-document-do-commas-matter
//        for (Map.Entry<RdfProperty, List<org.apache.lucene.document.Field>> e : valuesToIndex.entrySet()) {
//            //index all values for this indexEntry
//            for (org.apache.lucene.document.Field field : e.getValue()) {
//                this.luceneDoc.add(field);
//            }
//
////            //and once more with a small pre-processing step for sorting
////            //Lucene: only one value is allowed per field
////            this.luceneDoc.add(new SortedDocValuesField(e.getKey(), new BytesRef(preprocessSortValue(e.getValue()))));
//        }
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
            try (TokenStream ts = SORTFIELD_ANALYZER.tokenStream(null, value.toString())) {
                CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
                ts.reset();
                while (ts.incrementToken()) {
                    if (sb.length() > 0) {
                        sb.append(SORTFIELD_ANALYZER_JOIN_CHAR);
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
    public void indexBooleanField(String fieldName, boolean value)
    {
        this.luceneDoc.add(new StringField(fieldName, value ? BOOLEAN_TRUE_STRING : BOOLEAN_FALSE_STRING, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexCalendarField(String fieldName, Calendar value)
    {
        this.luceneDoc.add(new LongField(fieldName, value.getTimeInMillis(), org.apache.lucene.document.Field.Store.NO));
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

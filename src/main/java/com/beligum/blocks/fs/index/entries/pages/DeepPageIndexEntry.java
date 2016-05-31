package com.beligum.blocks.fs.index.entries.pages;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontology.vocabularies.XSD;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.openrdf.model.*;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by bram on 2/13/16.
 */
public class DeepPageIndexEntry extends SimplePageIndexEntry
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

            URI predicateCurie = RdfFactory.fullToCurie(URI.create(stmt.getPredicate().toString()));
            if (predicateCurie != null) {
                RdfProperty predicate = (RdfProperty) RdfFactory.getClassForResourceType(predicateCurie);
                if (predicate != null) {
                    Value obj = stmt.getObject();

                    String fieldName = predicate.getCurieName().toString();
                    org.apache.lucene.document.Field field = null;

                    if (obj instanceof Literal) {
                        Literal objLiteral = (Literal) obj;

                        //Note: for an overview possible values, check com.beligum.blocks.config.InputType
                        if (predicate.getDataType().equals(XSD.BOOLEAN)) {
                            field = createBooleanField(fieldName, objLiteral.booleanValue());
                        }
                        else if (predicate.getDataType().equals(XSD.DATE) || predicate.getDataType().equals(XSD.TIME) || predicate.getDataType().equals(XSD.DATE_TIME)) {
                            field = createCalendarField(fieldName, objLiteral.calendarValue().toGregorianCalendar());
                        }
                        else if (predicate.getDataType().equals(XSD.INT)
                                 || predicate.getDataType().equals(XSD.INTEGER)
                                 || predicate.getDataType().equals(XSD.NEGATIVE_INTEGER)
                                 || predicate.getDataType().equals(XSD.UNSIGNED_INT)
                                 || predicate.getDataType().equals(XSD.NON_NEGATIVE_INTEGER)
                                 || predicate.getDataType().equals(XSD.NON_POSITIVE_INTEGER)
                                 || predicate.getDataType().equals(XSD.POSITIVE_INTEGER)
                                 || predicate.getDataType().equals(XSD.SHORT)
                                 || predicate.getDataType().equals(XSD.UNSIGNED_SHORT)
                                 || predicate.getDataType().equals(XSD.BYTE)
                                 || predicate.getDataType().equals(XSD.UNSIGNED_BYTE))
                        {
                            field = createIntegerField(fieldName, objLiteral.intValue());
                        }
                        else if (predicate.getDataType().equals(XSD.LONG)
                                 || predicate.getDataType().equals(XSD.UNSIGNED_LONG)) {
                            field = createLongField(fieldName, objLiteral.longValue());
                        }
                        else if (predicate.getDataType().equals(XSD.FLOAT)) {
                            field = createFloatField(fieldName, objLiteral.floatValue());
                        }
                        else if (predicate.getDataType().equals(XSD.DOUBLE)) {
                            field = createDoubleField(fieldName, objLiteral.doubleValue());
                        }
                        //this is doubtful, but let's take the largest one
                        // Note we could also try to fit as closely as possible, but that would change the type per value (instead of per 'column'), and that's not a good idea
                        else if (predicate.getDataType().equals(XSD.DECIMAL)) {
                            field = createDoubleField(fieldName, objLiteral.doubleValue());
                        }
                        //TODO maybe we should split this up in constant text and anaylyzed text, depending on more values?
                        else {
                            field = createStringField(fieldName, objLiteral.getLabel());
                        }
                    }
                    else if (obj instanceof IRI) {
                        RdfQueryEndpoint endpoint = predicate.getDataType().getEndpoint();
                        if (endpoint != null) {
                            ResourceInfo resourceValue = endpoint.getResource(predicate, URI.create(obj.stringValue()), page.getLanguage());
                            if (resourceValue != null) {
                                field = createStringField(fieldName, resourceValue.getLabel());
                            }
                        }

                        if (field == null) {
                            Logger.warn("Encountered RDF IRI that won't be indexed because it couldn't be converted to a human-readable format; " + predicate.getCurieName());
                        }
                    }
                    else {
                        Logger.warn("Encountered RDF field of unsupported type " + obj.getClass().getSimpleName() + " that won't be indexed; " + predicate.getCurieName());
                    }

                    //group all values with the same predicate together (we need this later on)
                    if (field != null) {
                        List<org.apache.lucene.document.Field> fields = valuesToIndex.get(field);
                        if (fields == null) {
                            valuesToIndex.put(predicate, fields = new ArrayList<>());
                        }
                        fields.add(field);
                    }
                }
            }
        }

        //details on DocValues,
        // see https://www.norconex.com/facets-with-lucene/
        //See this discussion for background on multivalues
        // see http://stackoverflow.com/questions/21002042/adding-a-multi-valued-string-field-to-a-lucene-document-do-commas-matter
        for (Map.Entry<RdfProperty, List<org.apache.lucene.document.Field>> e : valuesToIndex.entrySet()) {
            //index all values for this indexEntry
            for (org.apache.lucene.document.Field field : e.getValue()) {
                this.luceneDoc.add(field);
            }

//            //and once more with a small pre-processing step for sorting
//            //Lucene: only one value is allowed per field
//            this.luceneDoc.add(new SortedDocValuesField(e.getKey(), new BytesRef(preprocessSortValue(e.getValue()))));
        }
    }

    //-----STATIC METHODS-----
    public static org.apache.lucene.document.Field createBooleanField(String fieldName, boolean value)
    {
        return new StringField(fieldName, value ? BOOLEAN_TRUE_STRING : BOOLEAN_FALSE_STRING, org.apache.lucene.document.Field.Store.NO);
    }
    public static org.apache.lucene.document.Field createCalendarField(String fieldName, Calendar value)
    {
        return new LongField(fieldName, value.getTimeInMillis(), org.apache.lucene.document.Field.Store.NO);
    }
    public org.apache.lucene.document.Field createIntegerField(String fieldName, int value)
    {
        return new IntField(fieldName, value, org.apache.lucene.document.Field.Store.NO);
    }
    public static org.apache.lucene.document.Field createLongField(String fieldName, long value)
    {
        return new LongField(fieldName, value, org.apache.lucene.document.Field.Store.NO);
    }
    public org.apache.lucene.document.Field createFloatField(String fieldName, float value)
    {
        return new FloatField(fieldName, value, org.apache.lucene.document.Field.Store.NO);
    }
    public org.apache.lucene.document.Field createDoubleField(String fieldName, double value)
    {
        return new DoubleField(fieldName, value, org.apache.lucene.document.Field.Store.NO);
    }
    public static org.apache.lucene.document.Field createStringField(String fieldName, String value)
    {
        return new TextField(fieldName, value, org.apache.lucene.document.Field.Store.NO);
    }
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

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----

    //-----INNER CLASSES-----

}

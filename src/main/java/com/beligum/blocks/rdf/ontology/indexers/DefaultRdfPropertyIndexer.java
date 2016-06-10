package com.beligum.blocks.rdf.ontology.indexers;

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.fs.index.entries.RdfIndexer;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontology.vocabularies.RDF;
import com.beligum.blocks.rdf.ontology.vocabularies.XSD;
import com.beligum.blocks.utils.RdfTools;
import org.openrdf.model.IRI;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 5/31/16.
 */
public class DefaultRdfPropertyIndexer implements RdfPropertyIndexer
{
    //-----CONSTANTS-----
    public static final RdfPropertyIndexer INSTANCE = new DefaultRdfPropertyIndexer();

    //Analogue to org.elasticsearch.index.mapper.core.BooleanFieldMapper.Values
    //also see http://stackoverflow.com/questions/9661489/which-is-the-best-choice-to-indexing-a-boolean-value-in-lucene
    private static final String BOOLEAN_TRUE_STRING = "T";
    private static final String BOOLEAN_FALSE_STRING = "F";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    private DefaultRdfPropertyIndexer()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public RdfIndexer.IndexResult index(RdfIndexer indexer, URI subject, RdfProperty property, Value value, Locale language) throws IOException
    {
        RdfIndexer.IndexResult retVal = null;

        String fieldName = property.getCurieName().toString();

        if (value instanceof Literal) {
            Literal objLiteral = (Literal) value;

            //Note: for an overview possible values, check com.beligum.blocks.config.InputType
            if (property.getDataType().equals(XSD.BOOLEAN)) {
                String val = objLiteral.booleanValue() ? BOOLEAN_TRUE_STRING : BOOLEAN_FALSE_STRING;
                indexer.indexConstantField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.DATE) || property.getDataType().equals(XSD.TIME) || property.getDataType().equals(XSD.DATE_TIME)) {
                //the return value is mostly used to sort the field, and to construct the _all field, do it makes sense to return the long instead of the calendar object
                Long val = objLiteral.calendarValue().toGregorianCalendar().getTimeInMillis();
                indexer.indexLongField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.INT)
                     || property.getDataType().equals(XSD.INTEGER)
                     || property.getDataType().equals(XSD.NEGATIVE_INTEGER)
                     || property.getDataType().equals(XSD.UNSIGNED_INT)
                     || property.getDataType().equals(XSD.NON_NEGATIVE_INTEGER)
                     || property.getDataType().equals(XSD.NON_POSITIVE_INTEGER)
                     || property.getDataType().equals(XSD.POSITIVE_INTEGER)
                     || property.getDataType().equals(XSD.SHORT)
                     || property.getDataType().equals(XSD.UNSIGNED_SHORT)
                     || property.getDataType().equals(XSD.BYTE)
                     || property.getDataType().equals(XSD.UNSIGNED_BYTE)) {
                Integer val = objLiteral.intValue();
                indexer.indexIntegerField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.LONG)
                     || property.getDataType().equals(XSD.UNSIGNED_LONG)) {
                Long val = objLiteral.longValue();
                indexer.indexLongField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.FLOAT)) {
                Float val = objLiteral.floatValue();
                indexer.indexFloatField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.DOUBLE)
                     //this is doubtful, but let's take the largest one
                     // Note we could also try to fit as closely as possible, but that would change the type per value (instead of per 'column'), and that's not a good idea
                     || property.getDataType().equals(XSD.DECIMAL)) {
                Double val = objLiteral.doubleValue();
                indexer.indexDoubleField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.STRING)
                     || property.getDataType().equals(XSD.NORMALIZED_STRING)
                     || property.getDataType().equals(RDF.LANGSTRING)) {
                String val = objLiteral.stringValue();
                indexer.indexStringField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(RDF.HTML)) {
                String val = StringFunctions.htmlToPlaintextRFC3676(objLiteral.stringValue());
                indexer.indexStringField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else {
                throw new IOException("Unable to index RDF property " + fieldName + " for value '" + value.stringValue() + "' of '"+subject+"' because the property type is unimplemented; "+property.getDataType());
            }
        }
        else if (value instanceof IRI) {
            //all local URIs should be handled (and indexed) relatively (outside URIs will be left untouched by this method)
            URI uriValue = RdfTools.relativizeToLocalDomain(URI.create(value.stringValue()));

            RdfQueryEndpoint endpoint = property.getDataType().getEndpoint();
            if (endpoint != null) {
                ResourceInfo resourceValue = endpoint.getResource(property, uriValue, language);
                if (resourceValue != null) {
                    String val = resourceValue.getResourceUri().toString();
                    indexer.indexConstantField(fieldName, val);
                    //makes sense to also index the string value (mainly because it's also added to the _all field; see DeepPageIndexEntry*)
                    String valStr = resourceValue.getLabel();
                    indexer.indexStringField(fieldName, valStr);
                    retVal = new RdfIndexer.IndexResult(val, valStr);
                }
                else {
                    throw new IOException("Unable to index RDF property " + fieldName + " for value '" + value.stringValue() + "' of '"+subject+"' because it's resource endpoint returned null");
                }
            }
            else {
                //not all URIs have an endpoint (eg an <img> tag)
                String val = uriValue.toString();
                indexer.indexConstantField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
                //throw new IOException("Unable to index RDF property " + fieldName + " for value '" + value.stringValue() + "' of '"+subject+"' because the property data type has no endpoint configured");
            }
        }
        else {
            throw new IOException("Unable to index RDF property " + fieldName + " for value '" + value.stringValue() + "' of '"+subject+"' because of an unsupported RDF type; " +
                                  value.getClass());
        }

        return retVal;
    }
    @Override
    public Object prepareIndexValue(RdfProperty property, String value, Locale language) throws IOException
    {
        Object retVal = null;

        if (value!=null) {
            if (property.getDataType().equals(XSD.BOOLEAN)) {
                retVal = Boolean.parseBoolean(value) ? BOOLEAN_TRUE_STRING : BOOLEAN_FALSE_STRING;
            }
            else if (property.getDataType().equals(XSD.DATE) || property.getDataType().equals(XSD.TIME) || property.getDataType().equals(XSD.DATE_TIME)) {
                retVal = Long.parseLong(value);
            }
            else if (property.getDataType().equals(XSD.INT)
                     || property.getDataType().equals(XSD.INTEGER)
                     || property.getDataType().equals(XSD.NEGATIVE_INTEGER)
                     || property.getDataType().equals(XSD.UNSIGNED_INT)
                     || property.getDataType().equals(XSD.NON_NEGATIVE_INTEGER)
                     || property.getDataType().equals(XSD.NON_POSITIVE_INTEGER)
                     || property.getDataType().equals(XSD.POSITIVE_INTEGER)
                     || property.getDataType().equals(XSD.SHORT)
                     || property.getDataType().equals(XSD.UNSIGNED_SHORT)
                     || property.getDataType().equals(XSD.BYTE)
                     || property.getDataType().equals(XSD.UNSIGNED_BYTE)) {
                retVal = Integer.parseInt(value);
            }
            else if (property.getDataType().equals(XSD.LONG)
                     || property.getDataType().equals(XSD.UNSIGNED_LONG)) {
                retVal = Long.parseLong(value);
            }
            else if (property.getDataType().equals(XSD.FLOAT)) {
                retVal = Float.parseFloat(value);
            }
            else if (property.getDataType().equals(XSD.DOUBLE)) {
                retVal = Double.parseDouble(value);
            }
            else if (property.getDataType().equals(XSD.DECIMAL)) {
                retVal = Double.parseDouble(value);
            }
            else if (property.getDataType().equals(XSD.STRING)
                     || property.getDataType().equals(XSD.NORMALIZED_STRING)
                     || property.getDataType().equals(RDF.LANGSTRING)) {
                retVal = retVal;
            }
            else if (property.getDataType().equals(RDF.HTML)) {
                retVal = retVal;
            }
            else {
                //TODO... maybe start with a try-parse of an URI and if that succeeds, lookup the Stirng value from the endpoint? Don't forget to sync with the method above though...
                if (true) {
                    throw new IOException("Unimplemented data type; " + property.getDataType());
                }

//                RdfQueryEndpoint endpoint = property.getDataType().getEndpoint();
//                if (endpoint != null) {
//
//                }
//                else {
//                    throw new IOException("Unable to prepare RDF index property " + fieldName + " for value '" + value + "' because the property type is unimplemented; " + property.getDataType());
//                }
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

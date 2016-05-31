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

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    private DefaultRdfPropertyIndexer()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public void index(RdfIndexer indexer, URI subject, RdfProperty property, Value value, Locale language) throws IOException
    {
        String fieldName = property.getCurieName().toString();

        if (value instanceof Literal) {
            Literal objLiteral = (Literal) value;

            //Note: for an overview possible values, check com.beligum.blocks.config.InputType
            if (property.getDataType().equals(XSD.BOOLEAN)) {
                indexer.indexBooleanField(fieldName, objLiteral.booleanValue());
            }
            else if (property.getDataType().equals(XSD.DATE) || property.getDataType().equals(XSD.TIME) || property.getDataType().equals(XSD.DATE_TIME)) {
                indexer.indexCalendarField(fieldName, objLiteral.calendarValue().toGregorianCalendar());
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
                indexer.indexIntegerField(fieldName, objLiteral.intValue());
            }
            else if (property.getDataType().equals(XSD.LONG)
                     || property.getDataType().equals(XSD.UNSIGNED_LONG)) {
                indexer.indexLongField(fieldName, objLiteral.longValue());
            }
            else if (property.getDataType().equals(XSD.FLOAT)) {
                indexer.indexFloatField(fieldName, objLiteral.floatValue());
            }
            else if (property.getDataType().equals(XSD.DOUBLE)) {
                indexer.indexDoubleField(fieldName, objLiteral.doubleValue());
            }
            //this is doubtful, but let's take the largest one
            // Note we could also try to fit as closely as possible, but that would change the type per value (instead of per 'column'), and that's not a good idea
            else if (property.getDataType().equals(XSD.DECIMAL)) {
                indexer.indexDoubleField(fieldName, objLiteral.doubleValue());
            }
            else if (property.getDataType().equals(XSD.STRING)
                     || property.getDataType().equals(XSD.NORMALIZED_STRING)
                     || property.getDataType().equals(RDF.LANGSTRING)) {
                indexer.indexStringField(fieldName, objLiteral.stringValue());
            }
            else if (property.getDataType().equals(RDF.HTML)) {
                indexer.indexStringField(fieldName, StringFunctions.htmlToPlaintextRFC3676(objLiteral.stringValue()));
            }
            else {
                throw new IOException("Unable to index RDF property " + fieldName + " for value '" + value.stringValue() + "' of '"+subject+"' because the property type is unimplemented; "+property.getDataType());
            }
        }
        else if (value instanceof IRI) {
            URI uriValue = URI.create(value.stringValue());

            //all local URIs should be handled (and indexed) relatively
            uriValue = RdfTools.relativizeToLocalDomain(uriValue);

            RdfQueryEndpoint endpoint = property.getDataType().getEndpoint();
            if (endpoint != null) {
                ResourceInfo resourceValue = endpoint.getResource(property, uriValue, language);
                if (resourceValue != null) {
                    indexer.indexStringField(fieldName, resourceValue.getLabel());
                }
                else {
                    throw new IOException("Unable to index RDF property " + fieldName + " for value '" + value.stringValue() + "' of '"+subject+"' because it's resource endpoint returned null");
                }
            }
            else {
                //not all URIs have an endpoint (eg an <img> tag)
                indexer.indexConstantField(fieldName, uriValue.toString());
                //throw new IOException("Unable to index RDF property " + fieldName + " for value '" + value.stringValue() + "' of '"+subject+"' because the property data type has no endpoint configured");
            }
        }
        else {
            throw new IOException("Unable to index RDF property " + fieldName + " for value '" + value.stringValue() + "' of '"+subject+"' because of an unsupported RDF type; " +
                                  value.getClass());
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

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

package com.beligum.blocks.rdf.indexers;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.server.R;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.exceptions.NotIndexedException;
import com.beligum.blocks.filesystem.index.LucenePageIndexer;
import com.beligum.blocks.filesystem.index.ifaces.RdfIndexer;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfPropertyIndexer;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.beligum.blocks.utils.RdfTools;
import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static java.time.ZoneOffset.UTC;

/**
 * Created by bram on 5/31/16.
 */
public class DefaultRdfPropertyIndexer implements RdfPropertyIndexer
{
    //-----CONSTANTS-----
    public static final RdfPropertyIndexer INSTANCE = new DefaultRdfPropertyIndexer();

    //Analogue to org.elasticsearch.index.mapper.core.BooleanFieldMapper.Values
    //also see http://stackoverflow.com/questions/9661489/which-is-the-best-choice-to-indexing-a-boolean-value-in-lucene
    //Update: switched "T" and "F" to "true" and "false" in Stralo v1.0 to sync the indexing behavior with RDF serialization;
    //see https://www.w3.org/TR/turtle/#booleans
    private static final String BOOLEAN_TRUE_STRING = "true";
    private static final String BOOLEAN_FALSE_STRING = "false";

    //This is the maximum length of a string value that will (also) be indexed as a constant value
    private static final int MAX_CONSTANT_STRING_FIELD_SIZE = 1024;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    private DefaultRdfPropertyIndexer()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public RdfIndexer.IndexResult index(RdfIndexer indexer, URI resource, RdfProperty property, Value value, Locale language, RdfQueryEndpoint.SearchOption... options) throws IOException
    {
        RdfIndexer.IndexResult retVal = null;

        String fieldName = property.getCurieName().toString();

        if (value instanceof Literal && !property.getDataType().equals(XSD.anyURI)) {
            Literal objLiteral = (Literal) value;

            //Note: for an overview possible values, check com.beligum.blocks.config.InputType
            if (property.getDataType().equals(XSD.boolean_)) {
                String val = objLiteral.booleanValue() ? BOOLEAN_TRUE_STRING : BOOLEAN_FALSE_STRING;
                indexer.indexConstantField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            //because both date and time are strict dates, we'll use the millis (long) since epoch as the index value
            else if (property.getDataType().equals(XSD.date) || property.getDataType().equals(XSD.dateTime)) {

                //the return value is mostly used to sort the field, and to construct the _all field, do it makes sense to return the long instead of the calendar object
                GregorianCalendar cal = objLiteral.calendarValue().toGregorianCalendar();
                //dates are indexed with UTC timezone, so make sure it's not created with the server's timezone
                cal.setTimeZone(TimeZone.getTimeZone(UTC));

                Long val = cal.getTimeInMillis();
                indexer.indexLongField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            //we don't have a date for time, so we'll use the millis since midnight as the index value
            else if (property.getDataType().equals(XSD.time)) {
                //Note that this will create a date with the day set to 01/01/1970
                GregorianCalendar cal = objLiteral.calendarValue().toGregorianCalendar();
                //dates are indexed with UTC timezone, so make sure it's not created with the server's timezone
                cal.setTimeZone(TimeZone.getTimeZone(UTC));

                //millis since midnight
                Long val = cal.toZonedDateTime().toLocalTime().toNanoOfDay() / 1000000;
                indexer.indexLongField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.int_)
                     || property.getDataType().equals(XSD.integer)
                     || property.getDataType().equals(XSD.negativeInteger)
                     || property.getDataType().equals(XSD.unsignedInt)
                     || property.getDataType().equals(XSD.nonNegativeInteger)
                     || property.getDataType().equals(XSD.nonPositiveInteger)
                     || property.getDataType().equals(XSD.positiveInteger)
                     || property.getDataType().equals(XSD.short_)
                     || property.getDataType().equals(XSD.unsignedShort)
                     || property.getDataType().equals(XSD.byte_)
                     || property.getDataType().equals(XSD.unsignedByte)) {
                int val = objLiteral.intValue();
                indexer.indexIntegerField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.language)) {
                String val = objLiteral.stringValue();
                indexer.indexStringField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.long_)
                     || property.getDataType().equals(XSD.unsignedLong)) {
                long val = objLiteral.longValue();
                indexer.indexLongField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.float_)) {
                float val = objLiteral.floatValue();
                indexer.indexFloatField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.double_)
                     //this is doubtful, but let's take the largest one
                     // Note we could also try to fit as closely as possible, but that would change the type per value (instead of per 'column'), and that's not a good idea
                     || property.getDataType().equals(XSD.decimal)) {
                double val = objLiteral.doubleValue();
                indexer.indexDoubleField(fieldName, val);
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(XSD.string)
                     || property.getDataType().equals(XSD.normalizedString)
                     || property.getDataType().equals(RDF.langString)
                     //this is a little tricky, but in the end it's just a string, right?
                     || property.getDataType().equals(XSD.base64Binary)) {
                String val = objLiteral.stringValue();
                indexer.indexStringField(fieldName, val);
                //if the value is within certain bounds, also index it as a constant,
                //so that we're able to search for it exactly.
                if (val.length() <= MAX_CONSTANT_STRING_FIELD_SIZE) {
                    indexer.indexConstantField(LucenePageIndexer.buildVerbatimFieldName(fieldName), val);
                }
                retVal = new RdfIndexer.IndexResult(val);
            }
            else if (property.getDataType().equals(RDF.HTML)) {
                String val = StringFunctions.htmlToPlaintextRFC3676(objLiteral.stringValue());
                indexer.indexStringField(fieldName, val);
                if (val.length() <= MAX_CONSTANT_STRING_FIELD_SIZE) {
                    indexer.indexConstantField(LucenePageIndexer.buildVerbatimFieldName(fieldName), val);
                }
                retVal = new RdfIndexer.IndexResult(val);
            }
            else {
                throw new IOException("Unable to index RDF property " + fieldName + " (value is '" + value.stringValue() + "') of '" + resource + "' because the property type is unimplemented; " +
                                      property.getDataType());
            }
        }
        else if (value instanceof IRI || property.getDataType().equals(XSD.anyURI)) {
            //all local URIs should be handled (and indexed) relatively (outside URIs will be left untouched by this method)
            URI uriValue = RdfTools.relativizeToLocalDomain(URI.create(value.stringValue()));

            //We'll always index the relative, stringified URI as a value for the field,
            //but if the property has an endpoint, we'll query it to get (and index) the label
            //as well.
            String uriValueStr = uriValue.toString();
            indexer.indexConstantField(fieldName, uriValueStr);
            //for now, we'll set the retVal to the URI and see if we can augment it with a human readable label
            retVal = new RdfIndexer.IndexResult(uriValueStr);

            RdfClass dataType = property.getDataType();
            RdfQueryEndpoint endpoint = dataType.getEndpoint();
            // If we have an endpoint, we'll contact it to get more (human readable) information about the resource
            if (endpoint != null) {
                ResourceInfo resourceValue = endpoint.getResource(dataType, uriValue, language, options);
                if (resourceValue != null) {
                    //this is setRollbackOnly prone, but the logging info is minimal, so we wrap it to have more information
                    try {
                        //makes sense to also index the string value (mainly because it's also added to the _all field; see DeepPageIndexEntry*)
                        String label = resourceValue.getLabel();

                        String humanReadableFieldName = LucenePageIndexer.buildHumanReadableFieldName(fieldName);

                        indexer.indexStringField(humanReadableFieldName, label);
                        //we'll mimic the behavior of String indexing, see above
                        if (label.length() <= MAX_CONSTANT_STRING_FIELD_SIZE) {
                            indexer.indexConstantField(LucenePageIndexer.buildVerbatimFieldName(humanReadableFieldName), label);
                        }
                        retVal = new RdfIndexer.IndexResult(uriValueStr, label);
                    }
                    catch (Exception e) {
                        throw new IOException("Unable to index RDF property " + fieldName + " (value is '" + value.stringValue() + "') of '" + resource +
                                              "' because there was an setRollbackOnly while parsing the information coming back from the resource endpoint for datatype " + property.getDataType() +
                                              ";", e);
                    }
                }
                //we didn't get a resource value from the endpoint and need to crash, but let's add some nice info to the stacktrace
                else {
                    //make sure we have a language or we won't be able to lookup the resource from the uri
                    URI resourceNeedingIndexation = uriValue;
                    Locale uriValueLang = R.i18n().getUrlLocale(resourceNeedingIndexation);
                    if (uriValueLang == null) {
                        //it's a resource, so add it as a query parameter
                        resourceNeedingIndexation = UriBuilder.fromUri(resourceNeedingIndexation).queryParam(I18nFactory.LANG_QUERY_PARAM, language.getLanguage()).build();
                    }

                    throw new NotIndexedException(resource, resourceNeedingIndexation, "Unable to index RDF property " + fieldName + " (value is '" + value.stringValue() + "') of '" + resource +
                                                                                       "' because it's resource endpoint returned null");
                }
            }
        }
        else {
            throw new IOException("Unable to index RDF property " + fieldName + " (value is '" + value.stringValue() + "') of '" + resource + "' because of an unsupported RDF type; " +
                                  value.getClass());
        }

        return retVal;
    }
    @Override
    public Object prepareIndexValue(RdfProperty property, String value, Locale language) throws IOException
    {
        Object retVal = null;

        if (value != null) {
            if (property.getDataType().equals(XSD.boolean_)) {
                retVal = Boolean.parseBoolean(value) ? BOOLEAN_TRUE_STRING : BOOLEAN_FALSE_STRING;
            }
            else if (property.getDataType().equals(XSD.date) || property.getDataType().equals(XSD.time) || property.getDataType().equals(XSD.dateTime)) {
                if (NumberUtils.isNumber(value)) {
                    retVal = Long.parseLong(value);
                }
                else {
                    if (property.getDataType().equals(XSD.date)) {
                        LocalDate localDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(value));
                        retVal = localDate.atStartOfDay(UTC).toInstant().toEpochMilli();
                    }
                    else if (property.getDataType().equals(XSD.dateTime)) {
                        ZonedDateTime zonedDateTime = ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(value));
                        retVal = zonedDateTime.toInstant().toEpochMilli();
                    }
                    else if (property.getDataType().equals(XSD.time)) {
                        LocalTime localTime = LocalTime.from(DateTimeFormatter.ISO_LOCAL_TIME.parse(value));
                        //we return the millis since midnight
                        retVal = localTime.toNanoOfDay() / 1000000;
                    }
                    else {
                        throw new IOException("Unsupported datatype; this shouldn't happen; " + property.getDataType());
                    }
                }
            }
            else if (property.getDataType().equals(XSD.int_)
                     || property.getDataType().equals(XSD.integer)
                     || property.getDataType().equals(XSD.negativeInteger)
                     || property.getDataType().equals(XSD.unsignedInt)
                     || property.getDataType().equals(XSD.nonNegativeInteger)
                     || property.getDataType().equals(XSD.nonPositiveInteger)
                     || property.getDataType().equals(XSD.positiveInteger)
                     || property.getDataType().equals(XSD.short_)
                     || property.getDataType().equals(XSD.unsignedShort)
                     || property.getDataType().equals(XSD.byte_)
                     || property.getDataType().equals(XSD.unsignedByte)) {
                retVal = Integer.parseInt(value);
            }
            else if (property.getDataType().equals(XSD.long_)
                     || property.getDataType().equals(XSD.unsignedLong)) {
                retVal = Long.parseLong(value);
            }
            else if (property.getDataType().equals(XSD.float_)) {
                retVal = Float.parseFloat(value);
            }
            else if (property.getDataType().equals(XSD.double_)) {
                retVal = Double.parseDouble(value);
            }
            else if (property.getDataType().equals(XSD.decimal)) {
                retVal = Double.parseDouble(value);
            }
            else if (property.getDataType().equals(XSD.string)
                     || property.getDataType().equals(XSD.normalizedString)
                     || property.getDataType().equals(RDF.langString)) {
                retVal = value;
            }
            else if (property.getDataType().equals(XSD.language)) {
                retVal = value;
            }
            else if (property.getDataType().equals(RDF.HTML)) {
                retVal = StringFunctions.htmlToPlaintextRFC3676(value);
            }
            //Note: the pure class options if the IRI in the index() counterpart
            else if (property.getDataType().equals(XSD.anyURI) || property.getDataType().getType().equals(RdfClass.Type.CLASS)) {
                //all local URIs should be handled (and indexed) relatively (outside URIs will be left untouched by this method)
                retVal = RdfTools.relativizeToLocalDomain(URI.create(value));
            }
            else {
                throw new IOException("Unimplemented data type; " + property.getDataType());
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

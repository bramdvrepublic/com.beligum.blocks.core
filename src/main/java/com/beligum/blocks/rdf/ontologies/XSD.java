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

package com.beligum.blocks.rdf.ontologies;

import com.beligum.blocks.rdf.ifaces.RdfDatatype;
import com.beligum.blocks.rdf.ifaces.RdfOntology;
import com.beligum.blocks.rdf.RdfDatatypeImpl;
import com.beligum.blocks.rdf.RdfOntologyImpl;
import gen.com.beligum.blocks.core.messages.blocks.ontology.Entries;

import java.net.URI;

/**
 * See http://rdf4j.org/doc/4/apidocs/index.html?org/openrdf/model/vocabulary/XMLSchema.html
 *
 * Created by bram on 2/27/16.
 */
public final class XSD extends RdfOntologyImpl
{
    //-----SINGLETON-----
    public static final RdfOntology INSTANCE = new XSD();
    
    private XSD()
    {
        super(URI.create("http://www.w3.org/2001/XMLSchema#"), "xsd");
    }

    //-----ENTRIES-----
    // Interesting read: https://www.w3.org/TR/rdf11-concepts/#xsd-datatypes
    // See "RDF-compatible XSD types"
    /**
     * Absolute or relative URIs and IRIs
     */
    public static final RdfDatatype ANY_URI = new RdfDatatypeImpl("anyURI", INSTANCE, Entries.XSD_title_anyUri, Entries.XSD_label_anyUri);

    /**
     * Base64-encoded binary data
     */
    public static final RdfDatatype BASE64_BINARY = new RdfDatatypeImpl("base64Binary", INSTANCE, Entries.XSD_title_base64Binary, Entries.XSD_label_base64Binary);

    /**
     * Dates (yyyy-mm-dd) with or without timezone
     */
    public static final RdfDatatype DATE = new RdfDatatypeImpl("date", INSTANCE, Entries.XSD_title_date, Entries.XSD_label_date);

    /**
     * Date and time with or without timezone
     */
    public static final RdfDatatype DATE_TIME = new RdfDatatypeImpl("dateTime", INSTANCE, Entries.XSD_title_dateTime, Entries.XSD_label_dateTime);

    /**
     * Arbitrary-precision decimal numbers
     */
    public static final RdfDatatype DECIMAL = new RdfDatatypeImpl("decimal", INSTANCE, Entries.XSD_title_decimal, Entries.XSD_label_decimal);

    /**
     * Duration of time
     */
    public static final RdfDatatype DURATION = new RdfDatatypeImpl("duration", INSTANCE, Entries.XSD_title_duration, Entries.XSD_label_duration);

    /**
     * Gregorian calendar day of the month
     */
    public static final RdfDatatype G_DAY = new RdfDatatypeImpl("gDay", INSTANCE, Entries.XSD_title_gDay, Entries.XSD_label_gDay);

    /**
     * Gregorian calendar month
     */
    public static final RdfDatatype G_MONTH = new RdfDatatypeImpl("gMonth", INSTANCE, Entries.XSD_title_gMonth, Entries.XSD_label_gMonth);

    /**
     * Gregorian calendar month and day
     */
    public static final RdfDatatype G_MONTH_DAY = new RdfDatatypeImpl("gMonthDay", INSTANCE, Entries.XSD_title_gMonthDay, Entries.XSD_label_gMonthDay);

    /**
     * Gregorian calendar year
     */
    public static final RdfDatatype G_YEAR = new RdfDatatypeImpl("gYear", INSTANCE, Entries.XSD_title_gYear, Entries.XSD_label_gYear);

    /**
     * Gregorian calendar year and month
     */
    public static final RdfDatatype G_YEAR_MONTH = new RdfDatatypeImpl("gYearMonth", INSTANCE, Entries.XSD_title_gYearMonth, Entries.XSD_label_gYearMonth);

    /**
     * Hex-encoded binary data
     */
    public static final RdfDatatype HEX_BINARY = new RdfDatatypeImpl("hexBinary", INSTANCE, Entries.XSD_title_hexBinary, Entries.XSD_label_hexBinary);

    /**
     * Arbitrary-size integer numbers
     */
    public static final RdfDatatype INTEGER = new RdfDatatypeImpl("integer", INSTANCE, Entries.XSD_title_integer, Entries.XSD_label_integer);

    /**
     * Language tags per [BCP47]
     */
    public static final RdfDatatype LANGUAGE = new RdfDatatypeImpl("language", INSTANCE, Entries.XSD_title_language, Entries.XSD_label_language);

    /**
     * XML Names
     */
    public static final RdfDatatype NAME = new RdfDatatypeImpl("Name", INSTANCE, Entries.XSD_title_Name, Entries.XSD_label_Name);

    /**
     * XML NCNames
     */
    public static final RdfDatatype NC_NAME = new RdfDatatypeImpl("NCName", INSTANCE, Entries.XSD_title_NCName, Entries.XSD_label_NCName);

    /**
     * Integer numbers <0
     */
    public static final RdfDatatype NEGATIVE_INTEGER = new RdfDatatypeImpl("negativeInteger", INSTANCE, Entries.XSD_title_negativeInteger, Entries.XSD_label_negativeInteger);

    /**
     * XML NMTOKENs
     */
    public static final RdfDatatype NMTOKEN = new RdfDatatypeImpl("NMTOKEN", INSTANCE, Entries.XSD_title_NMTOKEN, Entries.XSD_label_NMTOKEN);

    /**
     * Integer numbers ≥0
     */
    public static final RdfDatatype NON_NEGATIVE_INTEGER = new RdfDatatypeImpl("nonNegativeInteger", INSTANCE, Entries.XSD_title_nonNegativeInteger, Entries.XSD_label_nonNegativeInteger);

    /**
     * Integer numbers ≤0
     */
    public static final RdfDatatype NON_POSITIVE_INTEGER = new RdfDatatypeImpl("nonPositiveInteger", INSTANCE, Entries.XSD_title_nonPositiveInteger, Entries.XSD_label_nonPositiveInteger);

    /**
     * Whitespace-normalized strings
     */
    public static final RdfDatatype NORMALIZED_STRING = new RdfDatatypeImpl("normalizedString", INSTANCE, Entries.XSD_title_normalizedString, Entries.XSD_label_normalizedString);

    /**
     * Integer numbers >0
     */
    public static final RdfDatatype POSITIVE_INTEGER = new RdfDatatypeImpl("positiveInteger", INSTANCE, Entries.XSD_title_positiveInteger, Entries.XSD_label_positiveInteger);

    /**
     * Times (hh:mm:ss.sss…) with or without timezone
     */
    public static final RdfDatatype TIME = new RdfDatatypeImpl("time", INSTANCE, Entries.XSD_title_time, Entries.XSD_label_time);

    /**
     * Tokenized strings
     */
    public static final RdfDatatype TOKEN = new RdfDatatypeImpl("token", INSTANCE, Entries.XSD_title_token, Entries.XSD_label_token);

    /**
     * 0…255 (8 bit)
     */
    public static final RdfDatatype UNSIGNED_BYTE = new RdfDatatypeImpl("unsignedByte", INSTANCE, Entries.XSD_title_unsignedByte, Entries.XSD_label_unsignedByte);

    /**
     * 0…4294967295 (32 bit)
     */
    public static final RdfDatatype UNSIGNED_INT = new RdfDatatypeImpl("unsignedInt", INSTANCE, Entries.XSD_title_unsignedInt, Entries.XSD_label_unsignedInt);

    /**
     * 0…18446744073709551615 (64 bit)
     */
    public static final RdfDatatype UNSIGNED_LONG = new RdfDatatypeImpl("unsignedLong", INSTANCE, Entries.XSD_title_unsignedLong, Entries.XSD_label_unsignedLong);

    /**
     * 0…65535 (16 bit)
     */
    public static final RdfDatatype UNSIGNED_SHORT = new RdfDatatypeImpl("unsignedShort", INSTANCE, Entries.XSD_title_unsignedShort, Entries.XSD_label_unsignedShort);

    /**
     * true, false
     */
    public static final RdfDatatype BOOLEAN = new RdfDatatypeImpl("boolean", INSTANCE, Entries.XSD_title_boolean, Entries.XSD_label_boolean);

    /**
     * -128…+127 (8 bit)
     */
    public static final RdfDatatype BYTE = new RdfDatatypeImpl("byte", INSTANCE, Entries.XSD_title_byte, Entries.XSD_label_byte);

    /**
     * 64-bit floating point numbers incl. ±Inf, ±0, NaN
     */
    public static final RdfDatatype DOUBLE = new RdfDatatypeImpl("double", INSTANCE, Entries.XSD_title_double, Entries.XSD_label_double);

    /**
     * 32-bit floating point numbers incl. ±Inf, ±0, NaN
     */
    public static final RdfDatatype FLOAT = new RdfDatatypeImpl("float", INSTANCE, Entries.XSD_title_float, Entries.XSD_label_float);

    /**
     * -2147483648…+2147483647 (32 bit)
     */
    public static final RdfDatatype INT = new RdfDatatypeImpl("int", INSTANCE, Entries.XSD_title_int, Entries.XSD_label_int);

    /**
     * -9223372036854775808…+9223372036854775807 (64 bit)
     */
    public static final RdfDatatype LONG = new RdfDatatypeImpl("long", INSTANCE, Entries.XSD_title_long, Entries.XSD_label_long);

    /**
     * -32768…+32767 (16 bit)
     */
    public static final RdfDatatype SHORT = new RdfDatatypeImpl("short", INSTANCE, Entries.XSD_title_short, Entries.XSD_label_short);

    /**
     * Character strings (but not all Unicode character strings)
     */
    public static final RdfDatatype STRING = new RdfDatatypeImpl("string", INSTANCE, Entries.XSD_title_string, Entries.XSD_label_string);

    //see https://www.w3.org/TR/rdf11-concepts/#xsd-datatypes
    // "The other built-in XML Schema datatypes are unsuitable for various reasons and should not be used:"
    //    public static final RdfDataType QNAME = new RdfDataTypeImpl("QName", INSTANCE, Entries.XSD_title_QName, Entries.XSD_label_QName);
    //    public static final RdfDataType ENTITY = new RdfDataTypeImpl("ENTITY", INSTANCE, Entries.XSD_title_ENTITY, Entries.XSD_label_ENTITY);
    //    public static final RdfDataType ID = new RdfDataTypeImpl("ID", INSTANCE, Entries.XSD_title_ID, Entries.XSD_label_ID);
    //    public static final RdfDataType IDREF = new RdfDataTypeImpl("IDREF", INSTANCE, Entries.XSD_title_IDREF, Entries.XSD_label_IDREF);
    //    public static final RdfDataType NOTATION = new RdfDataTypeImpl("NOTATION", INSTANCE, Entries.XSD_title_NOTATION, Entries.XSD_label_NOTATION);
    //    public static final RdfDataType IDREFS = new RdfDataTypeImpl("IDREFS", INSTANCE, Entries.XSD_title_IDREFS, Entries.XSD_label_IDREFS);
    //    public static final RdfDataType ENTITIES = new RdfDataTypeImpl("ENTITIES", INSTANCE, Entries.XSD_title_ENTITIES, Entries.XSD_label_ENTITIES);
    //    public static final RdfDataType NMTOKENS = new RdfDataTypeImpl("NMTOKENS", INSTANCE, Entries.XSD_title_NMTOKENS, Entries.XSD_label_NMTOKENS);

}

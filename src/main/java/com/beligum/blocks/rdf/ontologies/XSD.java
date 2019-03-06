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

import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.RdfNamespaceImpl;
import com.beligum.blocks.rdf.ifaces.RdfDatatype;
import com.beligum.blocks.rdf.ifaces.RdfNamespace;
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
    //-----CONSTANTS-----
    public static final RdfNamespace NAMESPACE = new RdfNamespaceImpl("http://www.w3.org/2001/XMLSchema#", "xsd");

    //-----MEMBERS-----
    // Interesting read: https://www.w3.org/TR/rdf11-concepts/#xsd-datatypes
    // See "RDF-compatible XSD types"
    /**
     * Absolute or relative URIs and IRIs
     */
    public static final RdfDatatype anyURI = RdfFactory.newProxyDatatype("anyURI");

    /**
     * Base64-encoded binary data
     */
    public static final RdfDatatype base64Binary = RdfFactory.newProxyDatatype("base64Binary");

    /**
     * Dates (yyyy-mm-dd) with or without timezone
     */
    public static final RdfDatatype date = RdfFactory.newProxyDatatype("date");

    /**
     * Date and time with or without timezone
     */
    public static final RdfDatatype dateTime = RdfFactory.newProxyDatatype("dateTime");

    /**
     * Arbitrary-precision decimal numbers
     */
    public static final RdfDatatype decimal = RdfFactory.newProxyDatatype("decimal");

    /**
     * Duration of time
     */
    public static final RdfDatatype duration = RdfFactory.newProxyDatatype("duration");

    /**
     * Gregorian calendar day of the month
     */
    public static final RdfDatatype gDay = RdfFactory.newProxyDatatype("gDay");

    /**
     * Gregorian calendar month
     */
    public static final RdfDatatype gMonth = RdfFactory.newProxyDatatype("gMonth");

    /**
     * Gregorian calendar month and day
     */
    public static final RdfDatatype gMonthDay = RdfFactory.newProxyDatatype("gMonthDay");

    /**
     * Gregorian calendar year
     */
    public static final RdfDatatype gYear = RdfFactory.newProxyDatatype("gYear");

    /**
     * Gregorian calendar year and month
     */
    public static final RdfDatatype gYearMonth = RdfFactory.newProxyDatatype("gYearMonth");

    /**
     * Hex-encoded binary data
     */
    public static final RdfDatatype hexBinary = RdfFactory.newProxyDatatype("hexBinary");

    /**
     * Arbitrary-size integer numbers
     */
    public static final RdfDatatype integer = RdfFactory.newProxyDatatype("integer");

    /**
     * Language tags per [BCP47]
     */
    public static final RdfDatatype language = RdfFactory.newProxyDatatype("language");

    /**
     * XML Names
     */
    public static final RdfDatatype Name = RdfFactory.newProxyDatatype("Name");

    /**
     * XML NCNames
     */
    public static final RdfDatatype NCName = RdfFactory.newProxyDatatype("NCName");

    /**
     * Integer numbers <0
     */
    public static final RdfDatatype negativeInteger = RdfFactory.newProxyDatatype("negativeInteger");

    /**
     * XML NMTOKENs
     */
    public static final RdfDatatype NMTOKEN = RdfFactory.newProxyDatatype("NMTOKEN");

    /**
     * Integer numbers ≥0
     */
    public static final RdfDatatype nonNegativeInteger = RdfFactory.newProxyDatatype("nonNegativeInteger");

    /**
     * Integer numbers ≤0
     */
    public static final RdfDatatype nonPositiveInteger = RdfFactory.newProxyDatatype("nonPositiveInteger");

    /**
     * Whitespace-normalized strings
     */
    public static final RdfDatatype normalizedString = RdfFactory.newProxyDatatype("normalizedString");

    /**
     * Integer numbers >0
     */
    public static final RdfDatatype positiveInteger = RdfFactory.newProxyDatatype("positiveInteger");

    /**
     * Times (hh:mm:ss.sss…) with or without timezone
     */
    public static final RdfDatatype time = RdfFactory.newProxyDatatype("time");

    /**
     * Tokenized strings
     */
    public static final RdfDatatype token = RdfFactory.newProxyDatatype("token");

    /**
     * 0…255 (8 bit)
     */
    public static final RdfDatatype unsignedByte = RdfFactory.newProxyDatatype("unsignedByte");

    /**
     * 0…4294967295 (32 bit)
     */
    public static final RdfDatatype unsignedInt = RdfFactory.newProxyDatatype("unsignedInt");

    /**
     * 0…18446744073709551615 (64 bit)
     */
    public static final RdfDatatype unsignedLong = RdfFactory.newProxyDatatype("unsignedLong");

    /**
     * 0…65535 (16 bit)
     */
    public static final RdfDatatype unsignedShort = RdfFactory.newProxyDatatype("unsignedShort");

    /**
     * true, false
     */
    public static final RdfDatatype boolean_ = RdfFactory.newProxyDatatype("boolean");

    /**
     * -128…+127 (8 bit)
     */
    public static final RdfDatatype byte_ = RdfFactory.newProxyDatatype("byte");

    /**
     * 64-bit floating point numbers incl. ±Inf, ±0, NaN
     */
    public static final RdfDatatype double_ = RdfFactory.newProxyDatatype("double");

    /**
     * 32-bit floating point numbers incl. ±Inf, ±0, NaN
     */
    public static final RdfDatatype float_ = RdfFactory.newProxyDatatype("float");

    /**
     * -2147483648…+2147483647 (32 bit)
     */
    public static final RdfDatatype int_ = RdfFactory.newProxyDatatype("int");

    /**
     * -9223372036854775808…+9223372036854775807 (64 bit)
     */
    public static final RdfDatatype long_ = RdfFactory.newProxyDatatype("long");

    /**
     * -32768…+32767 (16 bit)
     */
    public static final RdfDatatype short_ = RdfFactory.newProxyDatatype("short");

    /**
     * Character strings (but not all Unicode character strings)
     */
    public static final RdfDatatype string = RdfFactory.newProxyDatatype("string");

    //see https://www.w3.org/TR/rdf11-concepts/#xsd-datatypes
    // "The other built-in XML Schema datatypes are unsuitable for various reasons and should not be used:"
    //    public static final RdfDataType QNAME = RdfFactory.newProxyDatatype("QName");
    //    public static final RdfDataType ENTITY = RdfFactory.newProxyDatatype("ENTITY");
    //    public static final RdfDataType ID = RdfFactory.newProxyDatatype("ID");
    //    public static final RdfDataType IDREF = RdfFactory.newProxyDatatype("IDREF");
    //    public static final RdfDataType NOTATION = RdfFactory.newProxyDatatype("NOTATION");
    //    public static final RdfDataType IDREFS = RdfFactory.newProxyDatatype("IDREFS");
    //    public static final RdfDataType ENTITIES = RdfFactory.newProxyDatatype("ENTITIES");
    //    public static final RdfDataType NMTOKENS = RdfFactory.newProxyDatatype("NMTOKENS");

    //-----CONSTRUCTORS-----
    @Override
    protected void create(RdfFactory rdfFactory) throws RdfInitializationException
    {
        rdfFactory.register(anyURI)
                  .title(Entries.XSD_title_anyUri)
                  .label(Entries.XSD_label_anyUri);

        rdfFactory.register(base64Binary)
                  .title(Entries.XSD_title_base64Binary)
                  .label(Entries.XSD_label_base64Binary);

        rdfFactory.register(date)
                  .title(Entries.XSD_title_date)
                  .label(Entries.XSD_label_date);

        rdfFactory.register(dateTime)
                  .title(Entries.XSD_title_dateTime)
                  .label(Entries.XSD_label_dateTime);

        rdfFactory.register(decimal)
                  .title(Entries.XSD_title_decimal)
                  .label(Entries.XSD_label_decimal);

        rdfFactory.register(duration)
                  .title(Entries.XSD_title_duration)
                  .label(Entries.XSD_label_duration);

        rdfFactory.register(gDay)
                  .title(Entries.XSD_title_gDay)
                  .label(Entries.XSD_label_gDay);

        rdfFactory.register(gMonth)
                  .title(Entries.XSD_title_gMonth)
                  .label(Entries.XSD_label_gMonth);

        rdfFactory.register(gMonthDay)
                  .title(Entries.XSD_title_gMonthDay)
                  .label(Entries.XSD_label_gMonthDay);

        rdfFactory.register(gYear)
                  .title(Entries.XSD_title_gYear)
                  .label(Entries.XSD_label_gYear);

        rdfFactory.register(gYearMonth)
                  .title(Entries.XSD_title_gYearMonth)
                  .label(Entries.XSD_label_gYearMonth);

        rdfFactory.register(hexBinary)
                  .title(Entries.XSD_title_hexBinary)
                  .label(Entries.XSD_label_hexBinary);

        rdfFactory.register(integer)
                  .title(Entries.XSD_title_integer)
                  .label(Entries.XSD_label_integer);

        rdfFactory.register(language)
                  .title(Entries.XSD_title_language)
                  .label(Entries.XSD_label_language);

        rdfFactory.register(Name)
                  .title(Entries.XSD_title_Name)
                  .label(Entries.XSD_label_Name);

        rdfFactory.register(NCName)
                  .title(Entries.XSD_title_NCName)
                  .label(Entries.XSD_label_NCName);

        rdfFactory.register(negativeInteger)
                  .title(Entries.XSD_title_negativeInteger)
                  .label(Entries.XSD_label_negativeInteger);

        rdfFactory.register(NMTOKEN)
                  .title(Entries.XSD_title_NMTOKEN)
                  .label(Entries.XSD_label_NMTOKEN);

        rdfFactory.register(nonNegativeInteger)
                  .title(Entries.XSD_title_nonNegativeInteger)
                  .label(Entries.XSD_label_nonNegativeInteger);

        rdfFactory.register(nonPositiveInteger)
                  .title(Entries.XSD_title_nonPositiveInteger)
                  .label(Entries.XSD_label_nonPositiveInteger);

        rdfFactory.register(normalizedString)
                  .title(Entries.XSD_title_normalizedString)
                  .label(Entries.XSD_label_normalizedString);

        rdfFactory.register(positiveInteger)
                  .title(Entries.XSD_title_positiveInteger)
                  .label(Entries.XSD_label_positiveInteger);

        rdfFactory.register(time)
                  .title(Entries.XSD_title_time)
                  .label(Entries.XSD_label_time);

        rdfFactory.register(token)
                  .title(Entries.XSD_title_token)
                  .label(Entries.XSD_label_token);

        rdfFactory.register(unsignedByte)
                  .title(Entries.XSD_title_unsignedByte)
                  .label(Entries.XSD_label_unsignedByte);

        rdfFactory.register(unsignedInt)
                  .title(Entries.XSD_title_unsignedInt)
                  .label(Entries.XSD_label_unsignedInt);

        rdfFactory.register(unsignedLong)
                  .title(Entries.XSD_title_unsignedLong)
                  .label(Entries.XSD_label_unsignedLong);

        rdfFactory.register(unsignedShort)
                  .title(Entries.XSD_title_unsignedShort)
                  .label(Entries.XSD_label_unsignedShort);

        rdfFactory.register(boolean_)
                  .title(Entries.XSD_title_boolean)
                  .label(Entries.XSD_label_boolean);

        rdfFactory.register(byte_)
                  .title(Entries.XSD_title_byte)
                  .label(Entries.XSD_label_byte);

        rdfFactory.register(double_)
                  .title(Entries.XSD_title_double)
                  .label(Entries.XSD_label_double);

        rdfFactory.register(float_)
                  .title(Entries.XSD_title_float)
                  .label(Entries.XSD_label_float);

        rdfFactory.register(int_)
                  .title(Entries.XSD_title_int)
                  .label(Entries.XSD_label_int);

        rdfFactory.register(long_)
                  .title(Entries.XSD_title_long)
                  .label(Entries.XSD_label_long);

        rdfFactory.register(short_)
                  .title(Entries.XSD_title_short)
                  .label(Entries.XSD_label_short);

        rdfFactory.register(string)
                  .title(Entries.XSD_title_string)
                  .label(Entries.XSD_label_string);
    }

    //-----PUBLIC METHODS-----
    @Override
    public RdfNamespace getNamespace()
    {
        return NAMESPACE;
    }
    @Override
    public boolean isPublic()
    {
        return false;
    }
}

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
import com.beligum.blocks.rdf.RdfOntologyImpl;
import gen.com.beligum.blocks.core.messages.blocks.ontology.Entries;

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
    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_anyURI.html
     *
     * The type xsd:anyURI represents a Uniform Resource Identifier (URI) reference. URIs are used to identify resources, and they may be absolute or relative.
     * Absolute URIs provide the entire context for locating the resources, such as http://datypic.com/prod.html.
     * Relative URIs are specified as the difference from a base URI, such as ../prod.html.
     * It is also possible to specify a fragment identifier, using the # character, such as ../prod.html#shirt.
     *
     * The three previous examples happen to be HTTP URLs (Uniform Resource Locators), but URIs also encompass URLs of other schemes (e.g., FTP, gopher, telnet),
     * as well as URNs (Uniform Resource Names). URIs are not required to be dereferencable; that is,
     * it is not necessary for there to be a web page at http://datypic.com/prod.html in order for this to be a valid URI.
     *
     * URIs require that some characters be escaped with their hexadecimal Unicode code point preceded by the % character.
     * This includes non-ASCII characters and some ASCII characters, namely control characters, spaces, and the following characters (unless they are used as deliimiters in the URI): <>#%{}|\^`.
     * For example, ../édition.html must be represented instead as ../%C3%A9dition.html, with the é escaped as %C3%A9. However, the anyURI type will accept these characters either escaped or unescaped.
     * With the exception of the characters % and #, it will assume that unescaped characters are intended to be escaped when used in an actual URI, although the schema processor will do nothing to alter them.
     * It is valid for an anyURI value to contain a space, but this practice is strongly discouraged. Spaces should instead be escaped using %20.
     *
     * The schema processor is not required to parse the contents of an xsd:anyURI value to determine whether it is valid according to any particular URI scheme.
     * Since the bare minimum rules for valid URI references are fairly generic, the schema processor will accept most character strings, including an empty value.
     * The only values that are not accepted are ones that make inappropriate use of reserved characters,
     * such as ones that contain multiple # characters or have % characters that are not followed by two hexadecimal digits.
     *
     * Note that when relative URI references such as "../prod" are used as values of xsd:anyURI, no attempt is made to determine or keep track of the base URI to which they may be applied.
     * For more information on URIs, see RFC 2396, Uniform Resource Identifiers (URI): Generic Syntax.
     */
    public static final RdfDatatype anyURI = RdfFactory.newProxyDatatype("anyURI");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_base64Binary.html
     *
     * The type xsd:base64Binary represents binary data as a sequence of binary octets. It uses base64 encoding, as described in RFC 2045.The following rules apply to xsd:base64Binary values:
     *   - The following characters are allowed: the letters A to Z (upper and lower case), digits 0 through 9, the plus sign ("+"), the slash ("/"), the equals sign ("=") and XML whitespace characters.
     *   - XML whitespace characters may appear anywhere in the value.
     *   - The number of non-whitespace characters must be divisible by 4.
     *   - Equals signs may only appear at the end of the value, and there may be zero, one or two of them. If there are two equals signs, they must be preceded by one of the following characters: AQgw.
     *     If there is only one equals sign, it must be preceded by one of the following characters: AEIMQUYcgkosw048. In either case, there may be whitespace in between the necessary characters and the equals sign(s).
     */
    public static final RdfDatatype base64Binary = RdfFactory.newProxyDatatype("base64Binary");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_date.html
     *
     * The type xsd:date represents a Gregorian calendar date in the format CCYY-MM-DD where CC represents the century, YY the year, MM the month and DD the day.
     * No left truncation is allowed for any part of the date. To represent years later than 9999, additional digits can be added to the left of the year value,
     * but extra leading zeros are not permitted. To represent years before 0001, a preceding minus sign ("-") is allowed. The year 0000 is not a valid year in the Gregorian calendar.
     *
     * An optional time zone expression may be added at the end. The letter Z is used to indicate Coordinated Universal Time (UTC).
     * All other time zones are represented by their difference from Coordinated Universal Time in the format +hh:mm, or -hh:mm. These values may range from -14:00 to 14:00.
     * For example, US Eastern Standard Time, which is five hours behind UTC, is represented as -05:00. If no time zone value is present, it is considered unknown; it is not assumed to be UTC.
     */
    public static final RdfDatatype date = RdfFactory.newProxyDatatype("date");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_dateTime.html
     *
     * The type xsd:dateTime represents a specific date and time in the format CCYY-MM-DDThh:mm:ss.sss, which is a concatenation of the date and time forms, separated by a literal letter "T".
     * All of the same rules that apply to the date and time types are applicable to xsd:dateTime as well.
     *
     * An optional time zone expression may be added at the end of the value. The letter Z is used to indicate Coordinated Universal Time (UTC).
     * All other time zones are represented by their difference from Coordinated Universal Time in the format +hh:mm, or -hh:mm. These values may range from -14:00 to 14:00.
     * For example, US Eastern Standard Time, which is five hours behind UTC, is represented as -05:00. If no time zone value is present, it is considered unknown; it is not assumed to be UTC.
     */
    public static final RdfDatatype dateTime = RdfFactory.newProxyDatatype("dateTime");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_decimal.html
     *
     * The type xsd:decimal represents a decimal number of arbitrary precision. Schema processors vary in the number of significant digits they support,
     * but a conforming processor must support a minimum of 18 significant digits. The format of xsd:decimal is a sequence of digits optionally preceded by a sign ("+" or "-") and optionally containing a period.
     * The value may start or end with a period. If the fractional part is 0 then the period and trailing zeros may be omitted.
     * Leading and trailing zeros are permitted, but they are not considered significant. That is, the decimal values 3.0 and 3.0000 are considered equal.
     */
    public static final RdfDatatype decimal = RdfFactory.newProxyDatatype("decimal");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_duration.html
     *
     * The type xsd:duration represents a duration of time expressed as a number of years, months, days, hours, minutes, and seconds.
     * The format of xsd:duration is PnYnMnDTnHnMnS, where P is a literal value that starts the expression, nY is the number of years followed by a literal Y,
     * nM is the number of months followed by a literal M, nD is the number of days followed by a literal D, T is a literal value that separates the date and time,
     * nH is the number of hours followed by a literal H, nM is the number of minutes followed by a literal M, and nS is the number of seconds followed by a literal S.
     *
     * The following rules apply to xsd:duration values:
     *   - Any of these numbers and corresponding designators may be absent if they are equal to 0, but at least one number and designator must appear.
     *   - The numbers may be any unsigned integer, with the exception of the number of seconds, which may be an unsigned decimal number.
     *   - If a decimal point appears in the number of seconds, there must be at least one digit after the decimal point.
     *   - A minus sign may appear before the P to specify a negative duration.
     *   - If no time items (hour, minute, second) are present, the letter T must not appear.
     */
    public static final RdfDatatype duration = RdfFactory.newProxyDatatype("duration");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_gDay.html
     *
     * The type xsd:gDay represents a day that recurs every month. The letter g signifies "Gregorian." xsd:gDay can be used to say, for example, that checks are paid on the 5th of each month.
     * To represent a duration of days, use the duration type instead. The format of gDay is ---DD.
     *
     * An optional time zone expression may be added at the end of the value. The letter Z is used to indicate Coordinated Universal Time (UTC).
     * All other time zones are represented by their difference from Coordinated Universal Time in the format +hh:mm, or -hh:mm. These values may range from -14:00 to 14:00.
     * For example, US Eastern Standard Time, which is five hours behind UTC, is represented as -05:00. If no time zone value is present, it is considered unknown; it is not assumed to be UTC.
     */
    public static final RdfDatatype gDay = RdfFactory.newProxyDatatype("gDay");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_gMonth.html
     *
     * The type xsd:gMonth represents a specific month that recurs every year. The letter g signifies "Gregorian." xsd:gMonth can be used to indicate,
     * for example, that fiscal year-end processing occurs in September of every year.
     * To represent a duration of months, use the duration type instead. The format of xsd:gMonth is --MM.
     *
     * An optional time zone expression may be added at the end of the value. The letter Z is used to indicate Coordinated Universal Time (UTC).
     * All other time zones are represented by their difference from Coordinated Universal Time in the format +hh:mm, or -hh:mm. These values may range from -14:00 to 14:00.
     * For example, US Eastern Standard Time, which is five hours behind UTC, is represented as -05:00. If no time zone value is present, it is considered unknown; it is not assumed to be UTC.
     */
    public static final RdfDatatype gMonth = RdfFactory.newProxyDatatype("gMonth");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_gMonthDay.html
     *
     * The type xsd:gMonthDay represents a specific day that recurs every year. The letter g signifies "Gregorian." xsd:gMonthDay can be used to say,
     * for example, that your birthday is on the 14th of April every year. The format of xsd:gMonthDay is --MM-DD.
     *
     * An optional time zone expression may be added at the end of the value. The letter Z is used to indicate Coordinated Universal Time (UTC).
     * All other time zones are represented by their difference from Coordinated Universal Time in the format +hh:mm, or -hh:mm. These values may range from -14:00 to 14:00.
     * For example, US Eastern Standard Time, which is five hours behind UTC, is represented as -05:00. If no time zone value is present, it is considered unknown; it is not assumed to be UTC.
     */
    public static final RdfDatatype gMonthDay = RdfFactory.newProxyDatatype("gMonthDay");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_gYear.html
     *
     * The type xsd:gYear represents a specific calendar year. The letter g signifies "Gregorian." The format of xsd:gYear is CCYY. No left truncation is allowed.
     * To represent years later than 9999, additional digits can be added to the left of the year value. To represent years before 0001, a preceding minus sign ("-") is allowed.
     *
     * An optional time zone expression may be added at the end of the value. The letter Z is used to indicate Coordinated Universal Time (UTC).
     * All other time zones are represented by their difference from Coordinated Universal Time in the format +hh:mm, or -hh:mm. These values may range from -14:00 to 14:00.
     * For example, US Eastern Standard Time, which is five hours behind UTC, is represented as -05:00. If no time zone value is present, it is considered unknown; it is not assumed to be UTC.
     */
    public static final RdfDatatype gYear = RdfFactory.newProxyDatatype("gYear");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_gYearMonth.html
     *
     * The type xsd:gYearMonth represents a specific month of a specific year. The letter g signifies "Gregorian." The format of xsd:gYearMonth is CCYY-MM.
     * No left truncation is allowed on either part. To represents years later than 9999, additional digits can be added to the left of the year value.
     * To represent years before 0001, a preceding minus sign ("-") is permitted.
     *
     * An optional time zone expression may be added at the end of the value. The letter Z is used to indicate Coordinated Universal Time (UTC).
     * All other time zones are represented by their difference from Coordinated Universal Time in the format +hh:mm, or -hh:mm. These values may range from -14:00 to 14:00.
     * For example, US Eastern Standard Time, which is five hours behind UTC, is represented as -05:00. If no time zone value is present, it is considered unknown; it is not assumed to be UTC.
     */
    public static final RdfDatatype gYearMonth = RdfFactory.newProxyDatatype("gYearMonth");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_hexBinary.html
     *
     * The xsd:hexBinary type represents binary data as a sequence of binary octets. It uses hexadecimal encoding, where each binary octet is a two-character hexadecimal number.
     * Lowercase and uppercase letters A through F are permitted. For example, 0FB8 and 0fb8 are two equal xsd:hexBinary representations consisting of two octets.
     */
    public static final RdfDatatype hexBinary = RdfFactory.newProxyDatatype("hexBinary");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_integer.html
     *
     * The type xsd:integer represents an arbitrarily large integer, from which twelve other built-in integer types are derived (directly or indirectly).
     * An xsd:integer is a sequence of digits, optionally preceded by a + or - sign. Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype integer = RdfFactory.newProxyDatatype("integer");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_language.html
     *
     * The type xsd:language represents a natural language identifier, generally used to indicate the language of a document or a part of a document.
     * Before creating a new attribute of type xsd:language, consider using the xml:lang attribute that is intended to indicate the natural language of the element and its content.
     *
     * Values of the xsd:language type conform to RFC 3066, Tags for the Identification of Languages. The three most common formats are:
     *
     *   - For ISO-recognized languages, the format is a two- or three-letter, (usually lowercase) language code that conforms to ISO 639, optionally followed by a hyphen and a two-letter,
     *     (usually uppercase) country code that conforms to ISO 3166. For example, en or en-US.
     *   - For languages registered by the Internet Assigned Numbers Authority (IANA), the format is i-langname, where langname is the registered name. For example, i-navajo.
     *   - For unofficial languages, the format is x-langname, where langname is a name of up to eight characters agreed upon by the two parties sharing the document. For example, x-Newspeak.
     *
     * Any of these three formats may have additional parts, each preceded by a hyphen, which identify additional countries or dialects.
     * Schema processors will not verify that values of the xsd:language type conform to the above rules.
     * They will simply validate based on the pattern specified for this type.
     */
    public static final RdfDatatype language = RdfFactory.newProxyDatatype("language");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_Name.html
     *
     * The type xsd:Name represents an XML name, which can be used as an element-type name or attribute name, among other things.
     * Specifically, this means that values must start with a letter, underscore(_), or colon (:), and may contain only letters, digits, underscores (_), colons (:), hyphens (-), and periods (.).
     * Colons should only be used to separate namespace prefixes from local names.
     */
    public static final RdfDatatype Name = RdfFactory.newProxyDatatype("Name");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_NCName.html
     *
     * The type xsd:NCName represents an XML non-colonized name, which is simply a name that does not contain colons.
     * An xsd:NCName value must start with either a letter or underscore (_) and may contain only letters, digits, underscores (_), hyphens (-), and periods (.).
     * This is equivalent to the Name type, except that colons are not permitted.
     */
    public static final RdfDatatype NCName = RdfFactory.newProxyDatatype("NCName");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_negativeInteger.html
     *
     * The type xsd:negativeInteger represents an arbitrarily large negative integer.
     * An xsd:negativeInteger is a sequence of digits, preceded by a - sign. Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype negativeInteger = RdfFactory.newProxyDatatype("negativeInteger");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_NMTOKEN.html
     *
     * The type xsd:NMTOKEN represents a single string token. xsd:NMTOKEN values may consist of letters, digits, periods (.), hyphens (-), underscores (_), and colons (:).
     * They may start with any of these characters. xsd:NMTOKEN has a whiteSpace facet value of collapse, so any leading or trailing whitespace will be removed.
     * However, no whitespace may appear within the value itself.
     */
    public static final RdfDatatype NMTOKEN = RdfFactory.newProxyDatatype("NMTOKEN");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_nonNegativeInteger.html
     *
     * The type xsd:nonNegativeInteger represents an arbitrarily large non-negative integer.
     * An xsd:nonNegativeInteger is a sequence of digits, optionally preceded by a + sign. Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype nonNegativeInteger = RdfFactory.newProxyDatatype("nonNegativeInteger");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_nonPositiveInteger.html
     *
     * The type xsd:nonPositiveInteger represents an arbitrarily large non-positive integer. An xsd:nonPositiveInteger is a sequence of digits, optionally preceded by a - sign.
     * Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype nonPositiveInteger = RdfFactory.newProxyDatatype("nonPositiveInteger");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_normalizedString.html
     *
     * The type xsd:normalizedString represents a character string that may contain any Unicode character allowed by XML.
     * Certain characters, namely the "less than" symbol (<) and the ampersand (&), must be escaped (using the entities &lt; and &amp;, respectively) when used in strings in XML instances.
     *
     * The xsd:normalizedString type has a whiteSpace facet of replace, which means that the processor replaces each carriage return, line feed, and tab by a single space.
     * This processing is equivalent to the processing of CDATA attribute values in XML 1.0. There is no collapsing of multiple consecutive spaces into a single space.
     */
    public static final RdfDatatype normalizedString = RdfFactory.newProxyDatatype("normalizedString");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_positiveInteger.html
     *
     * The type xsd:positiveInteger represents an arbitrarily large positive integer.
     * An xsd:positiveInteger is a sequence of digits, optionally preceded by a + sign. Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype positiveInteger = RdfFactory.newProxyDatatype("positiveInteger");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_time.html
     *
     * The type xsd:time represents a time of day in the format hh:mm:ss.sss where hh represents the hour, mm the minutes, and ss.sss the seconds.
     * An unlimited number of additional digits can be used to increase the precision of fractional seconds if desired.
     * The time is based on a 24-hour time period, so hours should be represented as 00 through 24. Either of the values 00:00:00 or 24:00:00 can be used to represent midnight.
     *
     * An optional time zone expression may be added at the end of the value. The letter Z is used to indicate Coordinated Universal Time (UTC).
     * All other time zones are represented by their difference from Coordinated Universal Time in the format +hh:mm, or -hh:mm. These values may range from -14:00 to 14:00.
     * For example, US Eastern Standard Time, which is five hours behind UTC, is represented as -05:00. If no time zone value is present, it is considered unknown; it is not assumed to be UTC.
     */
    public static final RdfDatatype time = RdfFactory.newProxyDatatype("time");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_token.html
     *
     * The type xsd:token represents a character string that may contain any Unicode character allowed by XML.
     * Certain characters, namely the "less than" symbol (<) and the ampersand (&), must be escaped (using the entities &lt; and &amp;, respectively) when used in strings in XML instances.
     *
     * The name xsd:token may be slightly confusing because it implies that there may be only one token with no whitespace.
     * In fact, there can be whitespace in a token value. The xsd:token type has a whiteSpace facet of collapse, which means that the processor replaces each carriage return, line feed, and tab by a single space.
     * After this replacement, each group of consecutive spaces is collapsed into one space character, and all leading and trailing spaces are removed.
     * This processing is equivalent to the processing of non-CDATA attribute values in XML 1.0.
     */
    public static final RdfDatatype token = RdfFactory.newProxyDatatype("token");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_unsignedByte.html
     *
     * The type xsd:unsignedByte represents an integer between 0 and 255. An xsd:unsignedByte is a sequence of digits, optionally preceded by a + sign.
     * Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype unsignedByte = RdfFactory.newProxyDatatype("unsignedByte");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_unsignedInt.html
     *
     * The type xsd:unsignedInt represents an integer between 0 and 4294967295.
     * An xsd:unsignedInt is a sequence of digits, optionally preceded by a + sign. Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype unsignedInt = RdfFactory.newProxyDatatype("unsignedInt");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_unsignedLong.html
     *
     * The type xsd:unsignedLong represents an integer between 0 and 18446744073709551615.
     * An xsd:unsignedLong is a sequence of digits, optionally preceded by a + sign. Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype unsignedLong = RdfFactory.newProxyDatatype("unsignedLong");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_unsignedShort.html
     *
     * The type xsd:unsignedShort represents an integer between 0 and 65535. An xsd:unsignedShort is a sequence of digits, optionally preceded by a + sign.
     * Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype unsignedShort = RdfFactory.newProxyDatatype("unsignedShort");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_boolean.html
     *
     * The type xsd:boolean represents logical yes/no values.
     * The valid values for xsd:boolean are true, false, 0, and 1. Values that are capitalized (e.g. TRUE) or abbreviated (e.g. T) are not valid.
     */
    public static final RdfDatatype boolean_ = RdfFactory.newProxyDatatype("boolean");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_byte.html
     *
     * The type xsd:byte represents an integer between -128 and 127. An xsd:byte is a sequence of digits, optionally preceded by a + or - sign.
     * Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype byte_ = RdfFactory.newProxyDatatype("byte");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_double.html
     *
     * The type xsd:double represents an IEEE double-precision 64-bit floating-point number.
     * The format of xsd:double values is a mantissa (a number which conforms to the type decimal) followed, optionally, by the character "E" or "e" followed by an exponent.
     * The exponent must be an integer. For example, 3E2 represents 3 times 10 to the 2nd power, or 300. The exponent must be an integer.
     *
     * In addition, the following values are valid: INF (infinity), -INF (negative infinity), and NaN (Not a Number).
     * INF is considered to be greater than all other values, while -INF is less than all other values.
     * The value NaN cannot be compared to any other values, although it equals itself.
     */
    public static final RdfDatatype double_ = RdfFactory.newProxyDatatype("double");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_float.html
     *
     * The type xsd:float represents an IEEE single-precision 32-bit floating-point number.
     * The format of xsd:float values is a mantissa (a number which conforms to the type decimal) followed, optionally, by the character "E" or "e" followed by an exponent.
     * The exponent must be an integer. For example, 3E2 represents 3 times 10 to the 2nd power, or 300. The exponent must be an integer.
     *
     * In addition, the following values are valid: INF (infinity), -INF (negative infinity), and NaN (Not a Number).
     * INF is considered to be greater than all other values, while -INF is less than all other values. The value NaN cannot be compared to any other values, although it equals itself.
     */
    public static final RdfDatatype float_ = RdfFactory.newProxyDatatype("float");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_int.html
     *
     * The type xsd:int represents an integer between -2147483648 and 2147483647. An xsd:int is a sequence of digits, optionally preceded by a + or - sign.
     * Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype int_ = RdfFactory.newProxyDatatype("int");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_long.html
     *
     * The type xsd:long represents an integer between -9223372036854775808 and 9223372036854775807. An xsd:long is a sequence of digits, optionally preceded by a + or - sign.
     * Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype long_ = RdfFactory.newProxyDatatype("long");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_short.html
     *
     * The type xsd:short represents an integer between -32768 and 32767. An xsd:short is a sequence of digits, optionally preceded by a + or - sign.
     * Leading zeros are permitted, but decimal points are not.
     */
    public static final RdfDatatype short_ = RdfFactory.newProxyDatatype("short");

    /**
     * See http://www.datypic.com/sc/xsd/t-xsd_string.html
     *
     * The type xsd:string represents a character string that may contain any Unicode character allowed by XML.
     * Certain characters, namely the "less than" symbol (<) and the ampersand (&), must be escaped (using the entities &lt; and &amp;, respectively) when used in strings in XML instances.
     *
     * The xsd:string type has a whiteSpace facet of preserve, which means that all whitespace characters (spaces, tabs, carriage returns, and line feeds) are preserved by the processor.
     * This is in contrast to two types derived from it: normalizedString, and token.
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
                  .label(Entries.XSD_label_anyUri);

        rdfFactory.register(base64Binary)
                  .label(Entries.XSD_label_base64Binary);

        rdfFactory.register(date)
                  .label(Entries.XSD_label_date);

        rdfFactory.register(dateTime)
                  .label(Entries.XSD_label_dateTime);

        rdfFactory.register(decimal)
                  .label(Entries.XSD_label_decimal);

        rdfFactory.register(duration)
                  .label(Entries.XSD_label_duration);

        rdfFactory.register(gDay)
                  .label(Entries.XSD_label_gDay);

        rdfFactory.register(gMonth)
                  .label(Entries.XSD_label_gMonth);

        rdfFactory.register(gMonthDay)
                  .label(Entries.XSD_label_gMonthDay);

        rdfFactory.register(gYear)
                  .label(Entries.XSD_label_gYear);

        rdfFactory.register(gYearMonth)
                  .label(Entries.XSD_label_gYearMonth);

        rdfFactory.register(hexBinary)
                  .label(Entries.XSD_label_hexBinary);

        rdfFactory.register(integer)
                  .label(Entries.XSD_label_integer);

        rdfFactory.register(language)
                  .label(Entries.XSD_label_language);

        rdfFactory.register(Name)
                  .label(Entries.XSD_label_Name);

        rdfFactory.register(NCName)
                  .label(Entries.XSD_label_NCName);

        rdfFactory.register(negativeInteger)
                  .label(Entries.XSD_label_negativeInteger);

        rdfFactory.register(NMTOKEN)
                  .label(Entries.XSD_label_NMTOKEN);

        rdfFactory.register(nonNegativeInteger)
                  .label(Entries.XSD_label_nonNegativeInteger);

        rdfFactory.register(nonPositiveInteger)
                  .label(Entries.XSD_label_nonPositiveInteger);

        rdfFactory.register(normalizedString)
                  .label(Entries.XSD_label_normalizedString);

        rdfFactory.register(positiveInteger)
                  .label(Entries.XSD_label_positiveInteger);

        rdfFactory.register(time)
                  .label(Entries.XSD_label_time);

        rdfFactory.register(token)
                  .label(Entries.XSD_label_token);

        rdfFactory.register(unsignedByte)
                  .label(Entries.XSD_label_unsignedByte);

        rdfFactory.register(unsignedInt)
                  .label(Entries.XSD_label_unsignedInt);

        rdfFactory.register(unsignedLong)
                  .label(Entries.XSD_label_unsignedLong);

        rdfFactory.register(unsignedShort)
                  .label(Entries.XSD_label_unsignedShort);

        rdfFactory.register(boolean_)
                  .label(Entries.XSD_label_boolean);

        rdfFactory.register(byte_)
                  .label(Entries.XSD_label_byte);

        rdfFactory.register(double_)
                  .label(Entries.XSD_label_double);

        rdfFactory.register(float_)
                  .label(Entries.XSD_label_float);

        rdfFactory.register(int_)
                  .label(Entries.XSD_label_int);

        rdfFactory.register(long_)
                  .label(Entries.XSD_label_long);

        rdfFactory.register(short_)
                  .label(Entries.XSD_label_short);

        rdfFactory.register(string)
                  .label(Entries.XSD_label_string);
    }

    //-----PUBLIC METHODS-----
    @Override
    public RdfNamespace getNamespace()
    {
        return NAMESPACE;
    }
    @Override
    protected boolean isPublicOntology()
    {
        return false;
    }
}

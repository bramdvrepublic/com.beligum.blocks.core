package com.beligum.blocks.rdf.ontology.vocabs;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ontology.RdfClassImpl;
import gen.com.beligum.blocks.core.messages.blocks.ontology.Entries;

import java.net.URI;

/**
 * Created by bram on 2/27/16.
 */
public final class XSD extends AbstractRdfVocabulary
{
    //-----SINGLETON-----
    public static final XSD INSTANCE = new XSD();
    private XSD()
    {
        super(URI.create("http://www.w3.org/2001/XMLSchema#"), "xsd");
    }

    //-----ENTRIES-----
    public static final RdfClass ANY_URI = new RdfClassImpl("anyURI", INSTANCE, Entries.classTitle_XSD_anyUri, Entries.classTitle_XSD_anyUri, null);
    public static final RdfClass BASE64_BINARY = new RdfClassImpl("base64Binary", INSTANCE, Entries.classTitle_XSD_base64Binary, Entries.classLabel_XSD_base64Binary, null);
    public static final RdfClass DATE = new RdfClassImpl("date", INSTANCE, Entries.classTitle_XSD_date, Entries.classLabel_XSD_date, null);
    public static final RdfClass DATE_TIME = new RdfClassImpl("dateTime", INSTANCE, Entries.classTitle_XSD_dateTime, Entries.classLabel_XSD_dateTime, null);
    public static final RdfClass DECIMAL = new RdfClassImpl("decimal", INSTANCE, Entries.classTitle_XSD_decimal, Entries.classLabel_XSD_decimal, null);
    public static final RdfClass DURATION = new RdfClassImpl("duration", INSTANCE, Entries.classTitle_XSD_duration, Entries.classLabel_XSD_duration, null);
    public static final RdfClass ENTITIES = new RdfClassImpl("ENTITIES", INSTANCE, Entries.classTitle_XSD_ENTITIES, Entries.classLabel_XSD_ENTITIES, null);
    public static final RdfClass ENTITY = new RdfClassImpl("ENTITY", INSTANCE, Entries.classTitle_XSD_ENTITY, Entries.classLabel_XSD_ENTITY, null);
    public static final RdfClass G_DAY = new RdfClassImpl("gDay", INSTANCE, Entries.classTitle_XSD_gDay, Entries.classLabel_XSD_gDay, null);
    public static final RdfClass G_MONTH = new RdfClassImpl("gMonth", INSTANCE, Entries.classTitle_XSD_gMonth, Entries.classLabel_XSD_gMonth, null);
    public static final RdfClass G_MONTH_DAY = new RdfClassImpl("gMonthDay", INSTANCE, Entries.classTitle_XSD_gMonthDay, Entries.classLabel_XSD_gMonthDay, null);
    public static final RdfClass G_YEAR = new RdfClassImpl("gYear", INSTANCE, Entries.classTitle_XSD_gYear, Entries.classLabel_XSD_gYear, null);
    public static final RdfClass G_YEAR_MONTH = new RdfClassImpl("gYearMonth", INSTANCE, Entries.classTitle_XSD_gYearMonth, Entries.classLabel_XSD_gYearMonth, null);
    public static final RdfClass HEX_BINARY = new RdfClassImpl("hexBinary", INSTANCE, Entries.classTitle_XSD_hexBinary, Entries.classLabel_XSD_hexBinary, null);
    public static final RdfClass ID = new RdfClassImpl("ID", INSTANCE, Entries.classTitle_XSD_ID, Entries.classLabel_XSD_ID, null);
    public static final RdfClass IDREF = new RdfClassImpl("IDREF", INSTANCE, Entries.classTitle_XSD_IDREF, Entries.classLabel_XSD_IDREF, null);
    public static final RdfClass IDREFS = new RdfClassImpl("IDREFS", INSTANCE, Entries.classTitle_XSD_IDREFS, Entries.classLabel_XSD_IDREFS, null);
    public static final RdfClass INTEGER = new RdfClassImpl("integer", INSTANCE, Entries.classTitle_XSD_integer, Entries.classLabel_XSD_integer, null);
    public static final RdfClass LANGUAGE = new RdfClassImpl("language", INSTANCE, Entries.classTitle_XSD_language, Entries.classLabel_XSD_language, null);
    public static final RdfClass NAME = new RdfClassImpl("Name", INSTANCE, Entries.classTitle_XSD_Name, Entries.classLabel_XSD_Name, null);
    public static final RdfClass NC_NAME = new RdfClassImpl("NCName", INSTANCE, Entries.classTitle_XSD_NCName, Entries.classLabel_XSD_NCName, null);
    public static final RdfClass NEGATIVE_INTEGER = new RdfClassImpl("negativeInteger", INSTANCE, Entries.classTitle_XSD_negativeInteger, Entries.classLabel_XSD_negativeInteger, null);
    public static final RdfClass NMTOKEN = new RdfClassImpl("NMTOKEN", INSTANCE, Entries.classTitle_XSD_NMTOKEN, Entries.classLabel_XSD_NMTOKEN, null);
    public static final RdfClass NMTOKENS = new RdfClassImpl("NMTOKENS", INSTANCE, Entries.classTitle_XSD_NMTOKENS, Entries.classLabel_XSD_NMTOKENS, null);
    public static final RdfClass NON_NEGATIVE_INTEGER = new RdfClassImpl("nonNegativeInteger", INSTANCE, Entries.classTitle_XSD_nonNegativeInteger, Entries.classLabel_XSD_nonNegativeInteger, null);
    public static final RdfClass NON_POSITIVE_INTEGER = new RdfClassImpl("nonPositiveInteger", INSTANCE, Entries.classTitle_XSD_nonPositiveInteger, Entries.classLabel_XSD_nonPositiveInteger, null);
    public static final RdfClass NORMALIZED_STRING = new RdfClassImpl("normalizedString", INSTANCE, Entries.classTitle_XSD_normalizedString, Entries.classLabel_XSD_normalizedString, null);
    public static final RdfClass NOTATION = new RdfClassImpl("NOTATION", INSTANCE, Entries.classTitle_XSD_NOTATION, Entries.classLabel_XSD_NOTATION, null);
    public static final RdfClass POSITIVE_INTEGER = new RdfClassImpl("positiveInteger", INSTANCE, Entries.classTitle_XSD_positiveInteger, Entries.classLabel_XSD_positiveInteger, null);
    public static final RdfClass QNAME = new RdfClassImpl("QName", INSTANCE, Entries.classTitle_XSD_QName, Entries.classLabel_XSD_QName, null);
    public static final RdfClass TIME = new RdfClassImpl("time", INSTANCE, Entries.classTitle_XSD_time, Entries.classLabel_XSD_time, null);
    public static final RdfClass TOKEN = new RdfClassImpl("token", INSTANCE, Entries.classTitle_XSD_token, Entries.classLabel_XSD_token, null);
    public static final RdfClass UNSIGNED_BYTE = new RdfClassImpl("unsignedByte", INSTANCE, Entries.classTitle_XSD_unsignedByte, Entries.classLabel_XSD_unsignedByte, null);
    public static final RdfClass UNSIGNED_INT = new RdfClassImpl("unsignedInt", INSTANCE, Entries.classTitle_XSD_unsignedInt, Entries.classLabel_XSD_unsignedInt, null);
    public static final RdfClass UNSIGNED_LONG = new RdfClassImpl("unsignedLong", INSTANCE, Entries.classTitle_XSD_unsignedLong, Entries.classLabel_XSD_unsignedLong, null);
    public static final RdfClass UNSIGNED_SHORT = new RdfClassImpl("unsignedShort", INSTANCE, Entries.classTitle_XSD_unsignedShort, Entries.classLabel_XSD_unsignedShort, null);
    public static final RdfClass BOOLEAN = new RdfClassImpl("boolean", INSTANCE, Entries.classTitle_XSD_boolean, Entries.classLabel_XSD_boolean, null);
    public static final RdfClass BYTE = new RdfClassImpl("byte", INSTANCE, Entries.classTitle_XSD_byte, Entries.classLabel_XSD_byte, null);
    public static final RdfClass DOUBLE = new RdfClassImpl("double", INSTANCE, Entries.classTitle_XSD_double, Entries.classLabel_XSD_double, null);
    public static final RdfClass FLOAT = new RdfClassImpl("float", INSTANCE, Entries.classTitle_XSD_float, Entries.classLabel_XSD_float, null);
    public static final RdfClass INT = new RdfClassImpl("int", INSTANCE, Entries.classTitle_XSD_int, Entries.classLabel_XSD_int, null);
    public static final RdfClass LONG = new RdfClassImpl("long", INSTANCE, Entries.classTitle_XSD_long, Entries.classLabel_XSD_long, null);
    public static final RdfClass SHORT = new RdfClassImpl("short", INSTANCE, Entries.classTitle_XSD_short, Entries.classLabel_XSD_short, null);
    public static final RdfClass STRING = new RdfClassImpl("string", INSTANCE, Entries.classTitle_XSD_string, Entries.classLabel_XSD_string, null);
}

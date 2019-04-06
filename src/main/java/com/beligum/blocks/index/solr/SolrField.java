package com.beligum.blocks.index.solr;

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.index.entries.JsonField;
import com.beligum.blocks.index.ifaces.IndexEntry;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.beligum.blocks.utils.RdfTools;
import com.google.common.collect.ImmutableMap;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static java.time.ZoneOffset.UTC;

/**
 * This is more or less the same implementation as org.apache.solr.schema.SchemaField
 * but wrapped around our RDF properties instead.
 */
public class SolrField extends JsonField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //See https://lucene.apache.org/solr/guide/7_7/defining-fields.html
    /**
     * The name of the fieldType for this field. This will be found in the name attribute on the fieldType definition. Every field must have a type.
     */
    private String type = null;

    /**
     * A default value that will be added automatically to any document that does not have a value in this field when it is indexed. If this property is not specified, there is no default.
     */
    private String default_ = null;

    /**
     * If true, the value of the field can be used in queries to retrieve matching documents.
     */
    private Boolean indexed = true;

    /**
     * If true, the actual value of the field can be retrieved by queries.
     */
    private Boolean stored = null;

    /**
     * If true, the value of the field will be put in a column-oriented DocValues structure.
     */
    private Boolean docValues = null;

    /**
     * Control the placement of documents when a sort field is not present.
     */
    private Boolean sortMissingFirst = null;
    private Boolean sortMissingLast = null;

    /**
     * If true, indicates that a single document might contain multiple values for this field type.
     */
    private Boolean multiValued = null;

    /**
     * If true, indicates that an indexed="true" docValues="false" field can be "un-inverted" at query time to build up
     * large in memory data structure to serve in place of DocValues. Defaults to true for historical reasons,
     * but users are strongly encouraged to set this to false for stability and use docValues="true" as needed.
     */
    private Boolean uninvertible = null;

    /**
     * If true, omits the norms associated with this field (this disables length normalization for the field, and saves some memory).
     * Defaults to true for all primitive (non-analyzed) field types, such as int, float, data, bool, and string.
     * Only full-text fields or fields need norms.
     */
    private Boolean omitNorms = null;

    /**
     * If true, omits term frequency, positions, and payloads from postings for this field.
     * This can be a performance boost for fields that don’t require that information.
     * It also reduces the storage space required for the index. Queries that rely on position that are issued
     * on a field with this option will silently fail to find documents.
     * This property defaults to true for all field types that are not text fields.
     */
    private Boolean omitTermFreqAndPositions = null;

    /**
     * Similar to omitTermFreqAndPositions but preserves term frequency information.
     */
    private Boolean omitPositions = null;

    /**
     * These options instruct Solr to maintain full term vectors for each document,
     * optionally including position, offset and payload information for each term occurrence in those vectors.
     * These can be used to accelerate highlighting and other ancillary functionality,
     * but impose a substantial cost in terms of index size.
     * They are not necessary for typical uses of Solr.
     */
    private Boolean termVectors = null;
    private Boolean termPositions = null;
    private Boolean termOffsets = null;
    private Boolean termPayloads = null;

    /**
     * Instructs Solr to reject any attempts to add a document which does not have a value for this field.
     * This property defaults to false.
     */
    private Boolean required = null;

    /**
     * If the field has docValues enabled, setting this to true would allow the field to be returned
     * as if it were a stored field (even if it has stored=false) when matching “*” in an fl parameter.
     */
    private Boolean useDocValuesAsStored = null;

    /**
     * Large fields are always lazy loaded and will only take up space in the document cache if the actual value is < 512KB.
     * This option requires stored="true" and multiValued="false".
     * It’s intended for fields that might have very large values so that they don’t get cached in memory.
     */
    private Boolean large = null;

    //-----CONSTRUCTORS-----
    // For internal use only, see SolrConfigs
    SolrField(String name, String type)
    {
        super(name);

        this.type = type;
    }
    public SolrField(RdfProperty property) throws IOException
    {
        super(property);

        this.type = this.toSolrFieldType(property);
    }

    //-----PUBLIC METHODS-----
    public static boolean isReservedField(String fieldName)
    {
        //see https://lucene.apache.org/solr/guide/7_7/defining-fields.html
        return fieldName.startsWith("_") && fieldName.endsWith("_");
    }
    @Override
    public String getValue(IndexEntry indexEntry)
    {
        //don't really know what to return here, this shouldn't be called anyhow
        return null;
    }
    @Override
    public boolean hasValue(IndexEntry indexEntry)
    {
        //see note above
        return false;
    }
    @Override
    public String serialize(Value rdfValue, RdfProperty predicate, Locale language) throws IOException
    {
        return this.toSolrFieldValue(rdfValue, predicate, language);
    }
    public ImmutableMap<String, Object> toMap()
    {
        ImmutableMap.Builder<String, Object> retVal = new ImmutableMap.Builder<String, Object>()
                        .put("name", this.name)
                        .put("type", this.type);

        if (this.default_ != null) {
            retVal.put("default", this.default_);
        }
        if (this.indexed != null) {
            retVal.put("indexed", this.indexed);
        }
        if (this.stored != null) {
            retVal.put("stored", this.stored);
        }
        if (this.docValues != null) {
            retVal.put("docValues", this.docValues);
        }
        if (this.sortMissingFirst != null) {
            retVal.put("sortMissingFirst", this.sortMissingFirst);
        }
        if (this.sortMissingLast != null) {
            retVal.put("sortMissingLast", this.sortMissingLast);
        }
        if (this.multiValued != null) {
            retVal.put("multiValued", this.multiValued);
        }
        if (this.uninvertible != null) {
            retVal.put("uninvertible", this.uninvertible);
        }
        if (this.omitNorms != null) {
            retVal.put("omitNorms", this.omitNorms);
        }
        if (this.omitTermFreqAndPositions != null) {
            retVal.put("omitTermFreqAndPositions", this.omitTermFreqAndPositions);
        }
        if (this.omitPositions != null) {
            retVal.put("omitPositions", this.omitPositions);
        }
        if (this.termVectors != null) {
            retVal.put("termVectors", this.termVectors);
        }
        if (this.termPositions != null) {
            retVal.put("termPositions", this.termPositions);
        }
        if (this.termOffsets != null) {
            retVal.put("termOffsets", this.termOffsets);
        }
        if (this.termPayloads != null) {
            retVal.put("termPayloads", this.termPayloads);
        }
        if (this.required != null) {
            retVal.put("required", this.required);
        }
        if (this.useDocValuesAsStored != null) {
            retVal.put("useDocValuesAsStored", this.useDocValuesAsStored);
        }
        if (this.large != null) {
            retVal.put("large", this.large);
        }

        return retVal.build();
    }
    public String getType()
    {
        return type;
    }
    public String getDefault()
    {
        return default_;
    }
    public Boolean getIndexed()
    {
        return indexed;
    }
    public Boolean getStored()
    {
        return stored;
    }
    public Boolean getDocValues()
    {
        return docValues;
    }
    public Boolean getSortMissingFirst()
    {
        return sortMissingFirst;
    }
    public Boolean getSortMissingLast()
    {
        return sortMissingLast;
    }
    public Boolean getMultiValued()
    {
        return multiValued;
    }
    public Boolean getUninvertible()
    {
        return uninvertible;
    }
    public Boolean getOmitNorms()
    {
        return omitNorms;
    }
    public Boolean getOmitTermFreqAndPositions()
    {
        return omitTermFreqAndPositions;
    }
    public Boolean getOmitPositions()
    {
        return omitPositions;
    }
    public Boolean getTermVectors()
    {
        return termVectors;
    }
    public Boolean getTermPositions()
    {
        return termPositions;
    }
    public Boolean getTermOffsets()
    {
        return termOffsets;
    }
    public Boolean getTermPayloads()
    {
        return termPayloads;
    }
    public Boolean getRequired()
    {
        return required;
    }
    public Boolean getUseDocValuesAsStored()
    {
        return useDocValuesAsStored;
    }
    public Boolean getLarge()
    {
        return large;
    }

    //-----PROTECTED METHODS-----
    /**
     * Translate the property name to the solr field name.
     * <p>
     * Note that solr says only alphanumeric names are supported
     * eg. the default core solrconfig.xml config has this UpdateProcessor in place, that auto-translates
     * all chars to underscores that are not: a word character (\w means [a-zA-Z_0-9]), a dash, a dot:
     * <updateProcessor class="solr.FieldNameMutatingUpdateProcessorFactory" name="field-name-mutating">
     * <str name="pattern">[^\w-\.]</str>
     * <str name="replacement">_</str>
     * </updateProcessor>
     * However, we tried to use colons in the field names without problems (you only need to escape them in your queries),
     * so we'll be using them until we run into problems.
     * <p>
     * Btw, this is what the docs say:
     * The name of the field. Field names should consist of alphanumeric or underscore characters only and not start with a digit.
     * This is not currently strictly enforced, but other field names will not have first class support from all components and
     * back compatibility is not guaranteed. Names with both leading and trailing underscores (e.g., _version_) are reserved. Every field must have a name.
     */
    @Override
    protected String toFieldName(RdfProperty property)
    {
        return super.toFieldName(property);
    }

    //-----PRIVATE METHODS-----
    /**
     * Convert the datatype of the supplied property to a valid Solr field type.
     * See https://lucene.apache.org/solr/guide/7_1/field-types-included-with-solr.html#field-types-included-with-solr
     */
    private String toSolrFieldType(RdfProperty property) throws IOException
    {
        String retVal = null;

        if (property.getDataType() != null) {

            //Note: for an overview possible values, check com.beligum.blocks.config.InputType
            if (property.getDataType().equals(XSD.boolean_)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_BOOLEANS;
            }
            //because both date and time are strict dates, we'll use the millis (long) since epoch as the index value
            else if (property.getDataType().equals(XSD.date) || property.getDataType().equals(XSD.dateTime)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PDATES;
            }
            //we don't have a date for time, so we'll use the millis (since midnight) as the index value
            else if (property.getDataType().equals(XSD.time)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PLONGS;
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
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PINTS;
            }
            else if (property.getDataType().equals(XSD.language)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_STRINGS;
            }
            else if (property.getDataType().equals(XSD.long_)
                     || property.getDataType().equals(XSD.unsignedLong)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PLONGS;
            }
            else if (property.getDataType().equals(XSD.float_)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PFLOATS;
            }
            else if (property.getDataType().equals(XSD.double_)
                     //this is doubtful, but let's take the largest one
                     // Note we could also try to fit as closely as possible, but that would change the type per value (instead of per 'column'), and that's not a good idea
                     || property.getDataType().equals(XSD.decimal)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PDOUBLES;
            }
            else if (property.getDataType().equals(XSD.string)
                     || property.getDataType().equals(XSD.normalizedString)
                     || property.getDataType().equals(RDF.langString)
                     //this is a little tricky, but in the end it's just a string, right?
                     || property.getDataType().equals(XSD.base64Binary)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_TEXT_SORTABLE;
            }
            else if (property.getDataType().equals(RDF.HTML)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_TEXT_SORTABLE;
            }
            else if (property.getDataType().equals(XSD.anyURI)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_STRINGS;
            }
            else {
                throw new IOException("Encountered RDF property '" + property + "' with unmapped Solr datatype, please fix this; " + property.getDataType());
            }
        }

        return retVal;
    }
    private String toSolrFieldValue(Value value, RdfProperty property, Locale language) throws IOException
    {
        String retVal = null;

        if (value != null) {

            //Note that XSD.anyURI is serialized to RDF as a Literal
            if (value instanceof Literal && !property.getDataType().equals(XSD.anyURI)) {

                Literal literal = (Literal) value;

                //Note: for an overview possible values, check com.beligum.blocks.config.InputType
                if (property.getDataType().equals(XSD.boolean_)) {
                    retVal = Boolean.toString(literal.booleanValue());
                }
                //because both date and time are strict dates, we'll use the millis (long) since epoch as the index value
                else if (property.getDataType().equals(XSD.date) || property.getDataType().equals(XSD.dateTime)) {
                    GregorianCalendar cal = literal.calendarValue().toGregorianCalendar();
                    //dates are indexed with UTC timezone, so make sure it's not created with the server's timezone
                    cal.setTimeZone(TimeZone.getTimeZone(UTC));
                    // See https://lucene.apache.org/solr/guide/7_5/working-with-dates.html
                    // Solr uses DateTimeFormatter.ISO_INSTANT for formatting and parsing
                    retVal = DateTimeFormatter.ISO_INSTANT.format(cal.toZonedDateTime());
                }
                //we don't have a date for time, so we'll use the millis since midnight as the index value
                else if (property.getDataType().equals(XSD.time)) {
                    //Note that this will create a date with the day set to 01/01/1970
                    GregorianCalendar cal = literal.calendarValue().toGregorianCalendar();
                    //dates are indexed with UTC timezone, so make sure it's not created with the server's timezone
                    cal.setTimeZone(TimeZone.getTimeZone(UTC));
                    //millis since midnight
                    retVal = Long.toString(cal.toZonedDateTime().toLocalTime().toNanoOfDay() / 1000000);
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
                    retVal = Integer.toString(literal.intValue());
                }
                else if (property.getDataType().equals(XSD.language)) {
                    retVal = literal.stringValue();
                }
                else if (property.getDataType().equals(XSD.long_)
                         || property.getDataType().equals(XSD.unsignedLong)) {
                    retVal = Long.toString(literal.longValue());
                }
                else if (property.getDataType().equals(XSD.float_)) {
                    retVal = Float.toString(literal.floatValue());
                }
                else if (property.getDataType().equals(XSD.double_)
                         //this is doubtful, but let's take the largest one
                         // Note we could also try to fit as closely as possible, but that would change the type per value (instead of per 'column'), and that's not a good idea
                         || property.getDataType().equals(XSD.decimal)) {
                    retVal = Double.toString(literal.doubleValue());
                }
                else if (property.getDataType().equals(XSD.string)
                         || property.getDataType().equals(XSD.normalizedString)
                         || property.getDataType().equals(RDF.langString)
                         //this is a little tricky, but in the end it's just a string, right?
                         || property.getDataType().equals(XSD.base64Binary)) {
                    retVal = literal.stringValue();
                }
                else if (property.getDataType().equals(RDF.HTML)) {
                    retVal = StringFunctions.htmlToPlaintextRFC3676(literal.stringValue());
                }
                else {
                    //this probably means we added a type to SolrConfigs without implementing it here
                    throw new IOException("Unable to serialize literal value for field '" + this.getName() + "' because the datatype '" + property.getDataType() + "' is unimplemented; " + value);
                }
            }
            else if (value instanceof IRI || property.getDataType().equals(XSD.anyURI)) {

                //all local URIs should be handled (and indexed) relatively (outside URIs will be left untouched by this method)
                URI uriValue = RdfTools.relativizeToLocalDomain(URI.create(value.stringValue()));

                //We'll always index the relative, stringified URI as a value for the field,
                //but if the property has an endpoint, we'll query it to get (and index) the label as well.
                retVal = uriValue.toString();

                //TODO label also needs to be indexed...
//                RdfClass dataType = property.getDataType();
//                RdfQueryEndpoint endpoint = dataType.getEndpoint();
//                // If we have an endpoint, we'll contact it to get more (human readable) information about the resource
//                if (endpoint != null) {
//
//                    //make sure we have a language or we won't be able to lookup the resource from the uri
//                    URI debugValue = uriValue;
//                    Locale uriValueLang = R.i18n().getUrlLocale(debugValue);
//                    if (uriValueLang == null) {
//                        //it's a resource, so add it as a query parameter
//                        debugValue = UriBuilder.fromUri(debugValue).queryParam(I18nFactory.LANG_QUERY_PARAM, language.getLanguage()).build();
//                    }
//
//                    ResourceInfo resourceValue = endpoint.getResource(dataType, uriValue, language);
//                    if (resourceValue != null) {
//                        //this is setRollbackOnly prone, but the logging info is minimal, so we wrap it to have more information
//                        try {
//                            //makes sense to also index the string value (mainly because it's also added to the _all field; see DeepPageIndexEntry*)
//                            String label = resourceValue.getLabel();
//
//                            String humanReadableFieldName = LucenePageIndexer.buildHumanReadableFieldName(fieldName);
//
//                            indexer.indexStringField(humanReadableFieldName, label);
//                            //we'll mimic the behavior of String indexing, see above
//                            if (label.length() <= MAX_CONSTANT_STRING_FIELD_SIZE) {
//                                indexer.indexConstantField(LucenePageIndexer.buildVerbatimFieldName(humanReadableFieldName), label);
//                            }
//                            retVal = new RdfIndexer.IndexResult(uriValueStr, label);
//                        }
//                        catch (Exception e) {
//                            throw new IOException("Unable to serialize resource value for field " + this.getName() + " because there was an setRollbackOnly" +
//                                                  " while parsing the information coming back from the resource endpoint for datatype " + dataType + ";" + debugValue, e);
//                        }
//                    }
//                    //we didn't get a resource value from the endpoint and need to crash, but let's add some nice info to the stacktrace
//                    else {
//                        throw new IOException("Unable to serialize resource value for field " + this.getName() + " because it's resource endpoint returned null; " + debugValue);
//                    }
//                }
            }
            else {
                throw new IOException("Unable to serialize unknown value for field '" + this.getName() + "' because the datatype '" + property.getDataType() + "' is unimplemented; " + value);
            }
        }

        return retVal;
    }

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return name;
    }
}
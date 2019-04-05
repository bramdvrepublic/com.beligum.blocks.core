package com.beligum.blocks.filesystem.index.solr;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.index.entries.JsonField;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.google.common.collect.ImmutableMap;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;

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
    public String serialize(RdfProperty predicate, Value rdfValue) throws IOException
    {
        return this.toSolrFieldValue(predicate, rdfValue);
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
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_BOOLEAN;
            }
            //because both date and time are strict dates, we'll use the millis (long) since epoch as the index value
            else if (property.getDataType().equals(XSD.date) || property.getDataType().equals(XSD.dateTime)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PDATE;
            }
            //we don't have a date for time, so we'll use the millis since midnight as the index value
            else if (property.getDataType().equals(XSD.time)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PLONG;
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
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PINT;
            }
            else if (property.getDataType().equals(XSD.language)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_STRING;
            }
            else if (property.getDataType().equals(XSD.long_)
                     || property.getDataType().equals(XSD.unsignedLong)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PLONG;
            }
            else if (property.getDataType().equals(XSD.float_)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PFLOAT;
            }
            else if (property.getDataType().equals(XSD.double_)
                     //this is doubtful, but let's take the largest one
                     // Note we could also try to fit as closely as possible, but that would change the type per value (instead of per 'column'), and that's not a good idea
                     || property.getDataType().equals(XSD.decimal)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_PDOUBLE;
            }
            else if (property.getDataType().equals(XSD.string)
                     || property.getDataType().equals(XSD.normalizedString)
                     || property.getDataType().equals(RDF.langString)
                     //this is a little tricky, but in the end it's just a string, right?
                     || property.getDataType().equals(XSD.base64Binary)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_STRING;
            }
            else if (property.getDataType().equals(RDF.HTML)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_STRING;
            }
            else if (property.getDataType().equals(XSD.anyURI)) {
                retVal = SolrConfigs.CORE_SCHEMA_TYPE_STRING;
            }
            else {
                throw new IOException("Encountered RDF property '" + property + "' with unsupported datatype; " + property.getDataType());
            }
        }

        return retVal;
    }
    private String toSolrFieldValue(RdfProperty predicate, Value rdfValue) throws IOException
    {
        String retVal = null;

        if (rdfValue != null) {
            if (predicate.getDataType().equals(XSD.boolean_)) {

            }
            else if (predicate.getDataType().equals(XSD.date) || predicate.getDataType().equals(XSD.dateTime)) {

            }
            else if (predicate.getDataType().equals(XSD.time)) {

            }
            else if (predicate.getDataType().equals(XSD.int_)
                     || predicate.getDataType().equals(XSD.integer)
                     || predicate.getDataType().equals(XSD.negativeInteger)
                     || predicate.getDataType().equals(XSD.unsignedInt)
                     || predicate.getDataType().equals(XSD.nonNegativeInteger)
                     || predicate.getDataType().equals(XSD.nonPositiveInteger)
                     || predicate.getDataType().equals(XSD.positiveInteger)
                     || predicate.getDataType().equals(XSD.short_)
                     || predicate.getDataType().equals(XSD.unsignedShort)
                     || predicate.getDataType().equals(XSD.byte_)
                     || predicate.getDataType().equals(XSD.unsignedByte)) {

            }
            else if (predicate.getDataType().equals(XSD.language)) {

            }
            else if (predicate.getDataType().equals(XSD.long_)
                     || predicate.getDataType().equals(XSD.unsignedLong)) {

            }
            else if (predicate.getDataType().equals(XSD.float_)) {

            }
            else if (predicate.getDataType().equals(XSD.double_)
                     //this is doubtful, but let's take the largest one
                     // Note we could also try to fit as closely as possible, but that would change the type per value (instead of per 'column'), and that's not a good idea
                     || predicate.getDataType().equals(XSD.decimal)) {

            }
            else if (predicate.getDataType().equals(XSD.string)
                     || predicate.getDataType().equals(XSD.normalizedString)
                     || predicate.getDataType().equals(RDF.langString)
                     //this is a little tricky, but in the end it's just a string, right?
                     || predicate.getDataType().equals(XSD.base64Binary)) {

            }
            else if (predicate.getDataType().equals(RDF.HTML)) {

            }
            else if (predicate.getDataType().equals(XSD.anyURI)) {

            }
            else {
                //this probably means we added a type to SolrConfigs without implementing it here
                throw new IOException("Encountered unsupported RDF property '" + predicate + "' while serializing RDF value; " + rdfValue);
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
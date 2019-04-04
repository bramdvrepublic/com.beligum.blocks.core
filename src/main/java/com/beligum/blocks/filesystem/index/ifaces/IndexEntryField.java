package com.beligum.blocks.filesystem.index.ifaces;

public interface IndexEntryField
{
    //-----CONSTANTS-----
    /**
     * This value is used to index "core" Fields (eg. the ones explicitly implementing IndexEntryField) to support
     * an easy means to search for null fields (eg. search for all fields NOT having this field). Analogue to:
     * https://www.elastic.co/guide/en/elasticsearch/reference/2.1/null-value.html
     */
    String NULL_VALUE = "NULL";

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * The name of this field, as it will be used in the index
     */
    String getName();

    /**
     * Returns the value of the index entry, associated with this field
     */
    String getValue(IndexEntry indexEntry);

    /**
     * Returns true if the value of the index entry has been set once (even if it was set to null)
     */
    boolean hasValue(IndexEntry indexEntry);

    /**
     * Sets the field of the indexEntry to the supplied value
     */
    void setValue(IndexEntry indexEntry, String value);

    /**
     * Make sure this just returns the name
     */
    String toString();
}

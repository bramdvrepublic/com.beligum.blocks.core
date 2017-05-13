package com.beligum.blocks.filesystem.index.entries;

/**
 * Created by bram on 5/31/16.
 */
public interface RdfIndexer
{
    //-----CONSTANTS-----
    class IndexResult
    {
        /**
         * The raw value as it was indexed
         */
        public Object indexValue;
        /**
         * The human-readable string value (eg. to use for sorting)
         */
        public String stringValue;

        public IndexResult()
        {
            this(null, null);
        }
        public IndexResult(Object indexValue)
        {
            this.indexValue = indexValue;
            this.stringValue = indexValue == null ? null : indexValue.toString();
        }
        public IndexResult(Object indexValue, String stringValue)
        {
            this.indexValue = indexValue;
            this.stringValue = stringValue;
        }
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * Add the supplied value as an integer to the index
     */
    void indexIntegerField(String fieldName, int value);

    /**
     * Add the supplied value as a long to the index
     */
    void indexLongField(String fieldName, long value);

    /**
     * Add the supplied value as a float to the index
     */
    void indexFloatField(String fieldName, float value);

    /**
     * Add the supplied value as a double to the index
     */
    void indexDoubleField(String fieldName, double value);

    /**
     * Add the supplied value as an *analyzed* string to the index
     */
    void indexStringField(String fieldName, String value);

    /**
     * Add the supplied value as a *constant* string to the index
     */
    void indexConstantField(String fieldName, String value);

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

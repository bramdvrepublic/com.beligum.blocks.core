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
    void indexIntegerField(String fieldName, int value);

    void indexLongField(String fieldName, long value);

    void indexFloatField(String fieldName, float value);

    void indexDoubleField(String fieldName, double value);

    void indexStringField(String fieldName, String value);

    void indexConstantField(String fieldName, String value);

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

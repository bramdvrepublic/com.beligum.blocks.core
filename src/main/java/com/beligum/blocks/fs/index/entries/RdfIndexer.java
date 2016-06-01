package com.beligum.blocks.fs.index.entries;

/**
 * Created by bram on 5/31/16.
 */
public interface RdfIndexer
{
    //-----CONSTANTS-----

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

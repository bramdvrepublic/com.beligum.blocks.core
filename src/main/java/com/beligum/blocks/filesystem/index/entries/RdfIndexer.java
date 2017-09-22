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

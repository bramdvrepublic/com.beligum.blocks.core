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

package com.beligum.blocks.templating;

import com.google.common.base.Joiner;

import java.util.ArrayList;

/**
 * Same as ArrayList but with a less-verbose toString() method
 * Created by bram on 7/5/15.
 */
public class PropertyArray<E> extends ArrayList<E>
{
    //-----CONSTANTS-----
    //sync these two
    public static final String PROPARR_FIELD = "PROPARR";
    public static final boolean PROPARR = true;

    //-----VARIABLES-----
    private String cachedJoin = null;
    private int writeCounter = 0;

    //-----CONSTRUCTORS-----
    public PropertyArray()
    {
        super();
    }

    //-----PUBLIC METHODS-----
    //NOTE sync these two
    // @see com.beligum.blocks.templating.HtmlParser for details about this
    public static final String WRITE_ONCE_METHOD_NAME = "writeOnce";
    public String writeOnce()
    {
        String retVal = "";

        if (this.writeCounter < 1) {
            //will use the overloaded toString() below
            retVal = this.buildString();
            this.writeCounter++;
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----
    private String buildString()
    {
        //this is safe because this array is constructed from the VTL while parsing and only rendered out when done
        if (this.cachedJoin == null) {
            this.cachedJoin = Joiner.on("").join(this);
        }

        return this.cachedJoin;
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    /**
     * @return the list without extra punctuation; copied and adapted from super.toString()
     */
    @Override
    public String toString()
    {
        return this.buildString();
    }
}

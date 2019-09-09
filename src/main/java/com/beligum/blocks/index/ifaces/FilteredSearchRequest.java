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

package com.beligum.blocks.index.ifaces;

import java.io.Serializable;

/**
 * Created by bram on 6/3/17.
 */
public interface FilteredSearchRequest extends Serializable
{
    /**
     * General interface to pass multiple options to the methods in this request
     */
    interface Option
    {
    }
    /**
     * Interface for all options that define how the passed value to the methods in this class are interpreted
     */
    interface ValueOption extends Option
    {
    }

    enum FilterBoolean implements FilteredSearchRequest.Option
    {
        AND,
        OR,
        NOT,
        //do not add a filterBoolean
        NONE
    }
    enum QueryType  implements FilteredSearchRequest.Option
    {
        MAIN,
        FILTER
    }



}

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

package com.beligum.blocks.index.results;

import com.beligum.blocks.index.ifaces.RdfResult;

/**
 * Created by bram on 6/05/17.
 */
public class StringTuple implements RdfResult.Tuple<String, String>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String label;
    private String value;

    //-----CONSTRUCTORS-----
    public StringTuple(String label, String value)
    {
        this.label = label;
        this.value = value;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getLabel()
    {
        return label;
    }
    @Override
    public String getValue()
    {
        return value;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

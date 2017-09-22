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

package com.beligum.blocks.utils.importer;

/**
 * Created by bram on 3/25/16.
 */
public class RdfPropertyValue
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String value;

    //-----CONSTRUCTORS-----
    public RdfPropertyValue(String value)
    {
        this.value = value;
    }

    //-----PUBLIC METHODS-----
    public String getValue()
    {
        return value;
    }
    public void setValue(String value)
    {
        this.value = value;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "'"+value+"'";
    }
}

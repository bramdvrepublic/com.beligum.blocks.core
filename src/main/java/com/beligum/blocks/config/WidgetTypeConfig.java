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

package com.beligum.blocks.config;

import java.util.HashMap;

/**
 * Created by bram on 3/7/16.
 */
public class WidgetTypeConfig extends HashMap<String, String>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    /**
     * Create an empty configuration
     */
    public WidgetTypeConfig()
    {
        super();
    }
    /**
     * Create a configuration filled with the supplied tuples
     */
    public WidgetTypeConfig(String[][] args)
    {
        this();

        for (String[] arg : args) {
            this.put(arg[0], arg[1]);
        }
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

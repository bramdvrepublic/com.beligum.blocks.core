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

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import static com.beligum.blocks.config.InputType.valueOfConstant;

/**
 * Created by bram on 3/25/16.
 */
public class InputTypeAdapter extends XmlAdapter<String, InputType>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public String marshal(InputType inputType)
    {
        return inputType == null ? null : inputType.getConstant();
    }
    @Override
    public InputType unmarshal(String val)
    {
        return StringUtils.isEmpty(val) ? null : valueOfConstant(val);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

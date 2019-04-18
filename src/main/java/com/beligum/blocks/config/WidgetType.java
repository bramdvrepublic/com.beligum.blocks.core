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

import com.beligum.base.filesystem.ConstantsFileEntry;
import gen.com.beligum.blocks.core.constants.blocks.core;

import java.util.HashMap;
import java.util.Map;

/**
 * This represents a widget in the blocks sidebar. To be used to link RDF data types to input-widgets.
 * <p>
 * Created by bram on 2/26/16.
 */
public enum WidgetType
{
    //-----CONSTANTS-----
    //this mapping makes sure we can use them in JS/CSS/...
    Undefined(core.Entries.INPUT_TYPE_UNDEFINED),
    Editor(core.Entries.INPUT_TYPE_EDITOR),
    InlineEditor(core.Entries.INPUT_TYPE_INLINE_EDITOR),
    Enum(core.Entries.INPUT_TYPE_ENUM),
    Boolean(core.Entries.INPUT_TYPE_BOOLEAN),
    Number(core.Entries.INPUT_TYPE_NUMBER),
    Date(core.Entries.INPUT_TYPE_DATE),
    Time(core.Entries.INPUT_TYPE_TIME),
    DateTime(core.Entries.INPUT_TYPE_DATETIME),
    Color(core.Entries.INPUT_TYPE_COLOR),
    Uri(core.Entries.INPUT_TYPE_URI),
    Resource(core.Entries.INPUT_TYPE_RESOURCE),
    Object(core.Entries.INPUT_TYPE_OBJECT),
    ;

    /**
     * this will allow us to map to this enum from a constant string value
     */
    private static final Map<String, WidgetType> constantValueMapping = new HashMap<>();

    static {
        for (WidgetType widget : WidgetType.values()) {
            constantValueMapping.put(widget.getConstant(), widget);
        }
    }

    //-----VARIABLES-----
    private ConstantsFileEntry constantEntry;

    //-----CONSTRUCTORS-----
    WidgetType(ConstantsFileEntry constantEntry)
    {
        this.constantEntry = constantEntry;
    }

    //-----STATIC METHODS-----
    public static WidgetType valueOfConstant(String constantValue)
    {
        return constantValueMapping.get(constantValue);
    }

    //-----PUBLIC METHODS-----
    /**
     * Note: this value will be used instead of the name of the enum during serialization;
     * Eg. see the @XmlJavaTypeAdapter on com.beligum.blocks.rdf.ifaces.RdfProperty.getWidgetType()
     */
    public String getConstant()
    {
        return constantEntry.getValue();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

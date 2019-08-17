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
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfResource;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.rdf.ontologies.XSD;
import gen.com.beligum.blocks.core.constants.blocks.core;

import java.util.*;

import static gen.com.beligum.blocks.core.constants.blocks.core.Entries.*;

/**
 * This represents a widget in the blocks sidebar. To be used to link RDF data types to input-widgets.
 * <p>
 * Created by bram on 2/26/16.
 */
public enum WidgetType
{
    //-----CONSTANTS-----
    //this mapping makes sure we can use them in JS/CSS/...
    Undefined(core.Entries.WIDGET_TYPE_UNDEFINED),

    // Note: for now, we assume this will only be used to add primitive types, but adding extra compatible classes
    // should only be a matter of implementing the JS side
    Immutable(WIDGET_TYPE_IMMUTABLE, new RdfClass[] { XSD.string,
                                                      XSD.byte_, XSD.unsignedByte,
                                                      XSD.int_, XSD.unsignedInt,
                                                      XSD.short_, XSD.unsignedShort,
                                                      XSD.long_, XSD.unsignedLong,
                                                      XSD.float_,
                                                      XSD.double_ }, null, new ConstantsFileEntry[] {
                    // the endpoint to fetch the initial value on creation
                    // note that the value should be returned by ResourceProxy.getResource()
                    WIDGET_CONFIG_IMMUTABLE_ENDPOINT
    }),

    Editor(core.Entries.WIDGET_TYPE_EDITOR, new RdfClass[] { RDF.HTML }, null, new ConstantsFileEntry[0]),

    //note: dependent on the type of value, it is language-oriented or not (eg. an identifier doesn't have a language)
    InlineEditor(core.Entries.WIDGET_TYPE_INLINE_EDITOR, new RdfClass[] { XSD.string, RDF.langString }),

    // For why we also include xsd:language, see LanguageEnumQueryEndpoint
    Enum(core.Entries.WIDGET_TYPE_ENUM, new RdfClass[] { XSD.string, XSD.language }, null, new ConstantsFileEntry[] {
                    // the endpoint to fetch all the possible values of this enum
                    WIDGET_CONFIG_ENUM_ENDPOINT
    }),

    Boolean(core.Entries.WIDGET_TYPE_BOOLEAN, new RdfClass[] { XSD.boolean_ }),

    Number(core.Entries.WIDGET_TYPE_NUMBER, new RdfClass[] { XSD.byte_, XSD.unsignedByte,
                                                             XSD.int_, XSD.unsignedInt,
                                                             XSD.short_, XSD.unsignedShort,
                                                             XSD.long_, XSD.unsignedLong,
                                                             XSD.float_,
                                                             XSD.double_ }),

    Date(core.Entries.WIDGET_TYPE_DATE, new RdfClass[] { XSD.date }),

    Time(core.Entries.WIDGET_TYPE_TIME, new RdfClass[] { XSD.time }),

    DateTime(core.Entries.WIDGET_TYPE_DATETIME, new RdfClass[] { XSD.dateTime }),

    Duration(core.Entries.WIDGET_TYPE_DURATION, new RdfClass[] { XSD.long_ }, null, new ConstantsFileEntry[] {
                    // the format of the human readable string of the value;
                    // can be any of these:
                    // WIDGET_CONFIG_DURATION_FORMAT_FULL
                    //   the duration will be written out in full, eg. 7 days, 3 hours, 3 minutes, 3 seconds, 7 milliseconds
                    //   note: this is the default
                    // WIDGET_CONFIG_DURATION_FORMAT_SHORT
                    //   the duration will be coded, eg. 7.23:59:59.999
                    // WIDGET_CONFIG_DURATION_FORMAT_ISO
                    //   the duration will be formatted in ISO 8601, eg. P1Y2M3DT4H5M6S
                    WIDGET_CONFIG_DURATION_FORMAT
    }),

    Timecode(core.Entries.WIDGET_TYPE_TIMECODE, new RdfClass[] { XSD.float_ }),

    Color(core.Entries.WIDGET_TYPE_COLOR, new RdfClass[] { XSD.string }),

    Uri(core.Entries.WIDGET_TYPE_URI, new RdfClass[] { XSD.anyURI }),

    Resource(core.Entries.WIDGET_TYPE_RESOURCE, new RdfClass[] { XSD.anyURI }, RdfClass.class, new ConstantsFileEntry[] {
                    // the autocomplete endpoint for the resource admin inputbox
                    WIDGET_CONFIG_RESOURCE_AC_ENDPOINT,
                    // the endpoint to request more information about the true resource value returned by the autocomplete
                    WIDGET_CONFIG_RESOURCE_VAL_ENDPOINT,
                    // the maximum number of suggestions or enum entries to render out
                    WIDGET_CONFIG_RESOURCE_MAXRESULTS,
                    // enables or disables the image rendering of a resource
                    // (when an image is available in the ResourceProxy)
                    // Default: true
                    WIDGET_CONFIG_RESOURCE_ENABLE_IMG,
                    // enables or disables the link rendering of a resource
                    // (when an link is available in the ResourceProxy)
                    // Default: true
                    WIDGET_CONFIG_RESOURCE_ENABLE_HREF,
                    // enables or disables the resource admin widget to be
                    // rendered as a combobox instead of an autocomplete
                    // Default: false
                    WIDGET_CONFIG_RESOURCE_ENABLE_COMBOBOX }),

    Object(core.Entries.WIDGET_TYPE_OBJECT, new RdfClass[0], RdfClass.class),
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
    private Set<RdfClass> compatibleDatatypes;
    private Class<? extends RdfResource> compatibleSuperclass;
    private Set<ConstantsFileEntry> compatibleConfigKeys;

    //-----CONSTRUCTORS-----
    WidgetType(ConstantsFileEntry constantEntry)
    {
        this(constantEntry, new RdfClass[0], null, new ConstantsFileEntry[0]);
    }
    WidgetType(ConstantsFileEntry constantEntry, RdfClass[] compatibleDatatypes)
    {
        this(constantEntry, compatibleDatatypes, null, new ConstantsFileEntry[0]);
    }
    WidgetType(ConstantsFileEntry constantEntry, RdfClass[] compatibleDatatypes, Class<? extends RdfResource> compatibleSuperclass)
    {
        this(constantEntry, compatibleDatatypes, compatibleSuperclass, new ConstantsFileEntry[0]);
    }
    WidgetType(ConstantsFileEntry constantEntry, RdfClass[] compatibleDatatypes, Class<? extends RdfResource> compatibleSuperclass, ConstantsFileEntry[] compatibleConfigKeys)
    {
        this.constantEntry = constantEntry;
        this.compatibleDatatypes = new HashSet<>(Arrays.asList(compatibleDatatypes));
        this.compatibleSuperclass = compatibleSuperclass;
        this.compatibleConfigKeys = new LinkedHashSet<>(Arrays.asList(compatibleConfigKeys));
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
    public Set<RdfClass> getCompatibleDatatypes()
    {
        return compatibleDatatypes;
    }
    public boolean isCompatibleDatatype(RdfClass datatype)
    {
        boolean retVal = this.compatibleDatatypes.contains(datatype);

        if (!retVal && this.compatibleSuperclass != null) {
            retVal = this.compatibleSuperclass.isAssignableFrom(datatype.getClass());
        }

        return retVal;
    }
    public Class<?> getCompatibleSuperclass()
    {
        return compatibleSuperclass;
    }
    public Set<ConstantsFileEntry> getCompatibleConfigKeys()
    {
        return compatibleConfigKeys;
    }
    public boolean isCompatibleConfigKey(ConstantsFileEntry config)
    {
        return this.compatibleConfigKeys.contains(config);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

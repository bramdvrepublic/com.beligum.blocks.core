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

package com.beligum.blocks.rdf.importers.semargl;

import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.RioSettingImpl;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.semarglproject.source.StreamProcessor;
import org.xml.sax.XMLReader;

/**
 * Settings specific to Semargl that are not in {@link org.openrdf.rio.helpers.BasicParserSettings}.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 * @since 0.5
 */
public final class SemarglParserSettings
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * TODO: Javadoc this setting
     * <p>
     * Defaults to false
     * @since 0.5
     */
    public static final RioSetting<Boolean> PROCESSOR_GRAPH_ENABLED = new RioSettingImpl<Boolean>(
                    RdfaParser.ENABLE_PROCESSOR_GRAPH, "Vocabulary Expansion", Boolean.FALSE);

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * TODO: Javadoc this setting
     * <p>
     * Defaults to null
     * @since 0.5
     */
    public static final RioSetting<XMLReader> CUSTOM_XML_READER = new RioSettingImpl<XMLReader>(
                    StreamProcessor.XML_READER_PROPERTY, "Custom XML Reader", null);

    private SemarglParserSettings()
    {
    }
}

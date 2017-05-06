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

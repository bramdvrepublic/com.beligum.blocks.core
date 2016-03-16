package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.config.Settings;

/**
 * Created by bram on 2/28/16.
 */
public class SettingsVocabulary extends AbstractRdfVocabulary
{
    //-----VARIABLES-----

    //-----SINGLETON-----
    public static final SettingsVocabulary INSTANCE = new SettingsVocabulary();
    private SettingsVocabulary()
    {
        super(Settings.instance().getRdfOntologyUri(), Settings.instance().getRdfOntologyPrefix());
    }

    //-----ENTRIES-----
}

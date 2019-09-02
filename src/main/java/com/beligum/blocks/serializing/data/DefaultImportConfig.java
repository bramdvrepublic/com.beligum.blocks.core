package com.beligum.blocks.serializing.data;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ontologies.Meta;

import java.net.URI;

/**
 * Created by bram on Sep 02, 2019
 */
public class DefaultImportConfig extends ImportConfig
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public DefaultImportConfig()
    {
        super();

        this.template = URI.create("/resources/templates/importer/resource.html");
        this.sameasProperty = Meta.sameAs.getCurie();
        this.titleProperty = Settings.instance().getRdfLabelProperty().getCurie();
        this.imageProperty = RdfFactory.getMainOntology().getMember("image").getCurie();
        this.videoProperty = RdfFactory.getMainOntology().getMember("video").getCurie();
        this.factBlock = "blocks-fact-entry";
        this.imageBlock = "blocks-image";
        this.videoBlock = "blocks-video";
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

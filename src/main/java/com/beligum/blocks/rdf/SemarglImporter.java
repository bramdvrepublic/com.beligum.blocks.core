package com.beligum.blocks.rdf;

import com.beligum.blocks.rdf.ifaces.Source;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.semarglproject.jena.rdf.rdfa.JenaRdfaReader;

import java.io.*;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/23/16.
 */
public class SemarglImporter extends AbstractImporter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public SemarglImporter()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public Model importDocument(Source source, Format inputFormat) throws IOException, URISyntaxException
    {
        if (inputFormat.equals(Format.RDFA)) {
            JenaRdfaReader.inject();
        }

        Model model = ModelFactory.createDefaultModel();
        try (InputStream is = source.openNewInputStream()) {
            model.read(is, source.getBaseUri().toString(), inputFormat.getSemarglType());
        }

        model = this.filterRelevantNodes(model, source.getBaseUri());

        return model;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

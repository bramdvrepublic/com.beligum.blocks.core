package com.beligum.blocks.rdf;

import com.beligum.blocks.rdf.ifaces.Exporter;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/23/16.
 */
public class JenaExporter implements Exporter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public JenaExporter()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public void exportModel(Model model, Format outputFormat, OutputStream outputStream) throws IOException, URISyntaxException
    {
        RDFDataMgr.write(outputStream, model, outputFormat.getJenaType());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

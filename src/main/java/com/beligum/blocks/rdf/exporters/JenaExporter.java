package com.beligum.blocks.rdf.exporters;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by bram on 1/23/16.
 */
public class JenaExporter extends AbstractExporter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public JenaExporter(Format exportFormat)
    {
        super(exportFormat);
    }

    //-----PUBLIC METHODS-----
    @Override
    public void exportModel(Model model, OutputStream outputStream) throws IOException
    {
        RDFDataMgr.write(outputStream, model, this.translateFormat(this.exportFormat));
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private Lang translateFormat(Format exportFormat) throws IOException
    {
        switch (exportFormat) {
            case JSONLD:
                return Lang.JSONLD;
            default:
                throw new IOException("Unsupported exporter format detected; "+exportFormat);
        }
    }
}

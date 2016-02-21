package com.beligum.blocks.rdf.exporters;

import com.beligum.blocks.rdf.ifaces.Format;
import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by bram on 1/23/16.
 */
public class SesameExporter extends AbstractExporter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public SesameExporter(Format exportFormat)
    {
        super(exportFormat);
    }

    //-----PUBLIC METHODS-----
    @Override
    public void exportModel(Model model, OutputStream outputStream) throws IOException
    {
        RDFWriter writer = Rio.createWriter(this.translateFormat(this.exportFormat), outputStream);

        try {
            writer.startRDF();

            //export the namespaces as well
            for (Namespace nextNamespace : ((Model)model).getNamespaces()) {
                writer.handleNamespace(nextNamespace.getPrefix(), nextNamespace.getName());
            }

            for (Statement st : model) {
                writer.handleStatement(st);
            }

            writer.endRDF();
        }
        catch (Exception e) {
            throw new IOException("Error while exporting model with sesame exporter", e);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private RDFFormat translateFormat(Format exportFormat) throws IOException
    {
        switch (exportFormat) {
            case JSONLD:
                return RDFFormat.JSONLD;
            case NTRIPLES:
                return RDFFormat.NTRIPLES;
            default:
                throw new IOException("Unsupported exporter format detected; "+exportFormat);
        }
    }
}

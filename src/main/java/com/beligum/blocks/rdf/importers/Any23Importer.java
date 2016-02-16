package com.beligum.blocks.rdf.importers;

import com.beligum.blocks.rdf.ifaces.Source;
import org.apache.any23.Any23;
import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.extractor.rdfa.RDFa11ExtractorFactory;
import org.apache.any23.source.ByteArrayDocumentSource;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.writer.NTriplesWriter;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by bram on 1/23/16.
 */
public class Any23Importer extends AbstractImporter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public Any23Importer(Format inputFormat) throws IOException
    {
        super(inputFormat);
    }

    //-----PUBLIC METHODS-----
    @Override
    public Model importDocument(Source source) throws IOException
    {
        Model model = ModelFactory.createDefaultModel();
        this.readToJenaModel(source, this.inputFormat, model);

        //Note: this doesn't seem to do anything for this importer (Any23 doesn't return an expanded @graph form)
        //model = this.filterRelevantNodes(model, source.getBaseUri());

        return model;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /*
     * See http://stackoverflow.com/questions/15140531/how-to-add-apache-any23-rdf-statements-to-apache-jena
     */
    private void readToJenaModel(Source source, Format inputFormat, Model model) throws IOException
    {
        Any23 runner = new Any23(this.translateFormat(inputFormat));

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            DocumentSource documentSource;
            try (InputStream is = source.openNewInputStream()) {
                documentSource = new ByteArrayDocumentSource(is, source.getSourceAddress().normalize().toString(), null);
            }

            TripleHandler handler = new NTriplesWriter(os);
            try {
                runner.extract(documentSource, handler);
            }
            catch (ExtractionException e) {
                throw new IOException("Error happened while parsing the document; "+documentSource.getDocumentURI(), e);
            }
            finally {
                try {
                    handler.close();
                }
                catch (TripleHandlerException e) {
                    throw new IOException("Error happened while closing the triple writer", e);
                }
            }

            try (InputStream decodedInput = new ByteArrayInputStream(os.toByteArray());) {
                model.read(decodedInput, source.getSourceAddress().toString(), "N-TRIPLE");
            }
        }
    }
    private String translateFormat(Format inputFormat) throws IOException
    {
        switch (inputFormat) {
            case RDFA:
                return RDFa11ExtractorFactory.NAME;
            default:
                throw new IOException("Unsupported importer format detected; "+inputFormat);
        }
    }
}

package com.beligum.blocks.rdf;

import com.beligum.blocks.rdf.ifaces.Source;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.any23.Any23;
import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.source.ByteArrayDocumentSource;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.writer.NTriplesWriter;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/23/16.
 */
public class Any23Importer extends AbstractImporter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public Any23Importer()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public Model importDocument(Source source, Format inputFormat) throws IOException, URISyntaxException
    {
        Model model = ModelFactory.createDefaultModel();
        this.readToJenaModel(source, inputFormat, model);

        //Note: this doesn't seem to do anything for this importer (Any23 doesn't return an expanded @graph form)
        model = this.filterRelevantNodes(model, source.getBaseUri());

        return model;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /*
     * See http://stackoverflow.com/questions/15140531/how-to-add-apache-any23-rdf-statements-to-apache-jena
     */
    private void readToJenaModel(Source source, Format inputFormat, Model model) throws IOException
    {
        Any23 runner = new Any23(inputFormat.getAny23Type());

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            DocumentSource documentSource;
            try (InputStream is = source.openNewInputStream()) {
                documentSource = new ByteArrayDocumentSource(is, source.getBaseUri().normalize().toString(), null);
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
                model.read(decodedInput, source.getBaseUri().toString(), "N-TRIPLE");
            }
        }
    }
}

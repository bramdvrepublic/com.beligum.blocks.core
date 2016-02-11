package com.beligum.blocks.rdf.importers;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.rdf.ifaces.Source;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.rio.*;
import org.openrdf.rio.helpers.RDFaParserSettings;
import org.openrdf.rio.helpers.RDFaVersion;
import org.openrdf.rio.helpers.StatementCollector;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

/**
 * Created by bram on 1/23/16.
 */
public class SesameImporter extends AbstractImporter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public SesameImporter(Format inputFormat)
    {
        super(inputFormat);
    }

    //-----PUBLIC METHODS-----
    @Override
    public Model importDocument(Source source) throws IOException
    {
        RDFParser parser = Rio.createParser(this.translateFormat(this.inputFormat));
        configureParser(parser, this.inputFormat);

        org.openrdf.model.Model sesameModel = new org.openrdf.model.impl.LinkedHashModel();
        parser.setRDFHandler(new StatementCollector(sesameModel));

        try (InputStream is = source.openNewInputStream()) {
            parser.parse(is, source.getBaseUri().toString());
        }
        catch (OpenRDFException e) {
            //when an exception is thrown, it's very handy to have the html source code, so add it to the exception
            throw new IOException(source.toString(), e);
        }

        //convert sesame to jena model
        Model model = ModelFactory.createDefaultModel();
        for (Statement stmt : sesameModel) {
            model.add(Convert.statementToJenaStatement(model, stmt));
        }

        //Note: this doesn't seem to do anything for this importer (Any23 doesn't return an expanded @graph form)
        model = this.filterRelevantNodes(model, source.getBaseUri());

        return model;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private RDFFormat translateFormat(Format inputFormat) throws IOException
    {
        switch (inputFormat) {
            case RDFA:
                return RDFFormat.RDFA;
            default:
                throw new IOException("Unsupported importer format detected; "+inputFormat);
        }
    }
    private void configureParser(RDFParser parser, Format inputFormat)
    {
        // adjusting these is exactly why we implemented a Sesame importer over an Any23 importer that also uses sesame internally);
        // see org.apache.any23.extractor.rdf.BaseRDFExtractor.run()

        parser.setParseErrorListener(new InternalParseErrorListener());

        parser.getParserConfig().setNonFatalErrors(new HashSet<RioSetting<?>>());

//        parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
//        parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);

        // Disable verification to ensure that DBPedia is accessible, given it uses so many custom datatypes
//        parser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false);
//        parser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);

//        parser.getParserConfig().set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, false);
//        parser.getParserConfig().addNonFatalError(BasicParserSettings.NORMALIZE_DATATYPE_VALUES);

//        parser.getParserConfig().set(BasicParserSettings.VERIFY_RELATIVE_URIS, true);
//        parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_RELATIVE_URIS);

        //specific config
        switch (inputFormat) {
            case RDFA:
                parser.getParserConfig().set(RDFaParserSettings.RDFA_COMPATIBILITY, RDFaVersion.RDFA_1_1);
//                parser.getParserConfig().set(RDFaParserSettings.VOCAB_EXPANSION_ENABLED, false);
                //parser.getParserConfig().set(RDFaParserSettings.FAIL_ON_RDFA_UNDEFINED_PREFIXES, false);
                break;
        }
    }

    //-----INNER CLASSES-----
    /**
     * Internal listener used to trace <i>RDF</i> parse errors.
     */
    private class InternalParseErrorListener implements ParseErrorListener
    {
        public void warning(String msg, int lineNo, int colNo)
        {
            Logger.warn(msg+"\n" +
                        "  line: "+lineNo+"\n" +
                        "  column: "+colNo);
        }
        public void error(String msg, int lineNo, int colNo)
        {
            Logger.error(msg+"\n" +
                        "  line: "+lineNo+"\n" +
                        "  column: "+colNo);
        }
        public void fatalError(String msg, int lineNo, int colNo)
        {
            //TODO don't know if this is the best approach...
            throw new RuntimeException(msg+"\n" +
                        "  line: "+lineNo+"\n" +
                        "  column: "+colNo);
        }
    }
}

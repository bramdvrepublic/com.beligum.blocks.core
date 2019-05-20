/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.rdf.importers;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.exceptions.RdfValidationException;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.importers.semargl.SesameRDFaParser;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.RDFaParserSettings;
import org.eclipse.rdf4j.rio.helpers.RDFaVersion;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
    public Model importDocument(URI baseUri, InputStream inputStream) throws IOException
    {
        try {
            return this.parseInputStream(baseUri, inputStream);
        }
        //don't wrap the validation exception in a IOException or the wrong exception handler will fire
        catch (RdfValidationException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException("Exception caught while parsing RDF import file; " + baseUri, e);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private Model parseInputStream(URI baseURI, InputStream is) throws IOException, RDFParseException, RDFHandlerException
    {
        // we needed to re-implement the SesameRDFaParser to make it work with Sesame 4;
        // so make an exception if we're using RDFa (so we don't have to instance a service loader)
        RDFParser parser = null;
        if (this.inputFormat == Format.RDFA) {
            parser = new SesameRDFaParser();
        }
        else {
            parser = Rio.createParser(this.translateFormat(this.inputFormat));
        }

        //give ourself a chance to set custom settings
        configureParser(parser, this.inputFormat);

        Model model = new LinkedHashModel();
        parser.setRDFHandler(new StatementCollector(model));

        parser.parse(is, baseURI.toString());

        model = this.filterModel(model, baseURI);

        return model;
    }
    private RDFFormat translateFormat(Format inputFormat) throws IOException
    {
        switch (inputFormat) {
            case RDFA:
                return RDFFormat.RDFA;
            case NTRIPLES:
                return RDFFormat.NTRIPLES;
            case RDF_XML:
                return RDFFormat.RDFXML;
            default:
                throw new IOException("Unsupported importer format detected; " + inputFormat);
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

        parser.getParserConfig().set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, false);
        parser.getParserConfig().addNonFatalError(BasicParserSettings.NORMALIZE_DATATYPE_VALUES);

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
        @Override
        public void warning(String msg, long lineNo, long colNo)
        {
            Logger.warn(msg + "\n" +
                        "  line: " + lineNo + "\n" +
                        "  column: " + colNo);
        }
        @Override
        public void error(String msg, long lineNo, long colNo)
        {
            Logger.error(msg + "\n" +
                         "  line: " + lineNo + "\n" +
                         "  column: " + colNo);
        }
        @Override
        public void fatalError(String msg, long lineNo, long colNo)
        {
            //TODO don't know if this is the best approach...
            throw new RuntimeException(msg + "\n" +
                                       "  line: " + lineNo + "\n" +
                                       "  column: " + colNo);
        }
    }
}

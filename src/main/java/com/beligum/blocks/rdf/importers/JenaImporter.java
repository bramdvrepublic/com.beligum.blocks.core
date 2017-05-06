//package com.beligum.blocks.rdf.importers;
//
//import com.beligum.blocks.rdf.ifaces.Format;
//import com.beligum.blocks.rdf.ifaces.Source;
//import org.apache.jena.rdf.model.Model;
//import org.apache.jena.rdf.model.ModelFactory;
//import org.apache.jena.riot.Lang;
//import org.apache.jena.riot.RDFDataMgr;
//import org.eclipse.rdf4j.rio.RDFHandlerException;
//import org.eclipse.rdf4j.rio.RDFParseException;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URI;
//
///**
// * Created by bram on 1/23/16.
// */
//public class JenaImporter extends AbstractImporter
//{
//    //-----CONSTANTS-----
//
//    //-----VARIABLES-----
//
//    //-----CONSTRUCTORS-----
//    public JenaImporter(Format inputFormat)
//    {
//        super(inputFormat);
//    }
//
//    //-----PUBLIC METHODS-----
//    @Override
//    public Model importDocument(Source source) throws IOException
//    {
//        try (InputStream is = source.newInputStream()) {
//            return this.parseInputStream(is, source.getUri());
//        }
//        catch (Exception e) {
//            //when an exception is thrown, it's very handy to have the html source code, so add it to the exception
//            throw new IOException("Exception caught while parsing RDF import source; "+source.toString(), e);
//        }
//    }
//    @Override
//    public Model importDocument(InputStream inputStream, URI baseUri) throws IOException
//    {
//        try {
//            return this.parseInputStream(inputStream, baseUri);
//        }
//        catch (Exception e) {
//            throw new IOException("Exception caught while parsing RDF import file; "+baseUri, e);
//        }
//    }
//
//    //-----PROTECTED METHODS-----
//
//    //-----PRIVATE METHODS-----
//    private Model parseInputStream(InputStream is, URI baseURI) throws IOException, RDFParseException, RDFHandlerException
//    {
//        Model model = ModelFactory.createDefaultModel();
//
//        RDFDataMgr.read(model, is, this.translateFormat(this.inputFormat));
//
//        model = this.filterModel(model, baseURI);
//
//        return model;
//    }
//    private Lang translateFormat(Format inputFormat) throws IOException
//    {
//        switch (inputFormat) {
//            case JSONLD:
//                return Lang.JSONLD;
//            case NTRIPLES:
//                return Lang.NTRIPLES;
//            default:
//                throw new IOException("Unsupported importer format detected; "+inputFormat);
//        }
//    }
//
//    //-----INNER CLASSES-----
//
//}

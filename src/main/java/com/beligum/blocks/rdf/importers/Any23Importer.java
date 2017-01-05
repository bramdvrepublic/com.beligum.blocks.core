//package com.beligum.blocks.rdf.importers;
//
//import com.beligum.blocks.rdf.ifaces.Format;
//import com.beligum.blocks.rdf.ifaces.Source;
//import org.apache.any23.Any23;
//import org.apache.any23.extractor.rdf.JSONLDExtractorFactory;
//import org.apache.any23.extractor.rdf.NTriplesExtractorFactory;
//import org.apache.any23.extractor.rdfa.RDFa11ExtractorFactory;
//import org.apache.any23.source.ByteArrayDocumentSource;
//import org.apache.any23.source.DocumentSource;
//import org.apache.any23.writer.ReportingTripleHandler;
//import org.apache.any23.writer.RepositoryWriter;
//import org.apache.any23.writer.TripleHandler;
//import org.openrdf.model.Model;
//import org.openrdf.query.QueryResults;
//import org.openrdf.repository.Repository;
//import org.openrdf.repository.RepositoryConnection;
//import org.openrdf.repository.sail.SailRepository;
//import org.openrdf.sail.memory.MemoryStore;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URI;

//<dependency>
//<groupId>org.apache.any23</groupId>
//<artifactId>apache-any23-core</artifactId>
//<version>1.1</version>
//<exclusions>
//<exclusion>
//<!-- This one doesn't seem to exist?? -->
//<groupId>org.apache.commons</groupId>
//<artifactId>commons-csv</artifactId>
//<!--<version>1.0-SNAPSHOT-rev1148315</version>-->
//</exclusion>
//</exclusions>
//</dependency>

///**
// * Created by bram on 1/23/16.
// */
//public class Any23Importer extends AbstractImporter
//{
//    //-----CONSTANTS-----
//
//    //-----VARIABLES-----
//
//    //-----CONSTRUCTORS-----
//    public Any23Importer(Format inputFormat) throws IOException
//    {
//        super(inputFormat);
//    }
//
//    //-----PUBLIC METHODS-----
//    @Override
//    public Model importDocument(Source source) throws IOException
//    {
//        try (InputStream is = source.newInputStream()) {
//            return this.readToModel(is, source.getUri(), this.inputFormat);
//        }
//    }
//    @Override
//    public Model importDocument(InputStream inputStream, URI baseUri) throws IOException
//    {
//        return this.readToModel(inputStream, baseUri, this.inputFormat);
//    }
//
//    //-----PROTECTED METHODS-----
//
//    //-----PRIVATE METHODS-----
//    /*
//     * See http://stackoverflow.com/questions/15140531/how-to-add-apache-any23-rdf-statements-to-apache-jena
//     */
//    private Model readToModel(InputStream is, URI baseURI, Format inputFormat) throws IOException
//    {
//        Model retVal = null;
//
//        try {
//            Any23 runner = new Any23(this.translateFormat(inputFormat));
//
//            RepositoryConnection connection = null;
//            TripleHandler writer = null;
//            try {
//                Repository store = new SailRepository(new MemoryStore());
//                store.initialize();
//                connection = store.getConnection();
//
//                DocumentSource reader = new ByteArrayDocumentSource(is, baseURI.toString(), null);
//                writer = new ReportingTripleHandler(new RepositoryWriter(connection));
//
//                runner.extract(reader, writer);
//
//                retVal = QueryResults.asModel(connection.getStatements(null, null, null));
//            }
//            finally {
//                if (connection!=null) {
//                    connection.close();
//                }
//                if (writer!=null) {
//                    writer.close();
//                }
//            }
//        }
//        catch (Exception e) {
//            throw new IOException("Error happened while parsing the document; " + baseURI, e);
//        }
//
//        if (retVal!=null) {
//            retVal = this.filterRelevantNodes(retVal, baseURI);
//        }
//
//        return retVal;
//    }
//    private String translateFormat(Format inputFormat) throws IOException
//    {
//        switch (inputFormat) {
//            case RDFA:
//                return RDFa11ExtractorFactory.NAME;
//            case JSONLD:
//                return JSONLDExtractorFactory.NAME;
//            case NTRIPLES:
//                return NTriplesExtractorFactory.NAME;
//            default:
//                throw new IOException("Unsupported importer format detected; " + inputFormat);
//        }
//    }
//}

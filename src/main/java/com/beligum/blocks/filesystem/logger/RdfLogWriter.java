package com.beligum.blocks.filesystem.logger;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.ifaces.BlocksResource;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontology.factories.Classes;
import com.beligum.blocks.rdf.ontology.factories.Terms;
import com.beligum.blocks.utils.RdfTools;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by bram on 6/10/16.
 */
public class RdfLogWriter extends AbstractHdfsLogWriter
{
    //-----CONSTANTS-----
    private static final ValueFactory RDF_FACTORY = SimpleValueFactory.getInstance();
    private static DatatypeFactory DATATYPE_FACTORY;
    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        }
        catch (DatatypeConfigurationException e) {
            Logger.error("Error while initializing the default DATATYPE_FACTORY, this shouldn't happen", e);
        }
    }

    //-----VARIABLES-----
    private RDFWriter rdfWriter;

    //-----CONSTRUCTORS-----
    public RdfLogWriter(BlocksResource blocksResource) throws IOException
    {
        super(blocksResource);

        this.rdfWriter = Rio.createWriter(RDFFormat.NTRIPLES, this.logWriter);
        this.rdfWriter.startRDF();
    }

    //-----PUBLIC METHODS-----
    @Override
    public void writeLogEntry(Entry logEntry) throws IOException
    {
        if (!(logEntry instanceof PageLogEntry)) {
            throw new IOException("Argument should be instance of " + PageLogEntry.class.getCanonicalName() + "; " + logEntry);
        }
        PageLogEntry pageEntry = (PageLogEntry) logEntry;

        //Note: external IRIs always need to be absolute
        IRI entryId = this.toIRI(RdfTools.createAbsoluteResourceId(Classes.LogEntry));

        String creatorFirstName = pageEntry.getCreator().getFirstName();
        if (creatorFirstName==null) {
            creatorFirstName = "";
        }
        String creatorLastName = pageEntry.getCreator().getLastName();
        if (creatorLastName==null) {
            creatorLastName = "";
        }
        String creatorName = (creatorFirstName+" "+creatorLastName).trim();
        if (StringUtils.isEmpty(creatorName)) {
            creatorName = "anonymous";
        }

        //this heading matches the filenames of the HISTORY folder entries
        String timestampStr = BlocksResource.FOLDER_TIMESTAMP_FORMAT.format(pageEntry.getUTCTimestamp());
        String startComment = "----- " + timestampStr + " -----";
        rdfWriter.handleComment(startComment);

        this.logStatement(entryId, RDF.TYPE, Classes.LogEntry);
        this.logStatement(entryId, Terms.type, pageEntry.getAction().name());
        this.logStatement(entryId, Terms.subject, pageEntry.getPage().getPublicAbsoluteAddress());
        this.logStatement(entryId, Terms.title, "Log entry "+timestampStr+" for page "+pageEntry.getPage().getPublicAbsoluteAddress()+"", Locale.ENGLISH);
        this.logStatement(entryId, Terms.description, "Page " + pageEntry.getPage().getPublicAbsoluteAddress() + " was " + pageEntry.getAction().getVerb() + " by " + creatorName + " on " +
                                                      ZonedDateTime.ofInstant(pageEntry.getUTCTimestamp(), BlocksResource.FOLDER_TIMESTAMP_TIMEZONE).format(DateTimeFormatter.RFC_1123_DATE_TIME), Locale.ENGLISH);
        this.logStatement(entryId, Terms.createdAt, pageEntry.getUTCTimestamp());
        this.logStatement(entryId, Terms.createdBy, RdfTools.createAbsoluteResourceId(Classes.Person, "" + pageEntry.getCreator().getId()));

        //length is always > 0
        String[] software = this.buildSoftwareId();
        this.logStatement(entryId, Terms.software, software[0]);
        if (software.length>1) {
            this.logStatement(entryId, Terms.softwareVersion, software[1]);
        }
    }
    @Override
    public void close() throws IOException
    {
        this.rdfWriter.endRDF();
        super.close();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void logStatement(IRI subject, RdfProperty predicate, Instant object)
    {
        this.rdfWriter.handleStatement(RDF_FACTORY.createStatement(subject, this.toIRI(predicate), this.toLiteral(object)));
    }
    private void logStatement(IRI subject, RdfProperty predicate, String object)
    {
        this.rdfWriter.handleStatement(RDF_FACTORY.createStatement(subject, this.toIRI(predicate), RDF_FACTORY.createLiteral(object)));
    }
    private void logStatement(IRI subject, RdfProperty predicate, String object, Locale language)
    {
        this.rdfWriter.handleStatement(RDF_FACTORY.createStatement(subject, this.toIRI(predicate), RDF_FACTORY.createLiteral(object, language.getLanguage())));
    }
    private void logStatement(IRI subject, RdfProperty predicate, URI object)
    {
        this.rdfWriter.handleStatement(RDF_FACTORY.createStatement(subject, this.toIRI(predicate), this.toIRI(object)));
    }
    private void logStatement(IRI subject, IRI predicate, RdfClass object)
    {
        this.rdfWriter.handleStatement(RDF_FACTORY.createStatement(subject, predicate, this.toIRI(object)));
    }
    private IRI toIRI(RdfClass rdfClass)
    {
        //Note: external IRIs always need to be absolute
        return this.toIRI(rdfClass.getFullName());
    }
    private IRI toIRI(URI uri)
    {
        return RDF_FACTORY.createIRI(uri.toString());
    }
    private Literal toLiteral(Instant instant)
    {
        //we need custom conversion to log everything in UTC format
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeZone(TimeZone.getTimeZone(BlocksResource.FOLDER_TIMESTAMP_TIMEZONE));
        c.setTime(Date.from(instant));
        return RDF_FACTORY.createLiteral(DATATYPE_FACTORY.newXMLGregorianCalendar(c));
    }

    //-----INNER CLASSES-----
}

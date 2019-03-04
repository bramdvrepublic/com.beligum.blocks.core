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

package com.beligum.blocks.filesystem.logger;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.InputType;
import com.beligum.blocks.filesystem.ifaces.BlocksResource;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.RdfClassImpl;
import com.beligum.blocks.rdf.RdfPropertyImpl;
import com.beligum.blocks.rdf.ontologies.Local;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.beligum.blocks.utils.RdfTools;
import gen.com.beligum.blocks.core.messages.blocks.ontology;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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
    private static final String ANONYMOUS_NAME = "anonymous";
    private static final String ANONYMOUS_USERNAME = ANONYMOUS_NAME;

    //instead of adding the LogEntry to the general ontology class, we decided to put it here,
    // because it has little to do with the mapping of the general/public ontologies,
    // it simply uses our same API.

    private static final RdfClass LogEntry = new RdfClassImpl("LogEntry",
                                                              Local.INSTANCE,
                                                              ontology.Entries.classTitle_LogEntry,
                                                              ontology.Entries.classLabel_LogEntry,
                                                              new URI[] {},
                                                              false,
                                                              null);

    private static final RdfProperty type = new RdfPropertyImpl("type",
                                                                Local.INSTANCE,
                                                                ontology.Entries.propertyTitle_type,
                                                                ontology.Entries.propertyLabel_type,
                                                                RDF.LANGSTRING,
                                                                InputType.InlineEditor,
                                                                null,
                                                                new URI[] {},
                                                                false);

    private static final RdfProperty subject = new RdfPropertyImpl("subject",
                                                                   Local.INSTANCE,
                                                                   ontology.Entries.propertyTitle_subject,
                                                                   ontology.Entries.propertyLabel_subject,
                                                                   XSD.ANY_URI,
                                                                   InputType.Resource,
                                                                   null,
                                                                   new URI[] {},
                                                                   false);

    private static final RdfProperty title = new RdfPropertyImpl("title",
                                                                 Local.INSTANCE,
                                                                 ontology.Entries.propertyTitle_title,
                                                                 ontology.Entries.propertyLabel_title,
                                                                 RDF.LANGSTRING,
                                                                 InputType.InlineEditor,
                                                                 null,
                                                                 new URI[] {},
                                                                 false);

    private static final RdfProperty description = new RdfPropertyImpl("description",
                                                                       Local.INSTANCE,
                                                                       ontology.Entries.propertyTitle_description,
                                                                       ontology.Entries.propertyLabel_description,
                                                                       RDF.LANGSTRING,
                                                                       InputType.Editor,
                                                                       null,
                                                                       new URI[] {},
                                                                       false);

    private static final RdfProperty createdAt = new RdfPropertyImpl("createdAt",
                                                                     Local.INSTANCE,
                                                                     ontology.Entries.propertyTitle_createdAt,
                                                                     ontology.Entries.propertyLabel_createdAt,
                                                                     XSD.DATE_TIME,
                                                                     InputType.DateTime,
                                                                     null,
                                                                     new URI[] {},
                                                                     true);

    private static final RdfProperty username = new RdfPropertyImpl("username",
                                                                    Local.INSTANCE,
                                                                    ontology.Entries.propertyTitle_username,
                                                                    ontology.Entries.propertyLabel_username,
                                                                    XSD.STRING,
                                                                    InputType.InlineEditor,
                                                                    null,
                                                                    new URI[] {},
                                                                    false);

    private static final RdfProperty software = new RdfPropertyImpl("software",
                                                                    Local.INSTANCE,
                                                                    ontology.Entries.propertyTitle_software,
                                                                    ontology.Entries.propertyLabel_software,
                                                                    RDF.LANGSTRING,
                                                                    InputType.InlineEditor,
                                                                    null,
                                                                    new URI[] {},
                                                                    false);

    private static final RdfProperty softwareVersion = new RdfPropertyImpl("softwareVersion",
                                                                           Local.INSTANCE,
                                                                           ontology.Entries.propertyTitle_softwareVersion,
                                                                           ontology.Entries.propertyLabel_softwareVersion,
                                                                           RDF.LANGSTRING,
                                                                           InputType.InlineEditor,
                                                                           null,
                                                                           new URI[] {},
                                                                           false);

    static {
        LogEntry.addProperties(RdfLogWriter.type,
                               RdfLogWriter.subject,
                               RdfLogWriter.title,
                               RdfLogWriter.description,
                               RdfLogWriter.createdAt,
                               RdfLogWriter.username,
                               RdfLogWriter.software,
                               RdfLogWriter.softwareVersion);

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
        IRI entryId = this.toIRI(RdfTools.createAbsoluteResourceId(RdfLogWriter.LogEntry));

        String creatorFirstName = pageEntry.getCreator() == null ? null : pageEntry.getCreator().getFirstName();
        if (creatorFirstName == null) {
            creatorFirstName = "";
        }
        String creatorLastName = pageEntry.getCreator() == null ? null : pageEntry.getCreator().getLastName();
        if (creatorLastName == null) {
            creatorLastName = "";
        }
        String creatorName = (creatorFirstName + " " + creatorLastName).trim();
        if (StringUtils.isEmpty(creatorName)) {
            creatorName = ANONYMOUS_NAME;
        }

        //this heading matches the filenames of the HISTORY folder entries
        String timestampStr = BlocksResource.FOLDER_TIMESTAMP_FORMAT.format(pageEntry.getUTCTimestamp());
        String startComment = "----- " + timestampStr + " -----";
        rdfWriter.handleComment(startComment);

        this.logStatement(entryId, org.eclipse.rdf4j.model.vocabulary.RDF.TYPE, RdfLogWriter.LogEntry);
        this.logStatement(entryId, RdfLogWriter.type, pageEntry.getAction().name());
        this.logStatement(entryId, RdfLogWriter.subject, pageEntry.getPage().getPublicAbsoluteAddress());
        this.logStatement(entryId, RdfLogWriter.title, "Log entry " + timestampStr + " for page " + pageEntry.getPage().getPublicAbsoluteAddress() + "", Locale.ENGLISH);
        this.logStatement(entryId, RdfLogWriter.description, "Page " + pageEntry.getPage().getPublicAbsoluteAddress() + " was " + pageEntry.getAction().getVerb() + " by " + creatorName + " on " +
                                                             ZonedDateTime.ofInstant(pageEntry.getUTCTimestamp(), BlocksResource.FOLDER_TIMESTAMP_TIMEZONE)
                                                                          .format(DateTimeFormatter.RFC_1123_DATE_TIME),
                          Locale.ENGLISH);
        this.logStatement(entryId, RdfLogWriter.createdAt, pageEntry.getUTCTimestamp());

        //This is legacy code to show this URI was logged in earlier versions of Stralo,
        //but was replaced by the more uniform 'username' property during the ontology cleanup.
        //See https://github.com/republic-of-reinvention/com.stralo.framework/issues/13
        //this.logStatement(entryId, Terms.createdBy, RdfTools.createAbsoluteResourceId(Classes.Person, "" + pageEntry.getCreator().getId()));
        String username = pageEntry.getCreator() != null && pageEntry.getCreator().getSubject() != null ? pageEntry.getCreator().getSubject().getPrincipal() : ANONYMOUS_USERNAME;
        this.logStatement(entryId, RdfLogWriter.username, username);

        //length is always > 0
        String[] software = this.buildSoftwareId();
        this.logStatement(entryId, RdfLogWriter.software, software[0]);
        if (software.length > 1) {
            this.logStatement(entryId, RdfLogWriter.softwareVersion, software[1]);
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

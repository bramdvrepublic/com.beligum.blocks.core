package com.beligum.blocks.fs.logger;

import com.beligum.blocks.fs.ifaces.ResourcePath;
import com.beligum.blocks.rdf.ontology.factories.Classes;
import com.beligum.blocks.utils.RdfTools;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.ntriples.NTriplesWriter;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by bram on 6/10/16.
 */
public class RdfLogWriter extends AbstractHdfsLogWriter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public RdfLogWriter(ResourcePath resourcePath) throws IOException
    {
        super(resourcePath);
    }

    //-----PUBLIC METHODS-----
    @Override
    public void writeLogEntry(Entry logEntry) throws IOException
    {
        if (!(logEntry instanceof PageLogEntry)) {
            throw new IOException("Argument should be instance of "+PageLogEntry.class.getCanonicalName()+"; "+logEntry);
        }
        PageLogEntry rdfEntry = (PageLogEntry) logEntry;

        RDFWriter rdfWriter = new NTriplesWriter(this.logWriter);

        rdfWriter.handleComment("----- New log entry -----");

        SimpleValueFactory rdfFactory = SimpleValueFactory.getInstance();

        //String person = rdfFactory.createIRI(logEntry.getCreator().)

        String swVersion = buildSoftwareVersion();
        String creatorURI = RdfTools.createRelativeResourceId(Classes.Person, "" + rdfEntry.getCreator().getId()).toString();
        //TODO: add human readable creator details?
        //stamp
        //action
        //typeof


        Iterator<Statement> modelIter = rdfEntry.getModel().iterator();
        while (modelIter.hasNext()) {
            rdfWriter.handleStatement(modelIter.next());
        }

        rdfWriter.handleComment("-------------------------");
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
}

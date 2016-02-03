package com.beligum.blocks.rdf.importers;

import com.beligum.blocks.rdf.ifaces.Importer;
import org.apache.jena.rdf.model.*;

import java.net.URI;

/**
 * Created by bram on 1/23/16.
 */
public abstract class AbstractImporter implements Importer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected final Format inputFormat;

    //-----CONSTRUCTORS-----
    protected AbstractImporter(Format inputFormat)
    {
        this.inputFormat = inputFormat;
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    protected Model filterRelevantNodes(Model model, URI documentBaseUri)
    {
        //select only the relevant triples
        //TODO this is probably not the best way...
        Model filteredModel = ModelFactory.createDefaultModel();
        //Note: check the property (document.toString); it should correspond with the @id in JSON-LD (unverified code)
        StmtIterator selection = model.listStatements(new SimpleSelector(model.createProperty(documentBaseUri.toString()), null, (RDFNode) null));
        while (selection.hasNext()) {
//            Statement stmt = selection.nextStatement();
//            filteredModel.add(stmt.getSubject(), stmt.getPredicate(), stmt.getObject());
            filteredModel.add(selection.nextStatement());
        }

        return filteredModel;
    }

    //-----PRIVATE METHODS-----

}
package com.beligum.blocks.rdf.importers;

import com.beligum.blocks.rdf.ifaces.Importer;
import org.apache.any23.vocab.XHTML;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

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
//        //select only the relevant triples
//        //TODO this is probably not the best way...
//        Model filteredModel = ModelFactory.createDefaultModel();
//        //Note: check the property (document.toString); it should correspond with the @id in JSON-LD (unverified code)
//        StmtIterator selection = model.listStatements(new SimpleSelector(model.createProperty(documentBaseUri.toString()), null, (RDFNode) null));
//        while (selection.hasNext()) {
////            Statement stmt = selection.nextStatement();
////            filteredModel.add(stmt.getSubject(), stmt.getPredicate(), stmt.getObject());
//            filteredModel.add(selection.nextStatement());
//        }
//
//        return filteredModel;

        final boolean IGNORE_STYLESHEETS = true;
        final boolean IGNORE_FAVICON = true;
        final boolean DOCBASE_ONLY = false;

        final Property XHTML_ICON = model.createProperty(XHTML.getInstance().NS, "icon");
        StmtIterator allStmts = model.listStatements();
        String documentBaseUriStr = documentBaseUri.toString();
        while (allStmts.hasNext()) {
            Statement stmt = allStmts.nextStatement();
            //remove all the XHTML stylesheets predicates from the model
            if (IGNORE_STYLESHEETS && stmt.getPredicate().getURI().equals(XHTML.getInstance().stylesheet.toString())) {
                allStmts.remove();
            }
            //removes all favicon statements. Note that this last check isn't waterproof (we can use any name for our favicons), but it works 99% of the time
            if (IGNORE_FAVICON && stmt.getPredicate().getURI().equals(XHTML_ICON.getURI()) && stmt.getObject().toString().contains("favicon")) {
                allStmts.remove();
            }
            //remove all non-documentBaseUri subjects from the model
            if (DOCBASE_ONLY && !stmt.getSubject().getURI().equals(documentBaseUriStr)) {
                allStmts.remove();
            }
        }

        //DEBUG
        //RDFDataMgr.write(System.out, model, RDFFormat.NTRIPLES);

        return model;
    }

    //-----PRIVATE METHODS-----

}

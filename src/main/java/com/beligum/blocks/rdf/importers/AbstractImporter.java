package com.beligum.blocks.rdf.importers;

import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.Importer;
import org.apache.any23.vocab.XHTML;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.ValueFactoryImpl;

import java.net.URI;
import java.util.Iterator;

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

        final org.openrdf.model.URI XHTML_ICON = ValueFactoryImpl.getInstance().createURI(XHTML.getInstance().NS, "icon");;
        String documentBaseUriStr = documentBaseUri.toString();
        Iterator<Statement> iter = model.iterator();
        while (iter.hasNext()) {
            Statement stmt = iter.next();

            //remove all the XHTML stylesheets predicates from the model
            if (IGNORE_STYLESHEETS && stmt.getPredicate().toString().equals(XHTML.getInstance().stylesheet.toString())) {
                iter.remove();
            }
            //removes all favicon statements. Note that this last check isn't waterproof (we can use any name for our favicons), but it works 99% of the time
            if (IGNORE_FAVICON && stmt.getPredicate().equals(XHTML_ICON) && stmt.getObject().toString().contains("favicon")) {
                iter.remove();
            }
            //remove all non-documentBaseUri subjects from the model
            if (DOCBASE_ONLY && !stmt.getSubject().toString().equals(documentBaseUriStr)) {
                iter.remove();
            }
        }

        //DEBUG
        //RDFDataMgr.write(System.out, model, RDFFormat.NTRIPLES);

        return model;
    }

    //-----PRIVATE METHODS-----

}

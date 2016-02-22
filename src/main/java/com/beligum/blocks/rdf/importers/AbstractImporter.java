package com.beligum.blocks.rdf.importers;

import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.Importer;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;

import java.net.URI;
import java.util.Iterator;

/**
 * Created by bram on 1/23/16.
 */
public abstract class AbstractImporter implements Importer
{
    //-----CONSTANTS-----
    //See https://www.w3.org/2011/rdfa-context/xhtml-rdfa-1.1
    public static final String XHTML_NS = "http://www.w3.org/1999/xhtml/vocab#";
    public static final String XHTML_NS_ALTERNATE = XHTML_NS+"alternate";
    public static final String XHTML_NS_ICON = XHTML_NS+"icon";
    public static final String XHTML_NS_STYLESHEET = XHTML_NS+"stylesheet";

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

        String documentBaseUriStr = documentBaseUri.toString();
        Iterator<Statement> iter = model.iterator();
        while (iter.hasNext()) {
            Statement stmt = iter.next();

            //remove all the XHTML stylesheets predicates from the model
            boolean removed = false;
            if (!removed && IGNORE_STYLESHEETS && stmt.getPredicate().toString().equals(XHTML_NS_STYLESHEET)) {
                iter.remove();
                removed = true;
            }
            //removes all favicon statements. Note that this last check isn't waterproof (we can use any name for our favicons), but it works 99% of the time
            if (!removed && IGNORE_FAVICON && stmt.getPredicate().toString().equals(XHTML_NS_ICON) && stmt.getObject().toString().contains("favicon")) {
                iter.remove();
                removed = true;
            }
            //remove all non-documentBaseUri subjects from the model
            if (!removed && DOCBASE_ONLY && !stmt.getSubject().toString().equals(documentBaseUriStr)) {
                iter.remove();
                removed = true;
            }
        }

        //DEBUG
        //RDFDataMgr.write(System.out, model, RDFFormat.NTRIPLES);

        return model;
    }

    //-----PRIVATE METHODS-----

}

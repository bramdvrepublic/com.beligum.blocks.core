package com.beligum.blocks.rdf.importers;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.Importer;
import org.apache.commons.lang.StringUtils;
import org.openrdf.model.*;
import org.openrdf.model.impl.SimpleLiteral;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bram on 1/23/16.
 */
public abstract class AbstractImporter implements Importer
{
    //-----CONSTANTS-----
    //See https://www.w3.org/2011/rdfa-context/xhtml-rdfa-1.1
    public static final String XHTML_NS = "http://www.w3.org/1999/xhtml/vocab#";
    public static final String LOCAL_NS = Settings.instance().getRdfOntologyUri().toString();
    public static final String XHTML_NS_ALTERNATE_NAME = "alternate";
    public static final String XHTML_NS_ALTERNATE = XHTML_NS + XHTML_NS_ALTERNATE_NAME;
    public static final String XHTML_NS_ICON_NAME = "icon";
    public static final String XHTML_NS_ICON = XHTML_NS + XHTML_NS_ICON_NAME;
    public static final String LOCAL_NS_ICON = LOCAL_NS + XHTML_NS_ICON_NAME;
    public static final String XHTML_NS_APPLE_TOUCH_ICON_NAME = "apple-touch-icon";
    public static final String XHTML_NS_APPLE_TOUCH_ICON = XHTML_NS + XHTML_NS_APPLE_TOUCH_ICON_NAME;
    public static final String LOCAL_NS_APPLE_TOUCH_ICON = LOCAL_NS + XHTML_NS_APPLE_TOUCH_ICON_NAME;
    public static final String XHTML_NS_STYLESHEET_NAME = "stylesheet";
    public static final String XHTML_NS_STYLESHEET = XHTML_NS + XHTML_NS_STYLESHEET_NAME;
    public static final String LOCAL_NS_STYLESHEET = LOCAL_NS + XHTML_NS_STYLESHEET_NAME;

    //-----VARIABLES-----
    protected final Format inputFormat;

    //-----CONSTRUCTORS-----
    protected AbstractImporter(Format inputFormat)
    {
        this.inputFormat = inputFormat;
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    protected Model filterRelevantNodes(Model model, URI documentBaseUri) throws IOException
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

        ValueFactory factory = SimpleValueFactory.getInstance();
        String documentBaseUriStr = documentBaseUri.toString();
        Iterator<Statement> iter = model.iterator();
        List<Statement> editedStatements = new ArrayList<>();
        while (iter.hasNext()) {
            Statement stmt = iter.next();

            //!!! Note !!!
            //The double (eg. both XHTML_NS_STYLESHEET and LOCAL_NS_STYLESHEET) checks below are the result of a bug:
            //Because we set the @vocab attribute on the <html> element, all the stylesheet-links are prefixed with that namespace,
            //so they're actually not real XHTML predicates, but changed into eg. http://www.mot.be/ontology/stylesheet
            //Because it's quite a hard fix to implement, we work around it here.

            //remove all the XHTML stylesheets predicates from the model
            boolean removed = false;
            if (!removed && IGNORE_STYLESHEETS && (stmt.getPredicate().toString().equals(XHTML_NS_STYLESHEET) || stmt.getPredicate().toString().equals(LOCAL_NS_STYLESHEET))) {
                iter.remove();
                removed = true;
            }
            //removes all favicon statements. Note that this last check isn't waterproof (we can use any name for our favicons), but it works 99% of the time
            if (!removed && IGNORE_FAVICON &&
                (stmt.getPredicate().toString().equals(XHTML_NS_ICON) || stmt.getPredicate().toString().equals(LOCAL_NS_ICON) ||
                 stmt.getPredicate().toString().equals(XHTML_NS_APPLE_TOUCH_ICON) || stmt.getPredicate().toString().equals(LOCAL_NS_APPLE_TOUCH_ICON)
                ) && stmt.getObject().toString().contains("favicon")) {
                iter.remove();
                removed = true;
            }
            //remove all non-documentBaseUri subjects from the model
            if (!removed && DOCBASE_ONLY && !stmt.getSubject().toString().equals(documentBaseUriStr)) {
                iter.remove();
                removed = true;
            }

            //we're not interested in blank nodes because they don't have a page as subject, so we might as well filter them out
            if (!removed && stmt.getSubject() instanceof BNode) {
                iter.remove();
                removed = true;
            }

            //we'll use this loop to whitespace-trim the values while we're at it
            //Note that this is a bit hacky. The more proper way would be to edit the com.beligum.blocks.rdf.importers.semargl.SesameSink.addPlainLiteral()
            // method, but that class is supposed to be part of the core Sesame library (only copied over to have RDFa support in Sesame4), so I opted to implement it here
            //Also note that this implementation doens't trim the non-plain literal values (like datetime). They are passed over via the @content attribute inside RDFa,
            // so it should be quite clean already. Also because the HTML input stream, coming from JSoup is minified before passed to Sesame.
            // But it can be easily supported (the part where the exception is thrown)
            if (!removed) {
                if (stmt.getObject() instanceof Literal) {
                    Literal literal = (Literal) stmt.getObject();
                    String objectValue = literal.getLabel();
                    String objectValueTrimmed = StringUtils.strip(objectValue);

                    if (!objectValue.equals(objectValueTrimmed)) {
                        //Note: this makes sense: see SimpleLiteral(String label, IRI datatype) constructor code
                        Literal trimmedLiteral = null;
                        if (literal.getDatatype().equals(RDF.LANGSTRING)) {
                            trimmedLiteral = factory.createLiteral(objectValueTrimmed, literal.getLanguage().get());
                        }
                        else if (literal.getDatatype().equals(XMLSchema.STRING)) {
                            trimmedLiteral = factory.createLiteral(objectValueTrimmed, literal.getDatatype());
                        }
                        else {
                            throw new IOException("Encountered unsupported simple literal value, this shouldn't happen; " + literal.getDatatype());
                        }

                        Statement trimmedStmt = null;
                        if (stmt.getContext() == null) {
                            trimmedStmt = factory.createStatement(stmt.getSubject(), stmt.getPredicate(), trimmedLiteral);
                        }
                        else {
                            trimmedStmt = factory.createStatement(stmt.getSubject(), stmt.getPredicate(), trimmedLiteral, stmt.getContext());
                        }
                        //we can't add them directly to the model or we'll have a concurrent mod exception, and since statements don't really have an order,
                        // it's safe to add them afterwards after this loop
                        editedStatements.add(trimmedStmt);
                        iter.remove();
                    }
                }
            }
        }

        if (!editedStatements.isEmpty()) {
            model.addAll(editedStatements);
        }

        //DEBUG
        //RDFDataMgr.write(System.out, model, RDFFormat.NTRIPLES);

        return model;
    }

    //-----PRIVATE METHODS-----

}

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

package com.beligum.blocks.rdf.importers;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.rdf.ifaces.*;
import com.beligum.blocks.rdf.validation.RdfModelImpl;
import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
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
    public static final String LOCAL_NS = Settings.instance().getRdfMainOntologyNamespace().getUri().toString();
    public static final String XHTML_NS_ALTERNATE_NAME = "alternate";
    public static final String XHTML_NS_ALTERNATE = XHTML_NS + XHTML_NS_ALTERNATE_NAME;
    public static final String XHTML_NS_ICON_NAME = "icon";
    public static final String XHTML_NS_ICON = XHTML_NS + XHTML_NS_ICON_NAME;
    public static final String LOCAL_NS_ICON = LOCAL_NS + XHTML_NS_ICON_NAME;
    public static final String LOCAL_NS_MANIFEST = LOCAL_NS + "manifest";
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
    /**
     * Note: this returns a newly created (memory based) model, so all connections with a possible underlying store are cut
     */
    protected Model filterModel(Model model, URI documentBaseUri) throws IOException
    {
        final boolean IGNORE_STYLESHEETS = true;
        final boolean IGNORE_FAVICON = true;
        final boolean DOCBASE_ONLY = false;

        ValueFactory factory = SimpleValueFactory.getInstance();
        String documentBaseUriStr = documentBaseUri.toString();
        Iterator<Statement> iter = model.iterator();
        //Note that the model has a "predictable iteration order", and since we get a ConcurrentModException while altering the model itself,
        // we choose to instance a new one, and add the valid filtered nodes in order to this new one
        Model filteredModel = new LinkedHashModel();
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
                removed = true;
            }
            //removes all favicon statements. Note that this last check isn't waterproof (we can use any name for our favicons), but it works 99% of the time
            if (!removed && IGNORE_FAVICON &&
                stmt.getObject().toString().contains("favicon") &&
                (stmt.getPredicate().toString().equals(XHTML_NS_ICON) || stmt.getPredicate().toString().equals(LOCAL_NS_ICON) ||
                 stmt.getPredicate().toString().equals(XHTML_NS_APPLE_TOUCH_ICON) || stmt.getPredicate().toString().equals(LOCAL_NS_APPLE_TOUCH_ICON) ||
                 stmt.getPredicate().toString().equals(LOCAL_NS_MANIFEST)
                )) {
                removed = true;
            }
            //remove all non-documentBaseUri subjects from the model
            if (!removed && DOCBASE_ONLY && !stmt.getSubject().toString().equals(documentBaseUriStr)) {
                removed = true;
            }
            //we're not interested in blank nodes because they don't have a page as subject, so we might as well filter them out
            if (!removed && stmt.getSubject() instanceof BNode) {
                removed = true;
            }

            //we'll use this loop to whitespace-trim the values while we're at it
            //Note that this is a bit hacky. The more proper way would be to edit the com.beligum.blocks.rdf.importers.semargl.SesameSink.addPlainLiteral()
            // method, but that class is supposed to be part of the core Sesame library (only copied over to have RDFa support in Sesame4), so I opted to implement it here
            //Also note that this implementation doens't trim the non-plain literal values (like datetime). They are passed over via the @content attribute inside RDFa,
            // so it should be quite clean already. Also because the HTML input stream, coming from JSoup is minified before passed to Sesame.
            // But it can be easily supported (the part where the exception is thrown)
            if (!removed) {
                //these will hold possible modified pieces of the statement
                Resource newSubject = null;
                IRI newPredicate = null;
                Value newObject = null;

                //trim all "lang" params from the subject
                //This is an important change:
                // From time to time, the eg. ?lang=en part of an URI seeps through, even though we set every RDFa html tag with a clear @about attribute
                // eg. this is the case as subject of "http://www.w3.org/ns/rdfa#usesVocabulary"
                // Because we don't save the statements of a resource by language (the @lang tag makes sure it is added statement-per-statement for certain datatypes),
                // the subject of the resource needs to be cleared from all lang query params to avoid false doubles while querying the triplestore (eg. using SPARQL).
                // We group by subject-URI and double (or triple, or ..., depends on the amount of languages in the system) entries mess up the counts, iterations, group-by's, etc.
                // That's why we try hard here to avoid these during import.
                if (stmt.getSubject() instanceof IRI) {
                    URI subject = URI.create(stmt.getSubject().stringValue());
                    MultivaluedMap<String, String> queryParams = StringFunctions.getQueryParameters(subject);
                    if (queryParams != null && queryParams.containsKey(I18nFactory.LANG_QUERY_PARAM)) {
                        URI noLangUri = UriBuilder.fromUri(subject).replaceQueryParam(I18nFactory.LANG_QUERY_PARAM).build();
                        newSubject = factory.createIRI(noLangUri.toString());
                    }
                }

                //if the object is a literal, check if it needs to be trimmed
                //BIG NOTE: XSD.anyURI is also an instance of a Literal!!
                if (stmt.getObject() instanceof Literal) {
                    Literal literal = (Literal) stmt.getObject();
                    String objectValue = literal.getLabel();

                    //we like to use and save relative URIs as relative URIs because it means we don't need to
                    //update them if the site domain changes. But the RDFa doc clearly states all relative URIs need
                    //to be converted to absolute URIs: https://www.w3.org/TR/rdfa-core/#s_curieprocessing
                    //However, when dealing with custom html tags (eg. <meta datatype="xsd:anyURI"> tags), this doesn't happen
                    //automatically, so let's do it manually.
                    if (literal.getDatatype().equals(XMLSchema.ANYURI)) {
                        //this is important: blank URIs (and this happens; eg. a tag <meta property="sameAs" datatype="xsd:anyURI" /> will yield a blank value for "sameAs"!)
                        // would get filled in without this check; eg. a "" value would become "http://localhost:8080/en/" by default and this is bad! (eg. for sameAs properties)
                        if (!StringUtils.isBlank(objectValue)) {
                            URI objectUri = URI.create(objectValue);
                            if (!objectUri.isAbsolute()) {
                                objectUri = documentBaseUri.resolve(objectUri);
                            }
                            //it makes sense to convert the data type to IRI as well
                            newObject = factory.createIRI(objectUri.toString());
                        }
                    }
                    //this means it's a 'true' literal -> check if we can trim
                    else {
                        String objectValueTrimmed = StringUtils.strip(objectValue);

                        if (!objectValue.equals(objectValueTrimmed)) {
                            //Note: this makes sense: see SimpleLiteral(String label, IRI datatype) constructor code
                            if (literal.getDatatype().equals(RDF.LANGSTRING)) {
                                newObject = factory.createLiteral(objectValueTrimmed, literal.getLanguage().get());
                            }
                            else if (literal.getDatatype().equals(XMLSchema.STRING)) {
                                newObject = factory.createLiteral(objectValueTrimmed, literal.getDatatype());
                            }
                            else if (literal.getDatatype().equals(RDF.HTML)) {
                                newObject = factory.createLiteral(objectValueTrimmed, literal.getDatatype());
                            }
                            else {
                                throw new IOException("Encountered unsupported simple literal value, this shouldn't happen; " + literal.getDatatype() + " - " + objectValue);
                            }
                        }
                    }
                }

                //After all the filtering above, check if we need to change the statement: remove it from the model and add it again later on
                Statement validStmt = null;
                if (newSubject != null || newObject != null || newPredicate != null) {
                    if (newSubject == null) {
                        newSubject = stmt.getSubject();
                    }
                    if (newPredicate == null) {
                        newPredicate = stmt.getPredicate();
                    }
                    if (newObject == null) {
                        newObject = stmt.getObject();
                    }

                    if (stmt.getContext() == null) {
                        validStmt = factory.createStatement(newSubject, newPredicate, newObject);
                    }
                    else {
                        validStmt = factory.createStatement(newSubject, newPredicate, newObject, stmt.getContext());
                    }
                }
                else {
                    validStmt = stmt;
                }

                filteredModel.add(validStmt);
            }
        }

        // if the settings say so, validate the incoming (filtered) model
        if (Settings.instance().getEnableRdfValidation()) {
            new RdfModelImpl(filteredModel).validate();
        }

        //DEBUG
        //RDFDataMgr.write(System.out, model, RDFFormat.NTRIPLES);

        return filteredModel;
    }

    //-----PRIVATE METHODS-----

}

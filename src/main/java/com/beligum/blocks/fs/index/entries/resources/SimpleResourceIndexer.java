package com.beligum.blocks.fs.index.entries.resources;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.rdf.ontology.factories.Terms;
import org.apache.commons.lang3.StringUtils;
import org.openrdf.model.IRI;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.SimpleValueFactory;

import java.net.URI;
import java.util.Iterator;

/**
 * Created by bram on 5/10/16.
 */
public class SimpleResourceIndexer implements ResourceIndexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private boolean initialized;
    private IRI titleIri;
    private IRI textIri;
    private IRI commentIri;
    private IRI imageIri;

    //-----CONSTRUCTORS-----
    //making it private and using the INSTANCE method instead to have control over the static initialization order...
    public SimpleResourceIndexer()
    {
        this.initialized = false;
    }

    //-----PUBLIC METHODS-----
    /**
     * Note: made synchronized to make it thread safe because of the single instance above
     */
    @Override
    public synchronized IndexedResource index(Model model)
    {
        //Need to do this here or we'll run into trouble while initializing static members
        this.assertInit();

        String title = null;
        String text = null;
        String comment = null;
        URI image = null;

        Iterator<Statement> iter = model.iterator();
        while (iter.hasNext()) {
            Statement stmt = iter.next();

            if (stmt.getPredicate().equals(titleIri)) {
                if (title == null) {
                    title = stmt.getObject().stringValue();
                }
                else {
                    Logger.warn("Double " + titleIri + " predicate entry found for " + stmt.getSubject() + "; only using first.");
                }
            }
            else if (stmt.getPredicate().equals(textIri)) {
                if (text == null) {
                    text = stmt.getObject().stringValue();
                }
                else {
                    Logger.warn("Double " + textIri + " predicate entry found for " + stmt.getSubject() + "; only using first.");
                }
            }
            else if (stmt.getPredicate().equals(commentIri)) {
                if (comment == null) {
                    comment = stmt.getObject().stringValue();
                }
                else {
                    Logger.warn("Double " + commentIri + " predicate entry found for " + stmt.getSubject() + "; only using first.");
                }
            }
            else if (stmt.getPredicate().equals(imageIri)) {
                if (image == null) {
                    image = URI.create(stmt.getObject().stringValue());
                }
                else {
                    Logger.warn("Double " + imageIri + " predicate entry found for " + stmt.getSubject() + "; only using first.");
                }
            }
        }

        //a comment property has precedence over a general 'text', but only if it's not empty
        if (StringUtils.isEmpty(comment)) {
            comment = text;
        }

        return new DefaultIndexedResource(title, comment, image);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void assertInit()
    {
        if (!this.initialized) {
            this.titleIri = SimpleValueFactory.getInstance().createIRI(Terms.title.getFullName().toString());
            this.textIri = SimpleValueFactory.getInstance().createIRI(Terms.text.getFullName().toString());
            this.commentIri = SimpleValueFactory.getInstance().createIRI(Terms.comment.getFullName().toString());
            this.imageIri = SimpleValueFactory.getInstance().createIRI(Terms.image.getFullName().toString());

            this.initialized = true;
        }
    }
}

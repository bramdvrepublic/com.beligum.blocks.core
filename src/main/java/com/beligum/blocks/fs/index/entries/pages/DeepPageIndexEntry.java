package com.beligum.blocks.fs.index.entries.pages;

import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.util.BytesRef;
import org.openrdf.model.*;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bram on 2/13/16.
 */
public class DeepPageIndexEntry extends SimplePageIndexEntry
{
    //-----CONSTANTS-----
    private static final char DOUBLE_FIELD_JOINER = '\n';

    //-----VARIABLES-----
    private Document luceneDoc;

    //-----CONSTRUCTORS-----
    public DeepPageIndexEntry(Page page) throws IOException
    {
        super(page);

        this.luceneDoc = super.createLuceneDoc();

        Map<String, String> valuesToIndex = new LinkedHashMap<>();
        Model rdfModel = page.readRdfModel();
        Iterator<Statement> stmtIter = rdfModel.iterator();
        while (stmtIter.hasNext()) {
            Statement stmt = stmtIter.next();

            URI predicateCurie = RdfFactory.fullToCurie(URI.create(stmt.getPredicate().toString()));
            if (predicateCurie!=null) {
                RdfProperty predicate = (RdfProperty) RdfFactory.getClassForResourceType(predicateCurie);
                if (predicate != null) {
                    Value obj = stmt.getObject();

                    String value = null;
                    if (obj instanceof Literal) {
                        Literal objLiteral = (Literal) obj;
                        value = objLiteral.getLabel();
                    }
                    else if (obj instanceof IRI) {
                        RdfQueryEndpoint endpoint = predicate.getDataType().getEndpoint();
                        if (endpoint!=null) {
                            ResourceInfo resourceValue = endpoint.getResource(predicate, URI.create(obj.stringValue()), page.getLanguage());
                            if (resourceValue != null) {
                                value = resourceValue.getLabel();
                            }
                        }
                    }

                    if (value!=null) {
                        String field = predicate.getCurieName().toString();

                        //Lucene: only one value is allowed per field
                        if (valuesToIndex.containsKey(field)) {
                            value = valuesToIndex.get(field)+DOUBLE_FIELD_JOINER+value;
                        }
                        valuesToIndex.put(field, value);
                    }
                }
            }
        }

        for (Map.Entry<String, String> e : valuesToIndex.entrySet()) {
            this.luceneDoc.add(new SortedDocValuesField(e.getKey(), new BytesRef(e.getValue())));
        }

    }

    //-----STATIC METHODS-----

    //-----PUBLIC METHODS-----
    @Override
    public Document createLuceneDoc() throws IOException
    {
        return this.luceneDoc;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
}

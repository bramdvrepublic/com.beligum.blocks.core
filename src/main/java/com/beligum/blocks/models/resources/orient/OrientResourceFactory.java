package com.beligum.blocks.models.resources.orient;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.controllers.OrientResourceController;
import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.beligum.blocks.models.resources.interfaces.ResourceFactory;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import java.util.Iterator;
import java.util.Locale;

/**
 * Created by wouter on 13/05/15.
 */
public class OrientResourceFactory implements ResourceFactory
{

    public static final String DEFAULT_CLASS = "DefaultResource";
    public static final String LOCALIZED_CLASS = "LocalizedResource";
    public final static String TYPE_FIELD = "@rdftype";
    public static final String LOCALIZED = "localized";




    public static OrientResourceFactory instance;

    private OrientResourceFactory() {


    }

    public static OrientResourceFactory instance() {
        if (OrientResourceFactory.instance == null) {
            OrientResourceFactory.instance = new OrientResourceFactory();
        }
        return OrientResourceFactory.instance;
    }


    public Resource createResource(String id, String rdfType, Locale language)
    {
        Resource retVal = null;
        ODocument defaultVertex = getVertexWithBlockId(id);
        if (defaultVertex == null) {
            defaultVertex = createDefaultVertex(id, rdfType);
        } else {
            Logger.debug("Use found document");
        }
        if (language == null || language.equals(Locale.ROOT)) language = BlocksConfig.instance().getDefaultLanguage();
        ODocument localized = getOrCreateLocalizedVertex(defaultVertex, language);
        retVal = new OrientResource(defaultVertex, localized);

        return retVal;
    }

    public Resource createResource(String id, Locale language)
    {
        return createResource(id, null, language);
    }


    public Resource asResource(ODocument vertex, Locale language)
    {
        ODocument defaultResource = vertex;
        Resource retVal = null;
        if (vertex.getClassName() !=null && vertex.getClassName().equals(OrientResourceFactory.DEFAULT_CLASS)) {
            ODocument localized = getOrCreateLocalizedVertex(defaultResource, language);
            retVal = new OrientResource(defaultResource, localized);
        } else {
            retVal = new OrientResource(defaultResource, null);
        }
        return retVal;
    }


    @Override
    public Node asNode(Object value, Locale language)
    {
        Node retVal = null;
        if (value instanceof ODocument) {
            retVal = this.asResource((ODocument)value, language);
        } else {
            retVal = new OrientNode(value, language);
        }

        return retVal;
    }

    public Resource getResourceWithBlockId(String id, Locale language)
    {
        Resource retVal = null;
        ODocument defaultVertex = getVertexWithBlockId(id);
        if (defaultVertex != null) {
            ODocument localized = getOrCreateLocalizedVertex(defaultVertex, language);
            retVal = new OrientResource(defaultVertex, localized);
        }
        return retVal;
    }

    public void save(Resource resource)
    {
        ((ODocument)resource.getValue()).save();
    }


    public ODocument getOrCreateLocalizedVertex(ODocument defaultVertex, Locale language) {
        ODocument retVal = getLocalizedVertexForDefault(defaultVertex, language);
        if (retVal == null) {
            // If resource does not yet exist, create it
            retVal = createLocalizedVertex(defaultVertex, language);
        }
        return retVal;
    }

    public ODocument createLocalizedVertex(ODocument defaultVertex, Locale language) {
        ODocument retVal = new ODocument(OrientResourceFactory.LOCALIZED_CLASS);
        retVal.field(ParserConstants.JSONLD_LANGUAGE, language);
        defaultVertex.field(localizedVersion(language.getLanguage()), retVal);
        return retVal;
    }


    public ODocument createDefaultVertex(String id, String rdfType) {
        if (id == null) throw new NullPointerException();

        ODocument vertex = new ODocument(OrientResourceFactory.DEFAULT_CLASS);
        if (rdfType != null) vertex.field(ParserConstants.JSONLD_TYPE, rdfType);
        vertex.field(ParserConstants.JSONLD_ID, id);
        return vertex;
    }


    // ---------------- PRIVATE METHODS -------------------------------

    private ODocument getVertexWithBlockId(String id)
    {
        ODocument retVal = null;
        Iterable<ODocument> docs = OrientResourceController.instance().getDatabase().command(new OSQLSynchQuery<ODocument>("select FROM " + OrientResourceFactory.DEFAULT_CLASS + " WHERE " + ParserConstants.JSONLD_ID + " = '" + id)).execute();
        if (docs != null) {
            Iterator<ODocument> iterator = docs.iterator();
            if (iterator.hasNext()) {
                retVal = iterator.next();
            }
        }
        return retVal;
    }



    protected ODocument getLocalizedVertexForDefault(ODocument defaultVertex, Locale language) {
        ODocument retVal = defaultVertex.field(localizedVersion(language.getLanguage()));

        return retVal;
    }




    private String localizedVersion(String language) {
        return OrientResourceFactory.LOCALIZED + "_" + language;
    }


}

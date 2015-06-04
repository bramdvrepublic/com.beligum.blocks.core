package com.beligum.blocks.models.resources.orient;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.beligum.blocks.models.resources.interfaces.ResourceController;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import java.net.URI;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by wouter on 13/05/15.
 */
public class OrientResourceController implements ResourceController
{

    public static final String DEFAULT_CLASS = "DefaultResource";
    public static final String LOCALIZED_CLASS = "LocalizedResource";
    public final static String TYPE_FIELD = "@rdftype";
    public static final String LOCALIZED = "localized";

    public static final String CREATED_BY = "createdBy";
    public static final String UPDATED_BY = "updatedBy";
    public static final String CREATED_AT = "createdAt";
    public static final String UPDATED_AT = "updatedAt";


    public static OrientResourceController instance;

    private OrientResourceController() {


    }

    public static OrientResourceController instance() {
        if (OrientResourceController.instance == null) {
            OrientResourceController.instance = new OrientResourceController();
        }
        return OrientResourceController.instance;
    }

    @Override
    public Resource createResource(URI id, URI rdfType, Locale language)
    {
        Resource retVal = null;
        ODocument defaultVertex = getVertexWithBlockId(id.toString());
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

    @Override
    public Resource createResource(URI id, Locale language)
    {
        return createResource(id, null, language);
    }


    public Resource asResource(ODocument vertex, Locale language)
    {
        ODocument defaultResource = vertex;
        Resource retVal = null;
        if (vertex.getClassName() !=null && vertex.getClassName().equals(OrientResourceController.DEFAULT_CLASS)) {
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

    @Override
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
        ODocument retVal = new ODocument(OrientResourceController.LOCALIZED_CLASS);
        retVal.field(ParserConstants.JSONLD_LANGUAGE, language);
        defaultVertex.field(localizedVersionField(language.getLanguage()), retVal);
        return retVal;
    }


    public ODocument createDefaultVertex(URI id, URI rdfType) {
        if (id == null) throw new NullPointerException();

        ODocument vertex = new ODocument(OrientResourceController.DEFAULT_CLASS);
        if (rdfType != null) vertex.field(ParserConstants.JSONLD_TYPE, rdfType);
        vertex.field(ParserConstants.JSONLD_ID, id.toString());
        return vertex;
    }


    // ---------------- PRIVATE METHODS -------------------------------

    private ODocument getVertexWithBlockId(String id)
    {
        ODocument retVal = null;
        Iterable<ODocument> docs = com.beligum.blocks.controllers.OrientResourceController
                        .instance().getDatabase().command(new OSQLSynchQuery<ODocument>("select FROM " + OrientResourceController.DEFAULT_CLASS + " WHERE " + ParserConstants.JSONLD_ID + " LIKE '%" + id +"'")).execute();
        if (docs != null) {
            Iterator<ODocument> iterator = docs.iterator();
            if (iterator.hasNext()) {
                retVal = iterator.next();
            }
        }
        return retVal;
    }



    protected ODocument getLocalizedVertexForDefault(ODocument defaultVertex, Locale language) {
        ODocument retVal = defaultVertex.field(localizedVersionField(language.getLanguage()));

        return retVal;
    }



    /*
    * Fieldname of the localized version of the resource
    * */
    private String localizedVersionField(String language) {
        return OrientResourceController.LOCALIZED + "_" + language;
    }


}

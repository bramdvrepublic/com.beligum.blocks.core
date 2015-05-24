package com.beligum.blocks.models.jsonld.interfaces;

import com.beligum.blocks.models.jsonld.OrientNode;
import com.beligum.blocks.models.jsonld.interfaces.Resource;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 13/05/15.
 */
public interface ResourceFactory
{
    public ODatabaseDocumentTx getGraph();

    public Resource createResource(String id, String rdfType, Locale language);

    public Resource getResourceWithBlockId(String id, Locale language);

    public Resource asResource(ODocument vertex, Locale language);

    public Node asNode(Boolean value, Locale language);

    public Node asNode(String value, Locale language);

    public Node asNode(Integer value, Locale language);

    public Node asNode(Long value, Locale language);

    public Node asNode(Double value, Locale language);

    public Node asNode(List value, Locale language);

    public Node asNode(Object value, Locale language);

    public String toJson(Resource resource);


}

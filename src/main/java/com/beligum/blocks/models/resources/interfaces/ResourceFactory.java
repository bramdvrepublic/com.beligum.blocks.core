package com.beligum.blocks.models.resources.interfaces;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.util.Locale;

/**
 * Created by wouter on 13/05/15.
 */
public interface ResourceFactory
{
    public Resource createResource(String id, String rdfType, Locale language);

    public Node asNode(Object value, Locale language);

}

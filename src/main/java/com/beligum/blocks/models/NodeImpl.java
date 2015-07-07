package com.beligum.blocks.models;

import com.beligum.blocks.controllers.PersistenceControllerImpl;
import com.beligum.blocks.controllers.interfaces.PersistenceController;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.ResourceFactory;

import java.util.Locale;

/**
 * Created by wouter on 22/06/15.
 */
public class NodeImpl extends AbstractNode
{

    public NodeImpl(Object value, Locale lang) {
        super(value, lang);
    }

    @Override
    public ResourceFactory getFactory()
    {
        return ResourceFactoryImpl.instance();
    }
}

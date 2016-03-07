package com.beligum.blocks.rdf.ontology;

import com.beligum.base.models.AbstractJsonObject;
import com.beligum.blocks.rdf.ifaces.RdfResource;

/**
 * Created by bram on 3/3/16.
 */
public abstract class AbstractRdfResourceImpl extends AbstractJsonObject implements RdfResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private boolean isPublic;

    //-----CONSTRUCTORS-----
    protected AbstractRdfResourceImpl(boolean isPublic)
    {
        this.isPublic = isPublic;
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean isPublic()
    {
        return isPublic;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

package com.beligum.blocks.core.models;

import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.ifaces.Identifiable;

/**
 * Created by bas on 07.10.14.
 * Inteface representing objects having a unique identifier
 */
public class IdentifiableObject implements Identifiable
{
    //string representing the unique id of this object
    protected final ID id;

    /**
     * Constructor taking a unique id.
     * @param id id for this object
     */
    public IdentifiableObject(ID id)
    {
        this.id = id;
    }


    public ID getId()
    {
        return id;
    }
}

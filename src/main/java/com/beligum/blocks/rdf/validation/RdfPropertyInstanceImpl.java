package com.beligum.blocks.rdf.validation;

import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.validation.ifaces.RdfClassInstance;
import com.beligum.blocks.rdf.validation.ifaces.RdfPropertyInstance;
import org.eclipse.rdf4j.model.Value;

/**
 * Created by bram on May 21, 2019
 */
public class RdfPropertyInstanceImpl implements RdfPropertyInstance
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final RdfClassInstance context;
    private RdfProperty type;
    private Value value;

    //-----CONSTRUCTORS-----
    public RdfPropertyInstanceImpl(RdfClassInstance context, RdfProperty type, Value value)
    {
        this.context = context;
        this.type = type;
        this.value = value;
    }

    //-----PUBLIC METHODS-----
    @Override
    public RdfClassInstance getContext()
    {
        return context;
    }
    @Override
    public RdfProperty getType()
    {
        return type;
    }
    @Override
    public Value getValue()
    {
        return value;
    }
    @Override
    public void validate()
    {
        if (this.getType().getValidator() != null) {
            this.getType().getValidator().validate(this);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

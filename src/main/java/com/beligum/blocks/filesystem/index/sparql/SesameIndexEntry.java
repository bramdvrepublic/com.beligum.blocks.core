package com.beligum.blocks.filesystem.index.sparql;

import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import org.eclipse.rdf4j.query.BindingSet;

public class SesameIndexEntry implements IndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final BindingSet bindingSet;

    //-----CONSTRUCTORS-----
    public SesameIndexEntry(BindingSet bindingSet)
    {
        this.bindingSet = bindingSet;
    }

    //-----PUBLIC METHODS-----
    public BindingSet getBindingSet()
    {
        return bindingSet;
    }
    @Override
    public String getId()
    {
        return null;
    }
    @Override
    public boolean hasId()
    {
        return false;
    }
    @Override
    public String getLabel()
    {
        return null;
    }
    @Override
    public boolean hasLabel()
    {
        return false;
    }
    @Override
    public String getDescription()
    {
        return null;
    }
    @Override
    public boolean hasDescription()
    {
        return false;
    }
    @Override
    public String getImage()
    {
        return null;
    }
    @Override
    public boolean hasImage()
    {
        return false;
    }
    @Override
    public Iterable<IndexEntryField> getInternalFields()
    {
        return null;
    }
    @Override
    public String getFieldValue(IndexEntryField field)
    {
        return null;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MGMT METHODS-----

    @Override
    public String toString()
    {
        return this.bindingSet.toString();
    }
}

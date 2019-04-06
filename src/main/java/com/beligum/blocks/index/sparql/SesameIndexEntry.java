package com.beligum.blocks.index.sparql;

import com.beligum.blocks.index.ifaces.IndexEntry;
import com.beligum.blocks.index.ifaces.IndexEntryField;
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
    public void setId(String value)
    {
        //NOOP
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
    public void setLabel(String value)
    {
        //NOOP
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
    public void setDescription(String value)
    {
        //NOOP
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
    public void setImage(String value)
    {
        //NOOP
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

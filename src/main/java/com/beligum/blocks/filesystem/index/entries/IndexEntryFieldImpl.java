package com.beligum.blocks.filesystem.index.entries;

import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;

import java.util.Objects;

public abstract class IndexEntryFieldImpl implements IndexEntryField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String name;

    //-----CONSTRUCTORS-----
    public IndexEntryFieldImpl(String name)
    {
        this.name = name;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return name;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return name;
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof IndexEntryFieldImpl)) return false;
        IndexEntryFieldImpl that = (IndexEntryFieldImpl) o;
        return Objects.equals(getName(), that.getName());
    }
    @Override
    public int hashCode()
    {
        return Objects.hash(getName());
    }
}

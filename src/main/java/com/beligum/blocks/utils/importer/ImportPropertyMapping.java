package com.beligum.blocks.utils.importer;

import java.io.Serializable;
import java.net.URI;

/**
 * Created by bram on 4/5/16.
 */
public class ImportPropertyMapping implements Serializable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI rdfPropertyCurieName;
    private String rdfPropertyValue;

    //-----CONSTRUCTORS-----
    public ImportPropertyMapping()
    {
    }
    public ImportPropertyMapping(URI rdfPropertyCurieName, String rdfPropertyValue)
    {
        this.rdfPropertyCurieName = rdfPropertyCurieName;
        this.rdfPropertyValue = rdfPropertyValue;
    }

    //-----PUBLIC METHODS-----
    public URI getRdfPropertyCurieName()
    {
        return rdfPropertyCurieName;
    }
    public String getRdfPropertyValue()
    {
        return rdfPropertyValue;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof ImportPropertyMapping))
            return false;

        ImportPropertyMapping that = (ImportPropertyMapping) o;

        if (getRdfPropertyCurieName() != null ? !getRdfPropertyCurieName().equals(that.getRdfPropertyCurieName()) : that.getRdfPropertyCurieName() != null)
            return false;
        return getRdfPropertyValue() != null ? getRdfPropertyValue().equals(that.getRdfPropertyValue()) : that.getRdfPropertyValue() == null;

    }
    @Override
    public int hashCode()
    {
        int result = getRdfPropertyCurieName() != null ? getRdfPropertyCurieName().hashCode() : 0;
        result = 31 * result + (getRdfPropertyValue() != null ? getRdfPropertyValue().hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "" + rdfPropertyCurieName + " -> '" + rdfPropertyValue + "'";
    }
}

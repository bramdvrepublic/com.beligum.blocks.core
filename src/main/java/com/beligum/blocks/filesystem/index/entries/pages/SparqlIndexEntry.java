package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.openrdf.model.Model;
import org.openrdf.model.impl.SimpleValueFactory;

import java.io.IOException;

/**
 * Created by bram on 2/13/16.
 */
public class SparqlIndexEntry extends AbstractPageIndexEntry implements PageIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Model model;

    //-----CONSTRUCTORS-----
    public SparqlIndexEntry(String publicRelativeAddress, Model model) throws IOException
    {
        super(publicRelativeAddress);

        this.model = model;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getResource()
    {
        return null;
    }
    @Override
    public String getTypeOf()
    {
        return null;
    }
    @Override
    public String getLanguage()
    {
        return null;
    }
    @Override
    public String getCanonicalAddress()
    {
        return null;
    }
    public String getObject(RdfProperty predicate)
    {
        return this.getModelValue(this.model, predicate);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private String getModelValue(Model model, RdfProperty property)
    {
        String retVal = null;

        Model filteredModel = model.filter(null, SimpleValueFactory.getInstance().createIRI(property.getFullName().toString()), null);
        if (!filteredModel.isEmpty()) {
            retVal = filteredModel.iterator().next().getObject().stringValue();
        }

        return retVal;
    }

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "SparqlIndexEntry{" +
               "id='" + id + '\'' +
               '}';
    }
}

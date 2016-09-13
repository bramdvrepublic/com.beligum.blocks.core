package com.beligum.blocks.utils.importer;

import com.beligum.blocks.rdf.ifaces.RdfProperty;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bram on 4/5/16.
 */
public class ImportResource implements Serializable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //a list instead of a map allows us to add double mappings...
    @XmlElement
    private List<ImportPropertyMapping> properties;

    //-----CONSTRUCTORS-----
    public ImportResource()
    {
        this.properties = new ArrayList<>();
    }

    //-----PUBLIC METHODS-----
    public void addRdfProperty(RdfProperty rdfProperty, String rdfPropertyValue)
    {
        this.properties.add(new ImportPropertyMapping(rdfProperty.getCurieName(), rdfPropertyValue));
    }
    public ImportPropertyMapping getMapping(String rdfPropertyCurieName)
    {
        ImportPropertyMapping retVal = null;

        //lazy solution...
        for (ImportPropertyMapping entry : this.properties) {
            if (entry.getRdfPropertyCurieName().equals(rdfPropertyCurieName)) {
                retVal = entry;
                break;
            }
        }

        return retVal;
    }
    public List<ImportPropertyMapping> getRdfProperties()
    {
        return properties;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

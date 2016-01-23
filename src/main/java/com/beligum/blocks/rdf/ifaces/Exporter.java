package com.beligum.blocks.rdf.ifaces;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/23/16.
 */
public interface Exporter
{
    //-----CONSTANTS-----
    enum Format
    {
        JSONLD(Lang.JSONLD)

        ;

        private Lang jenaType;
        Format(Lang jenaType)
        {
            this.jenaType = jenaType;
        }
        public Lang getJenaType()
        {
            return jenaType;
        }
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    void exportModel(Model model, Format outputFormat, OutputStream outputStream) throws IOException, URISyntaxException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

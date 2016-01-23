package com.beligum.blocks.rdf.ifaces;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.any23.extractor.rdfa.RDFa11ExtractorFactory;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/23/16.
 */
public interface Importer
{
    //-----CONSTANTS-----
    enum Format
    {
        RDFA(RDFa11ExtractorFactory.NAME, "RDFA")

        ;

        private String any23Type;
        private String semarglType;
        Format(String any23Type, String semarglType)
        {
            this.any23Type = any23Type;
            this.semarglType = semarglType;
        }
        public String getAny23Type()
        {
            return any23Type;
        }
        public String getSemarglType()
        {
            return semarglType;
        }
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    Model importDocument(Source source, Format inputFormat) throws IOException, URISyntaxException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

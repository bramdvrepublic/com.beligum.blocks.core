package com.beligum.blocks.rdf.exporters;

import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Format;

/**
 * Created by bram on 1/26/16.
 */
public abstract class AbstractExporter implements Exporter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected final Format exportFormat;

    //-----CONSTRUCTORS-----
    protected AbstractExporter(Format exportFormat)
    {
        this.exportFormat = exportFormat;
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

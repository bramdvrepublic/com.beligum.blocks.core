package com.beligum.blocks.rdf.sources;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 1/24/16.
 */
public class HtmlStringSource extends HtmlSource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public HtmlStringSource(URI sourceAddress, String html) throws IOException
    {
        super(sourceAddress);

        this.document = Jsoup.parse(html, this.getSourceAddress().toString());

        this.initDocument();
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

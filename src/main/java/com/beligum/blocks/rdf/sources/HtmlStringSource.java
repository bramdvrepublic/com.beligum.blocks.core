package com.beligum.blocks.rdf.sources;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/24/16.
 */
public class HtmlStringSource extends HtmlSource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public HtmlStringSource(String html, URI baseUri) throws IOException, URISyntaxException
    {
        this(html, baseUri, false);
    }
    public HtmlStringSource(String html, URI baseUri, boolean compact) throws IOException, URISyntaxException
    {
        super(baseUri);

        this.document = Jsoup.parse(html, this.getBaseUri().toString());

        this.initJSoupDocument(compact);
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}

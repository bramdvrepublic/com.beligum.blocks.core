package com.beligum.blocks.rdf;

import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/24/16.
 */
public class HtmlStreamSource extends HtmlSource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public HtmlStreamSource(URI stream, URI baseUri) throws IOException, URISyntaxException
    {
        super(baseUri);

        InputStream is = null;
        try {
            if (stream.getScheme().equals("file")) {
                is = new FileInputStream(new File(stream));
            }
            else if (stream.getScheme().equals("http") || stream.getScheme().equals("https")) {
                is = stream.toURL().openStream();
            }
            else {
                throw new IOException("Unsupported URI scheme; " + stream.getScheme());
            }

            this.document = Jsoup.parse(is, null, this.getBaseUri().toString());
        }
        finally {
            if (is!=null) {
                is.close();
            }
        }

        this.initJSoupDocument();
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}

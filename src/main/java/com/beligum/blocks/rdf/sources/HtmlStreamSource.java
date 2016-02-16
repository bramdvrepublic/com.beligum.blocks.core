package com.beligum.blocks.rdf.sources;

import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by bram on 1/24/16.
 */
public class HtmlStreamSource extends HtmlSource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public HtmlStreamSource(URI sourceAddress, URI stream) throws IOException
    {
        super(sourceAddress);

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

            this.construct(sourceAddress, is);
        }
        finally {
            if (is!=null) {
                is.close();
            }
        }

        this.initDocument();
    }
    public HtmlStreamSource(URI sourceAddress, InputStream stream) throws IOException
    {
        super(sourceAddress);

        this.construct(sourceAddress, stream);
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void construct(URI sourceAddress, InputStream stream) throws IOException
    {
        this.document = Jsoup.parse(stream, null, this.getSourceAddress().toString());
    }
}

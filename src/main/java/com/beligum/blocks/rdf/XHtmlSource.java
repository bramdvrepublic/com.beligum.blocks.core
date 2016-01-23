package com.beligum.blocks.rdf;

import com.beligum.blocks.rdf.ifaces.Source;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/23/16.
 */
public class XHtmlSource implements Source
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected URI document;
    protected URI baseUri;
    protected InputStream inputStream;

    //-----CONSTRUCTORS-----
    public XHtmlSource(URI document, URI baseUri) throws IOException, URISyntaxException
    {
        this.document = document;
        this.baseUri = baseUri;

        this.initInputStream();
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getUri()
    {
        return document;
    }
    @Override
    public URI getBaseUri()
    {
        return baseUri;
    }
    @Override
    public InputStream getInputStream()
    {
        return inputStream;
    }
    @Override
    public void close() throws Exception
    {
        if (this.inputStream!=null) {
            this.inputStream.close();
            this.inputStream = null;
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void initInputStream() throws IOException
    {
        if (this.document.getScheme().equals("file")) {
            this.inputStream = new FileInputStream(new File(document));
        }
        else if (this.document.getScheme().equals("http") || this.document.getScheme().equals("https")) {
            this.inputStream = this.document.toURL().openStream();
        }
        else {
            throw new IOException("Unsupported URI scheme; "+this.document.getScheme());
        }
    }
}

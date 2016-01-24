package com.beligum.blocks.rdf;

import com.beligum.blocks.rdf.ifaces.Source;
import org.jsoup.nodes.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/23/16.
 */
public abstract class HtmlSource implements Source
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected URI baseUri;
    protected Document document;

    //-----CONSTRUCTORS-----
    protected HtmlSource(URI baseUri) throws IOException, URISyntaxException
    {
        this.baseUri = baseUri;
        this.document = null;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getBaseUri()
    {
        return baseUri;
    }
    @Override
    public InputStream openNewInputStream() throws IOException
    {
        return new ByteArrayInputStream(this.document.html().getBytes(this.document.charset()));
    }

    //-----PROTECTED METHODS-----
    protected void initJSoupDocument()
    {
        // Clean the document (doesn't work because it strips the head out)
        //Whitelist whitelist = Whitelist.relaxed();
        //doc = new Cleaner(whitelist).clean(doc);

        // Adjust escape mode
        //doc.outputSettings().escapeMode(Entities.EscapeMode.base);

        //we'll normalize everything to XHTML
        this.document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        //no serialization yet, we might have to apply tweaks later on..
    }

    //-----PRIVATE METHODS-----
}

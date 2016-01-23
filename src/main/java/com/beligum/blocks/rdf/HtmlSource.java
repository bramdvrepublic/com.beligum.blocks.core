package com.beligum.blocks.rdf;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/23/16.
 */
public class HtmlSource extends XHtmlSource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI originalDocument;
    private File tempFile;

    //-----CONSTRUCTORS-----
    public HtmlSource(URI document, URI baseUri) throws IOException, URISyntaxException
    {
        super(document, baseUri);

        //convert the dirty html file to a temp xhtml file (on disk, so we can keep working with URIs)
        this.tempFile = File.createTempFile(this.getClass().getCanonicalName(), ".xhtml");

        //we need to read everything in to parse the dirty html to clean xhtml
        Document doc = null;
        try (InputStream dirtyStream = super.getInputStream()) {
            doc = Jsoup.parse(dirtyStream, null, this.getBaseUri().toString());
        }

        // Clean the document (doesn't work because it strips the head out)
        //Whitelist whitelist = Whitelist.relaxed();
        //doc = new Cleaner(whitelist).clean(doc);

        // Adjust escape mode
        //doc.outputSettings().escapeMode(Entities.EscapeMode.base);

        //note: this is required! (doesn't work with html)
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        FileUtils.write(this.tempFile, doc.html());

        this.inputStream = new FileInputStream(this.tempFile);
        this.originalDocument = this.document;
        this.document = this.tempFile.toURI();
    }

    //-----PUBLIC METHODS-----
    @Override
    public void close() throws Exception
    {
        super.close();

        synchronized (this) {
            if (this.tempFile != null) {
                if (this.tempFile.exists()) {
                    this.tempFile.delete();
                }
                this.tempFile = null;
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
